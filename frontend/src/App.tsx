import { useState, useCallback } from 'react';
import { GameMode, GameSnapshot, PieceColor, PieceType, Square } from './types/game';
import { GameEndedPayload, GameStateUpdatedPayload, MoveRejectedPayload } from './types/protocol';
import { Lobby } from './components/Lobby/Lobby';
import { GameView } from './views/GameView';
import { useChessWebSocket } from './hooks/useChessWebSocket';
import { createGameApi, joinGameApi } from './services/api';
import './App.css';

const INITIAL_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

export default function App() {
  const [playerId] = useState(() => `user-${Date.now().toString(36)}`);
  const [gameId, setGameId] = useState<string | null>(null);
  const [gameState, setGameState] = useState<GameSnapshot | null>(null);
  const [myColor, setMyColor] = useState<PieceColor | null>('WHITE');
  const [isLoading, setIsLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [promoMove, setPromoMove] = useState<{ from: Square; to: Square } | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 4000);
  };

  const handleStateUpdated = useCallback((u: GameStateUpdatedPayload, ver: number) => {
    setGameState((prev) => !prev ? null : {
      ...prev,
      gameVersion: ver,
      turn: u.turn,
      fen: u.fen,
      lastMove: u.lastMove,
      isCheck: u.isCheck,
      whitePlayer: { ...prev.whitePlayer, remainingTimeMs: u.whiteRemainingTimeMs },
      blackPlayer: prev.blackPlayer ? { ...prev.blackPlayer, remainingTimeMs: u.blackRemainingTimeMs } : null,
    });
  }, []);

  const handleGameEnded = useCallback((e: GameEndedPayload) => {
    setGameState((prev) => !prev ? null : {
      ...prev,
      gameStatus: e.reason === 'RESIGNATION' ? 'RESIGNED' : e.reason === 'CHECKMATE' ? 'CHECKMATE' : 'DRAW',
      result: e.result,
      endReason: e.reason,
      fen: e.finalFen,
    });
    showToast(`대국 종료: ${e.result} (${e.reason})`);
  }, []);

  const { connectionStatus, sendMove, sendResign, sendOfferDraw, sendSyncState } = useChessWebSocket({
    gameId,
    playerId,
    onSnapshot: (snap) => setGameState(snap),
    onStateUpdated: handleStateUpdated,
    onMoveRejected: (r: MoveRejectedPayload) => showToast(`착수 거부: ${r.message}`),
    onGameEnded: handleGameEnded,
    onError: showToast,
  });

  const handleCreate = async (opts: {
    gameMode: GameMode;
    aiLevel: number;
    baseMinutes: number;
    incrementSeconds: number;
    preferredColor: PieceColor | 'RANDOM';
    playerName: string;
  }) => {
    setIsLoading(true);
    try {
      const data = await createGameApi(opts);
      const color = opts.preferredColor === 'RANDOM' ? (Math.random() > 0.5 ? 'WHITE' : 'BLACK') : opts.preferredColor;
      setMyColor(color as PieceColor);
      setGameId(data.gameId);
      setGameState({
        gameId: data.gameId,
        gameMode: opts.gameMode,
        aiLevel: opts.aiLevel,
        gameStatus: opts.gameMode === 'PVC' ? 'ACTIVE' : 'WAITING_FOR_OPPONENT',
        gameVersion: 0,
        turn: 'WHITE',
        fen: INITIAL_FEN,
        whitePlayer: {
          id: color === 'WHITE' ? playerId : 'ai-stockfish',
          name: color === 'WHITE' ? opts.playerName : 'Stockfish AI (2000)',
          remainingTimeMs: opts.baseMinutes * 60 * 1000,
          isOnline: true,
          isAi: color !== 'WHITE' && opts.gameMode === 'PVC',
        },
        blackPlayer: {
          id: color === 'BLACK' ? playerId : (opts.gameMode === 'PVC' ? 'ai-stockfish' : 'waiting'),
          name: color === 'BLACK' ? opts.playerName : (opts.gameMode === 'PVC' ? 'Stockfish AI (2000)' : '대기 중...'),
          remainingTimeMs: opts.baseMinutes * 60 * 1000,
          isOnline: true,
          isAi: color !== 'BLACK' && opts.gameMode === 'PVC',
        },
        isCheck: false,
      });
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : '생성 실패');
    } finally {
      setIsLoading(false);
    }
  };

  const handleJoin = async (id: string) => {
    setIsLoading(true);
    try {
      await joinGameApi(id);
      setMyColor('BLACK');
      setGameId(id);
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : '참가 실패');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-layout">
      <header className="app-navbar">
        <div className="logo-section">
          <h1>jchess</h1>
          <span className="badge-tag">실시간 온라인 체스</span>
        </div>
        {gameId && (
          <div className="game-status-nav">
            <span className="nav-game-id">방: <code>{gameId}</code></span>
            <span className={`connection-badge ${connectionStatus.toLowerCase()}`}>
              {connectionStatus === 'CONNECTED' ? '🟢 연결됨' : connectionStatus === 'RECONNECTING' ? '🟡 재연결 중' : '🔴 오프라인'}
            </span>
          </div>
        )}
      </header>

      {toast && <div className="toast-notification">{toast}</div>}

      <main className="app-main">
        {!gameId || !gameState ? (
          <Lobby onCreateGame={handleCreate} onJoinGame={handleJoin} isLoading={isLoading} />
        ) : (
          <GameView
            gameState={gameState}
            myColor={myColor}
            promoMove={promoMove}
            onMove={(from, to) => sendMove(from, to, null)}
            onRequestPromotion={(from, to) => setPromoMove({ from, to })}
            onSelectPromotion={(p: PieceType) => {
              if (promoMove) {
                sendMove(promoMove.from, promoMove.to, p);
                setPromoMove(null);
              }
            }}
            onCancelPromotion={() => setPromoMove(null)}
            onResign={sendResign}
            onOfferDraw={sendOfferDraw}
            onSync={sendSyncState}
            onLeave={() => { setGameId(null); setGameState(null); }}
          />
        )}
      </main>
    </div>
  );
}
