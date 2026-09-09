import React from 'react';
import './GameRulesModal.css';

interface GameRulesModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const GameRulesModal: React.FC<GameRulesModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="rules-modal-backdrop" onClick={onClose}>
      <div className="rules-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="rules-modal-header">
          <div className="rules-header-title">
            <span className="rules-header-icon">♟️</span>
            <h3>체스 게임 방법 및 규칙 가이드</h3>
          </div>
          <button className="rules-close-btn" onClick={onClose} aria-label="닫기">✕</button>
        </div>

        <div className="rules-modal-body">
          {/* 1. 게임 목표 */}
          <div className="rule-box">
            <h4>1. 게임 목표 & 기본 진행</h4>
            <p>
              체스판은 <strong>8x8 총 64칸</strong>이며, 항상 <strong>백(White)이 선공</strong>으로 시작하여 번갈아 1수씩 둡니다.<br />
              상대 킹을 공격하여 킹이 피할 수 없는 상태인 <strong>체크메이트(Checkmate)</strong>를 만들면 승리합니다.
            </p>
          </div>

          {/* 2. 기물 이동 규칙 */}
          <div className="rule-box">
            <h4>2. 6종 기물별 이동 방법</h4>
            <div className="pieces-summary-grid">
              <div className="piece-item">
                <span className="p-sym">♔</span>
                <strong>킹 (King)</strong>
                <span>전 방향 1칸 (보호 필수)</span>
              </div>
              <div className="piece-item">
                <span className="p-sym">♕</span>
                <strong>퀸 (Queen)</strong>
                <span>가로/세로/대각선 무제한</span>
              </div>
              <div className="piece-item">
                <span className="p-sym">♖</span>
                <strong>룩 (Rook)</strong>
                <span>가로/세로 직선 무제한</span>
              </div>
              <div className="piece-item">
                <span className="p-sym">♗</span>
                <strong>비숍 (Bishop)</strong>
                <span>대각선 방향 무제한</span>
              </div>
              <div className="piece-item">
                <span className="p-sym">♘</span>
                <strong>나이트 (Knight)</strong>
                <span>L자 점프 (기물 통과 가능)</span>
              </div>
              <div className="piece-item">
                <span className="p-sym">♙</span>
                <strong>폰 (Pawn)</strong>
                <span>앞으로 1칸 (첫 수 2칸, 대각선 캡처)</span>
              </div>
            </div>
          </div>

          {/* 3. 3대 특수 규칙 */}
          <div className="rule-box">
            <h4>3. 3대 특수 규칙</h4>
            <ul className="special-rules-list">
              <li>
                <strong>🏰 캐슬링 (Castling)</strong>: 킹과 룩이 처음 움직일 때, 킹을 2칸 이동시키고 룩을 킹 옆으로 넘기는 특수 수.
              </li>
              <li>
                <strong>⚔️ 앙파상 (En Passant)</strong>: 상대 폰이 2칸 전진하여 내 폰 옆에 붙었을 때, 대각선으로 지나치며 잡는 특수 캡처 (직후 1턴만 유효).
              </li>
              <li>
                <strong>👑 프로모션 (Promotion)</strong>: 폰이 상대편 끝 줄(8랭크)에 도달하면 즉시 <strong>퀸, 룩, 비숍, 나이트</strong> 중 원하는 기물로 승급.
              </li>
            </ul>
          </div>

          {/* 4. 대국 모드 */}
          <div className="rule-box">
            <h4>4. 대국 모드 안내</h4>
            <p>
              • <strong>🤖 AI 대전 (Stockfish 2000+)</strong>: ELO 2000+ AI와 즉시 싱글 대국을 진행합니다.<br />
              • <strong>👥 2인 대전 (PVP)</strong>: 방 생성 후 발급된 Game ID를 상대에게 공유하여 실시간 대국을 진행합니다.
            </p>
          </div>
        </div>

        <div className="rules-modal-footer">
          <button className="rules-confirm-btn" onClick={onClose}>
            확인 및 닫기
          </button>
        </div>
      </div>
    </div>
  );
};
