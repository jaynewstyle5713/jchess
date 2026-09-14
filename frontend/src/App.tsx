import { useState, useCallback, useRef } from 'react';
import { GameMode, GameSnapshot, PieceColor, PieceType, Square } from './types/game';
import { GameEndedPayload, GameStateUpdatedPayload, MoveRejectedPayload } from './types/protocol';
import { Lobby } from './components/Lobby/Lobby';
import { GameView } from './views/GameView';
import { GameRulesModal } from './components/RulesModal/GameRulesModal';
import { useChessWebSocket } from './hooks/useChessWebSocket';
import { createGameApi, joinGameApi, requestHintApi, undoMoveApi } from './services/api';
import { CommentaryMessage, generateCommentary } from './utils/commentaryEngine';
import './App.css';

const INITIAL_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

function getAiDisplayName(elo: number): string {
  if (elo <= 750) return `Stockfish 8 (입문 · 600)`;
  if (elo <= 1050) return `Stockfish 11 (초급 · 900)`;
  if (elo <= 1450) return `Stockfish 14 (중급 · 1300)`;
  if (elo <= 1850) return `Stockfish 17 (고급 · 1700)`;
  return `Stockfish 19 (마스터 · 2000+)`;
}

export default function App() {
  const [playerId] = useState(() => `user-${Date.now().toString(36)}`);
  const [gameId, setGameId] = useState<string | null>(null);
  const [gameState, setGameState] = useState<GameSnapshot | null>(null);
  const [myColor, setMyColor] = useState<PieceColor | null>('WHITE');
  const [isLoading, setIsLoading] = useState(false);
  const [isRulesOpen, setIsRulesOpen] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [promoMove, setPromoMove] = useState<{ from: Square; to: Square } | null>(null);
  const [hintMove, setHintMove] = useState<{ from: Square; to: Square } | null>(null);
  const [commentaryMessages, setCommentaryMessages] = useState<CommentaryMessage[]>([]);

  const gameStateRef = useRef<GameSnapshot | null>(null);
  gameStateRef.current = gameState;

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 4000);
  }, []);

  const handleSnapshot = useCallback((snap: GameSnapshot) => {
    setGameState(snap);
    if (snap.whitePlayer.id === playerId) {
      setMyColor('WHITE');
    } else if (snap.blackPlayer && snap.blackPlayer.id === playerId) {
      setMyColor('BLACK');
    }
  }, [playerId]);

  const handleStateUpdated = useCallback((u: GameStateUpdatedPayload, ver: number) => {
    setHintMove(null);
    setGameState((prev) => {
      if (!prev) return null;
      return {
        ...prev,
        gameVersion: ver,
        turn: u.turn,
        fen: u.fen,
        lastMove: u.lastMove,
        isCheck: u.isCheck,
        whitePlayer: { ...prev.whitePlayer, remainingTimeMs: u.whiteRemainingTimeMs },
        blackPlayer: prev.blackPlayer ? { ...prev.blackPlayer, remainingTimeMs: u.blackRemainingTimeMs } : null,
      };
    });

    if (u.lastMove) {
      const movedColor: PieceColor = u.turn === 'WHITE' ? 'BLACK' : 'WHITE';
      const currentSnap = gameStateRef.current;
      const playerName = movedColor === 'WHITE'
        ? (currentSnap?.whitePlayer?.name || '백 플레이어')
        : (currentSnap?.blackPlayer?.name || '흑 플레이어');

      const moveNotation = u.lastMove.notation || `${u.lastMove.from}-${u.lastMove.to}`;
      const comm = generateCommentary({
        moveNumber: u.moveNumber || 1,
        color: movedColor,
        playerName,
        from: u.lastMove.from,
        to: u.lastMove.to,
        notation: moveNotation,
        promotionPiece: u.lastMove.promotion,
        isCheck: u.isCheck,
        capturedPiece: u.lastMove.captured ? String(u.lastMove.captured) : null,
      });

      setCommentaryMessages((prev) => [...prev, comm]);
    }
  }, []);

  const handleGameEnded = useCallback((e: GameEndedPayload) => {
    setHintMove(null);
    setGameState((prev) => !prev ? null : {
      ...prev,
      gameStatus: e.reason === 'RESIGNATION' ? 'RESIGNED' : e.reason === 'CHECKMATE' ? 'CHECKMATE' : 'DRAW',
      result: e.result,
      endReason: e.reason,
      fen: e.finalFen,
    });

    const now = new Date();
    const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
    const resultTitle = e.reason === 'CHECKMATE'
      ? `👑 외통수 체크메이트! ${e.result === 'WHITE_WON' ? '백' : '흑'} 승리`
      : e.reason === 'RESIGNATION'
      ? `🏳️ 기권으로 인한 ${e.result === 'WHITE_WON' ? '백' : '흑'} 승리`
      : `🤝 무승부 (${e.reason})`;

    const endComm: CommentaryMessage = {
      id: `comm-end-${Date.now()}`,
      moveNumber: 999,
      color: e.result === 'WHITE_WON' ? 'WHITE' : 'BLACK',
      playerName: '경기 종료',
      notation: e.reason,
      title: resultTitle,
      body: e.message || `대국이 공식 종료되었습니다. 최종 결과: ${e.result} (${e.reason})`,
      tag: e.reason === 'CHECKMATE' ? 'MATE' : 'ENDGAME',
      timestamp: timeStr,
    };
    setCommentaryMessages((prev) => [...prev, endComm]);

    showToast(e.message || `대국 종료: ${e.result} (${e.reason})`);
  }, [showToast]);

  const handleMoveRejected = useCallback((r: MoveRejectedPayload) => {
    showToast(`착수 거부: ${r.message}`);
  }, [showToast]);

  const handleDrawRejected = useCallback((d: { reason: string }) => {
    showToast(d.reason || 'AI가 무승부 제안을 거절했습니다.');
  }, [showToast]);

  const { connectionStatus, sendMove, sendResign, sendOfferDraw, sendSyncState } = useChessWebSocket({
    gameId,
    playerId,
    onSnapshot: handleSnapshot,
    onStateUpdated: handleStateUpdated,
    onMoveRejected: handleMoveRejected,
    onGameEnded: handleGameEnded,
    onDrawRejected: handleDrawRejected,
    onError: showToast,
  });

  const handleRequestHint = async () => {
    if (!gameId) return;
    try {
      const res = await requestHintApi(gameId, playerId);
      if (res.from && res.to) {
        setHintMove({ from: res.from as Square, to: res.to as Square });
        setGameState((prev) => !prev ? null : {
          ...prev,
          remainingHints: res.remainingHints,
        });
        showToast(`💡 AI 추천TIP: ${res.from} ➔ ${res.to} (남은 힌트: ${res.remainingHints}회)`);
      }
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : '추천TIP 조회 실패');
    }
  };

  const handleUndo = async () => {
    if (!gameId) return;
    try {
      const res = await undoMoveApi(gameId, playerId);
      if (res.success && res.snapshot) {
        setHintMove(null);
        setGameState(res.snapshot);
        // 무르기 시 최근 2개의 중계 메시지 제거 및 안내 추가
        setCommentaryMessages((prev) => {
          const sliced = prev.slice(0, Math.max(0, prev.length - 2));
          const now = new Date();
          const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
          const undoNotice: CommentaryMessage = {
            id: `comm-undo-${Date.now()}`,
            moveNumber: 0,
            color: 'WHITE',
            playerName: '심판석',
            notation: 'UNDO',
            title: '↩️ 무르기(Undo) 적용',
            body: `플레이어의 요청으로 직전 수가 되돌려졌습니다. (남은 무르기: ${res.remainingUndos}/${res.maxUndos}회)`,
            tag: 'INFO',
            timestamp: timeStr,
          };
          return [...sliced, undoNotice];
        });
        showToast(`↩️ 무르기가 적용되었습니다. (남은 횟수: ${res.remainingUndos}/${res.maxUndos}회)`);
      }
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : '무르기 실패');
    }
  };

  const handleCreate = async (opts: {
    gameMode: GameMode;
    aiLevel: number;
    baseMinutes: number;
    incrementSeconds: number;
    preferredColor: PieceColor | 'RANDOM';
    playerName: string;
  }) => {
    setIsLoading(true);
    setHintMove(null);
    try {
      const data = await createGameApi({
        ...opts,
        playerId,
        playerName: opts.playerName,
      });
      const color = opts.preferredColor === 'RANDOM' ? (Math.random() > 0.5 ? 'WHITE' : 'BLACK') : opts.preferredColor;
      const aiName = getAiDisplayName(opts.aiLevel);
      let maxUndos = 3;
      if (opts.gameMode === 'PVC') {
        if (opts.aiLevel <= 750) maxUndos = 10;
        else if (opts.aiLevel <= 1050) maxUndos = 8;
        else if (opts.aiLevel <= 1450) maxUndos = 5;
        else maxUndos = 3;
      }
      setMyColor(color as PieceColor);
      setGameId(data.gameId);
      setGameState({
        gameId: data.gameId,
        gameMode: opts.gameMode,
        aiLevel: opts.aiLevel,
        remainingHints: 3,
        maxUndos,
        remainingUndos: maxUndos,
        gameStatus: opts.gameMode === 'PVC' ? 'ACTIVE' : 'WAITING_FOR_OPPONENT',
        gameVersion: 0,
        turn: 'WHITE',
        fen: INITIAL_FEN,
        whitePlayer: {
          id: color === 'WHITE' ? playerId : 'ai-stockfish',
          name: color === 'WHITE' ? opts.playerName : aiName,
          remainingTimeMs: opts.baseMinutes * 60 * 1000,
          isOnline: true,
          isAi: color !== 'WHITE' && opts.gameMode === 'PVC',
        },
        blackPlayer: {
          id: color === 'BLACK' ? playerId : (opts.gameMode === 'PVC' ? 'ai-stockfish' : 'waiting'),
          name: color === 'BLACK' ? opts.playerName : (opts.gameMode === 'PVC' ? aiName : '대기 중...'),
          remainingTimeMs: opts.baseMinutes * 60 * 1000,
          isOnline: true,
          isAi: color !== 'BLACK' && opts.gameMode === 'PVC',
        },
        isCheck: false,
      });

      const now = new Date();
      const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
      const whiteName = color === 'WHITE' ? opts.playerName : aiName;
      const blackName = color === 'BLACK' ? opts.playerName : (opts.gameMode === 'PVC' ? aiName : '대기 중...');
      setCommentaryMessages([
        {
          id: `comm-start-${Date.now()}`,
          moveNumber: 0,
          color: 'WHITE',
          playerName: '중계석',
          notation: 'START',
          title: '🎙️ 실시간 체스 중계 개시',
          body: `[${whiteName}] 님과 [${blackName}] 님의 명승부가 시작되었습니다! 백의 첫 수를 기다립니다.`,
          tag: 'INFO',
          timestamp: timeStr,
        },
      ]);
    } catch (err: unknown) {
      showToast(err instanceof Error ? err.message : '생성 실패');
    } finally {
      setIsLoading(false);
    }
  };

  const handleJoin = async (id: string, playerName: string) => {
    setIsLoading(true);
    setHintMove(null);
    try {
      await joinGameApi(id, playerId, playerName);
      setMyColor('BLACK');
      setGameId(id);
      const now = new Date();
      const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
      setCommentaryMessages([
        {
          id: `comm-join-${Date.now()}`,
          moveNumber: 0,
          color: 'BLACK',
          playerName: '중계석',
          notation: 'JOIN',
          title: '🎙️ 2인 대전 중계 연결',
          body: `대국실에 성공적으로 입장하셨습니다. 백의 첫 수로 경기가 진행됩니다.`,
          tag: 'INFO',
          timestamp: timeStr,
        },
      ]);
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
        <div className="nav-actions">
          <button
            type="button"
            className="nav-rules-btn"
            onClick={() => setIsRulesOpen(true)}
            title="게임 방법 및 체스 규칙 확인"
          >
            📖 game방법
          </button>
          {gameId && (
            <div className="game-status-nav">
              <span className="nav-game-id">방: <code>{gameId}</code></span>
              <span className={`connection-badge ${connectionStatus.toLowerCase()}`}>
                {connectionStatus === 'CONNECTED' ? '🟢 연결됨' : connectionStatus === 'RECONNECTING' ? '🟡 재연결 중' : '🔴 오프라인'}
              </span>
            </div>
          )}
        </div>
      </header>

      {toast && <div className="toast-notification">{toast}</div>}

      <main className="app-main">
        {!gameId || !gameState ? (
          <Lobby
            onCreateGame={handleCreate}
            onJoinGame={handleJoin}
            onOpenRules={() => setIsRulesOpen(true)}
            isLoading={isLoading}
          />
        ) : (
          <GameView
            gameState={gameState}
            myColor={myColor}
            promoMove={promoMove}
            hintMove={hintMove}
            commentaryMessages={commentaryMessages}
            onRequestHint={handleRequestHint}
            onRequestUndo={handleUndo}
            onMove={(from, to) => {
              setHintMove(null);
              sendMove(from, to, null);
            }}
            onRequestPromotion={(from, to) => setPromoMove({ from, to })}
            onSelectPromotion={(p: PieceType) => {
              if (promoMove) {
                setHintMove(null);
                sendMove(promoMove.from, promoMove.to, p);
                setPromoMove(null);
              }
            }}
            onCancelPromotion={() => setPromoMove(null)}
            onResign={sendResign}
            onOfferDraw={sendOfferDraw}
            onSync={sendSyncState}
            onLeave={() => {
              setGameId(null);
              setGameState(null);
              setHintMove(null);
              setCommentaryMessages([]);
            }}
          />
        )}
      </main>

      {/* 게임 방법 모달 */}
      <GameRulesModal isOpen={isRulesOpen} onClose={() => setIsRulesOpen(false)} />
    </div>
  );
}
