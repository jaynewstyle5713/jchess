import React from 'react';
import './ChessRules.css';

interface ChessRulesProps {
  onStartGame: () => void;
}

export const ChessRules: React.FC<ChessRulesProps> = ({ onStartGame }) => {
  return (
    <div className="rules-container">
      {/* 히어로 배너 */}
      <div className="rules-hero">
        <span className="hero-icon">♔</span>
        <h1 className="hero-title">체스(Chess) 규칙 & 대국 가이드</h1>
        <p className="hero-subtitle">
          jchess는 FIDE 국제 체스 연맹 표준 규칙을 준수하는 실시간 플랫폼입니다.
          체스 기본 룰부터 특수 수까지 확인하고 바로 대국을 시작해 보세요!
        </p>
        <button className="hero-start-btn" onClick={onStartGame}>
          🎮 대국 시작하기 (로비 입장)
        </button>
      </div>

      {/* 1. 기본 원칙 & 승리 조건 */}
      <section className="rules-section">
        <h2 className="section-title"><span>1</span> ♟️ 기본 원칙 & 승리 조건</h2>
        <div className="rules-grid-2">
          <div className="rule-card">
            <h3>보드 및 차례</h3>
            <ul>
              <li>체스판은 <strong>8x8 총 64칸</strong>으로 구성됩니다.</li>
              <li>항상 <strong>백(White)</strong>이 먼저 시작하며, 번갈아 1수씩 둡니다.</li>
              <li>White 기준 우측 하단 칸(h1)은 항상 밝은 칸입니다.</li>
            </ul>
          </div>
          <div className="rule-card">
            <h3>체크 & 체크메이트</h3>
            <ul>
              <li><strong>체크(Check)</strong>: 상대 기물이 킹을 직접 위협하는 상태입니다.</li>
              <li><strong>체크메이트(Checkmate)</strong>: 체크 상태에서 킹을 피하거나 방어할 수 없을 때 즉시 승리합니다.</li>
              <li>자신의 킹을 체크 상태로 방치하는 수는 둘 수 없습니다.</li>
            </ul>
          </div>
        </div>
      </section>

      {/* 2. 기물별 이동 방식 */}
      <section className="rules-section">
        <h2 className="section-title"><span>2</span> 🛡️ 기물별 이동 방식과 가치</h2>
        <div className="pieces-grid">
          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♔</span>
              <div>
                <h4>킹 (King)</h4>
                <span className="piece-value">가치: ∞</span>
              </div>
            </div>
            <p className="piece-desc">모든 방향으로 <strong>1칸</strong>씩 이동합니다. 잡히면 패배하므로 반드시 지켜야 합니다.</p>
          </div>

          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♕</span>
              <div>
                <h4>퀸 (Queen)</h4>
                <span className="piece-value">가치: 9점</span>
              </div>
            </div>
            <p className="piece-desc">가로, 세로, 대각선 모든 방향으로 <strong>원하는 칸만큼</strong> 직선 이동합니다.</p>
          </div>

          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♖</span>
              <div>
                <h4>룩 (Rook)</h4>
                <span className="piece-value">가치: 5점</span>
              </div>
            </div>
            <p className="piece-desc">가로 및 세로 방향으로 <strong>원하는 칸만큼</strong> 이동하며, 캐슬링에 참여합니다.</p>
          </div>

          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♗</span>
              <div>
                <h4>비숍 (Bishop)</h4>
                <span className="piece-value">가치: 3점</span>
              </div>
            </div>
            <p className="piece-desc">대각선 방향으로 <strong>원하는 칸만큼</strong> 이동하며, 시작 칸 색상만 이동합니다.</p>
          </div>

          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♘</span>
              <div>
                <h4>나이트 (Knight)</h4>
                <span className="piece-value">가치: 3점</span>
              </div>
            </div>
            <p className="piece-desc"><strong>L자 모양</strong>(2칸+1칸)으로 이동하며, 유일하게 <strong>다른 기물을 뛰어넘습니다</strong>.</p>
          </div>

          <div className="piece-card">
            <div className="piece-header">
              <span className="piece-symbol">♙</span>
              <div>
                <h4>폰 (Pawn)</h4>
                <span className="piece-value">가치: 1점</span>
              </div>
            </div>
            <p className="piece-desc">앞으로 1칸 전진(첫 이동 2칸 가능)하며, <strong>앞 대각선 1칸</strong> 적 기물을 잡습니다.</p>
          </div>
        </div>
      </section>

      {/* 3. 특수 규칙 */}
      <section className="rules-section">
        <h2 className="section-title"><span>3</span> ⚡ 3대 특수 규칙</h2>
        <div className="special-rules-grid">
          <div className="special-card">
            <div className="special-icon">🏰</div>
            <h3>캐슬링 (Castling)</h3>
            <p>킹과 룩을 1수에 동시 이동하여 킹을 보호하고 룩을 중앙으로 전개합니다. 킹과 룩이 처음 움직일 때만 가능합니다.</p>
          </div>

          <div className="special-card">
            <div className="special-icon">⚔️</div>
            <h3>앙파상 (En Passant)</h3>
            <p>상대 폰이 2칸 전진하여 내 폰 옆에 붙었을 때, 대각선으로 지나치며 잡는 특수 캡처입니다. (직후 1턴만 유효)</p>
          </div>

          <div className="special-card">
            <div className="special-icon">👑</div>
            <h3>프로모션 (Promotion)</h3>
            <p>폰이 상대 끝 줄(8랭크)에 도달하면 즉시 <strong>퀸, 룩, 비숍, 나이트</strong> 중 하나로 승급합니다.</p>
          </div>
        </div>
      </section>

      {/* 4. 무승부 판정 조건 */}
      <section className="rules-section">
        <h2 className="section-title"><span>4</span> 🤝 무승부 (Draw) 조건</h2>
        <div className="draw-grid">
          <div className="draw-item">
            <h4>스테일메이트 (Stalemate)</h4>
            <p>체크 상태가 아니지만 둘 수 있는 합법 수가 전혀 없을 때</p>
          </div>
          <div className="draw-item">
            <h4>3회 동형반복 & 50수 규칙</h4>
            <p>동일 포지션 3회 반복 또는 50수간 폰 이동/기물 캡처가 없을 때</p>
          </div>
        </div>
      </section>

      {/* 하단 CTA */}
      <div className="rules-bottom-cta">
        <h2>규칙을 모두 확인하셨나요?</h2>
        <p>지금 바로 실시간 체스 대국에 참여해 보세요!</p>
        <button className="cta-start-btn" onClick={onStartGame}>
          ⚔️ 지금 대국 시작하기
        </button>
      </div>
    </div>
  );
};
