import React, { useEffect, useMemo, useState } from 'react';
import { GameEndReason, GameResult, GameStatus, PieceColor, PlayerInfo } from '../../types/game';
import { calculateMaterial } from '../../utils/chess';
import './GamePanel.css';

interface GamePanelProps {
  fen: string;
  status: GameStatus;
  turn: PieceColor;
  isCheck: boolean;
  whitePlayer: PlayerInfo;
  blackPlayer: PlayerInfo | null;
  myColor?: PieceColor | null;
  result?: GameResult | null;
  endReason?: GameEndReason | null;
  isAiGame?: boolean;
  remainingHints?: number;
  maxUndos?: number;
  remainingUndos?: number;
  onRequestHint?: () => void;
  onRequestUndo?: () => void;
  onResign: () => void;
  onOfferDraw: () => void;
  onSync: () => void;
  onLeave: () => void;
}

export const GamePanel: React.FC<GamePanelProps> = ({
  fen,
  status,
  turn,
  isCheck,
  whitePlayer,
  blackPlayer,
  myColor,
  result,
  endReason,
  isAiGame = false,
  remainingHints = 3,
  maxUndos = 3,
  remainingUndos = 3,
  onRequestHint,
  onRequestUndo,
  onResign,
  onOfferDraw,
  onSync,
  onLeave,
}) => {
  const [whiteTime, setWhiteTime] = useState(whitePlayer.remainingTimeMs);
  const [blackTime, setBlackTime] = useState(blackPlayer?.remainingTimeMs || 0);

  useEffect(() => {
    setWhiteTime(whitePlayer.remainingTimeMs);
  }, [whitePlayer.remainingTimeMs]);

  useEffect(() => {
    if (blackPlayer) setBlackTime(blackPlayer.remainingTimeMs);
  }, [blackPlayer?.remainingTimeMs]);

  // 실시간 타이머 카운트다운
  useEffect(() => {
    if (status !== 'ACTIVE' && status !== 'CHECK') return;

    const interval = setInterval(() => {
      if (turn === 'WHITE') {
        setWhiteTime((prev) => Math.max(0, prev - 1000));
      } else {
        setBlackTime((prev) => Math.max(0, prev - 1000));
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [status, turn]);

  // 기물 점수 및 실시간 형세 분석 (REQ-UAT-05)
  const materialEval = useMemo(() => {
    return calculateMaterial(fen, myColor || 'WHITE');
  }, [fen, myColor]);

  const formatTime = (ms: number) => {
    const totalSec = Math.floor(ms / 1000);
    const min = Math.floor(totalSec / 60);
    const sec = totalSec % 60;
    return `${min}:${sec < 10 ? '0' : ''}${sec}`;
  };

  const isGameOver = ['CHECKMATE', 'STALEMATE', 'DRAW', 'RESIGNED', 'TIMEOUT'].includes(status);

  const topPlayer = myColor === 'BLACK' ? whitePlayer : (blackPlayer || { id: 'waiting', name: '상대 대기 중...', remainingTimeMs: 0, isOnline: false });
  const bottomPlayer = myColor === 'BLACK' ? (blackPlayer || { id: 'me', name: '나', remainingTimeMs: 0, isOnline: true }) : whitePlayer;

  const topColor: PieceColor = myColor === 'BLACK' ? 'WHITE' : 'BLACK';
  const bottomColor: PieceColor = myColor === 'BLACK' ? 'BLACK' : 'WHITE';

  return (
    <div className="game-panel">
      {/* ⚖️ 기물 점수 & 실시간 형세 분석 바 (REQ-UAT-05) */}
      <div className="material-advantage-card">
        <div className="material-card-header">
          <span className="material-card-title">⚖️ 기물 점수 & 형세 분석</span>
          <span className="material-advantage-badge">{materialEval.advantageText}</span>
        </div>
        <div className="material-scores-row">
          <div className="score-chip">
            <span className="chip-avatar">{topColor === 'WHITE' ? '♔' : '♚'}</span>
            <span className="chip-name">{topPlayer.name}</span>
            <span className="chip-score">{topColor === 'WHITE' ? materialEval.whiteScore : materialEval.blackScore}점</span>
          </div>
          <span className="scores-vs">vs</span>
          <div className="score-chip my-chip">
            <span className="chip-avatar">{bottomColor === 'WHITE' ? '♔' : '♚'}</span>
            <span className="chip-name">{bottomPlayer.name}</span>
            <span className="chip-score">{bottomColor === 'WHITE' ? materialEval.whiteScore : materialEval.blackScore}점</span>
          </div>
        </div>
      </div>

      {/* 상단 플레이어 카드 */}
      <div className={`player-card top ${turn === topColor && !isGameOver ? 'active-turn' : ''}`}>
        <div className="player-info">
          <span className="player-avatar">{topColor === 'WHITE' ? '♔' : '♚'}</span>
          <div>
            <div className="player-name">
              {topPlayer.name}
              {topPlayer.isAi && <span className="ai-badge">AI (ELO 2000)</span>}
            </div>
            <div className="player-status-indicator">
              <span className={`status-dot ${topPlayer.isOnline ? 'online' : 'offline'}`} />
              {topPlayer.isOnline ? '온라인' : '연결 끊김'}
            </div>
          </div>
        </div>
        <div className={`player-clock ${topTimeIsLow(topColor === 'WHITE' ? whiteTime : blackTime) ? 'time-low' : ''}`}>
          {formatTime(topColor === 'WHITE' ? whiteTime : blackTime)}
        </div>
      </div>

      {/* 중앙 상태 및 안내창 */}
      <div className="game-status-banner">
        {isGameOver ? (
          <div className="game-over-banner">
            <span className="game-over-title">대국 종료: {status}</span>
            <span className="game-over-desc">
              {result === 'WHITE_WON' ? '백 승리' : result === 'BLACK_WON' ? '흑 승리' : '무승부'} ({endReason})
            </span>
          </div>
        ) : (
          <div className="turn-status">
            <span className="turn-text">
              현재 차례: <strong>{turn === 'WHITE' ? '백 (White)' : '흑 (Black)'}</strong>
            </span>
            {isCheck && <span className="check-alert">CHECK!</span>}
            {turn === topColor && topPlayer.isAi && (
              <div className="ai-thinking">
                <span className="spinner" /> AI 연산 중...
              </div>
            )}
          </div>
        )}
      </div>

      {/* 하단 플레이어 카드 */}
      <div className={`player-card bottom ${turn === bottomColor && !isGameOver ? 'active-turn' : ''}`}>
        <div className="player-info">
          <span className="player-avatar">{bottomColor === 'WHITE' ? '♔' : '♚'}</span>
          <div>
            <div className="player-name">
              {bottomPlayer.name} (나)
            </div>
            <div className="player-status-indicator">
              <span className="status-dot online" /> 온라인
            </div>
          </div>
        </div>
        <div className={`player-clock ${topTimeIsLow(bottomColor === 'WHITE' ? whiteTime : blackTime) ? 'time-low' : ''}`}>
          {formatTime(bottomColor === 'WHITE' ? whiteTime : blackTime)}
        </div>
      </div>

      {/* 조작 버튼 패널 (무르기, 힌트, 동기화, 무승부, 기권) */}
      <div className="game-controls">
        {!isGameOver ? (
          <>
            {isAiGame && onRequestUndo && (
              <button
                className="control-btn undo-btn"
                onClick={onRequestUndo}
                disabled={remainingUndos <= 0}
                title={remainingUndos <= 0 ? `무르기 ${maxUndos}회를 모두 소진하셨습니다.` : `직전 수 되돌리기 (남은 횟수: ${remainingUndos}/${maxUndos}회)`}
              >
                ↩️ 무르기 ({remainingUndos}/{maxUndos})
              </button>
            )}
            {isAiGame && onRequestHint && (
              <button
                className="control-btn hint-btn"
                onClick={onRequestHint}
                disabled={remainingHints <= 0 || (myColor !== null && turn !== myColor)}
                title={remainingHints <= 0 ? '추천TIP 3회를 모두 사용하셨습니다.' : 'AI 최선의 수 추천 받기 (게임당 3회)'}
              >
                💡 추천TIP ({remainingHints}/3)
              </button>
            )}
            <button className="control-btn sync-btn" onClick={onSync} title="서버 최신 상태 동기화">동기화</button>
            <button className="control-btn draw-btn" onClick={onOfferDraw} title="무승부 제안">무승부 제안</button>
            <button className="control-btn resign-btn" onClick={onResign} title="기권하기">기권</button>
          </>
        ) : (
          <button className="control-btn leave-btn" onClick={onLeave}>로비로 나가기</button>
        )}
      </div>
    </div>
  );
};

function topTimeIsLow(timeMs: number): boolean {
  return timeMs > 0 && timeMs <= 30000;
}
