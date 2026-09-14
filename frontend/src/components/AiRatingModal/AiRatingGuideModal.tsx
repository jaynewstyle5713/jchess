import React from 'react';
import './AiRatingGuideModal.css';

interface AiRatingGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AiRatingGuideModal: React.FC<AiRatingGuideModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="rating-modal-backdrop" onClick={onClose}>
      <div className="rating-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="rating-modal-header">
          <div className="rating-header-title">
            <span className="rating-header-icon">📊</span>
            <h3>체스 ELO 레이팅 및 AI 난이도 기준표</h3>
          </div>
          <button className="rating-close-btn" onClick={onClose} aria-label="닫기">✕</button>
        </div>

        <div className="rating-modal-body">
          {/* 1. ELO 레이팅 정의 및 공인 출처 */}
          <div className="rating-intro-box">
            <h4>📖 ELO 레이팅의 정의 및 출처</h4>
            <p>
              <strong>ELO 레이팅 시스템</strong>은 물리학자이자 체스 마스터인 <em>아르파드 엘뢰(Arpad Elo) 박사</em>가 고안한 수학적 상대 실력 평점 체계입니다.<br />
              현재 <strong>국제체스연맹(FIDE)</strong>, <strong>미국체스연맹(USCF)</strong> 및 글로벌 체스 플랫폼(<strong>Chess.com, Lichess</strong>)에서 공식 표준으로 사용하고 있습니다.
            </p>
          </div>

          {/* 2. 5단계 세부 난이도 레벨 가이드 */}
          <div className="rating-levels-container">
            <h4>🎯 Stockfish 엔진 단계별 체감 실력 및 ELO 기준</h4>
            <div className="rating-card-list">
              <div className="rating-level-card level-600">
                <div className="rating-level-badge">🌱 Stockfish 8 · ELO 600 (입문)</div>
                <div className="rating-level-desc">
                  <strong>체감 수준</strong>: 체스 행마법과 규칙을 갓 배운 입문자 단계<br />
                  <strong>특징</strong>: 기물 헌납 실수가 잦으며, 누구나 편안하게 이기는 재미를 느낄 수 있는 난이도입니다.
                </div>
              </div>

              <div className="rating-level-card level-900">
                <div className="rating-level-badge">🐣 Stockfish 11 · ELO 900 (초급)</div>
                <div className="rating-level-desc">
                  <strong>체감 수준</strong>: 포크, 핀 등 기본 전술과 단순 기물 교환을 인지하는 초급 단계<br />
                  <strong>특징</strong>: 기초적인 방어와 공격을 시도하지만 종종 실수를 합니다.
                </div>
              </div>

              <div className="rating-level-card level-1300">
                <div className="rating-level-badge">⚔️ Stockfish 14 · ELO 1300 (중급)</div>
                <div className="rating-level-desc">
                  <strong>체감 수준</strong>: 일반 체스 동호인 및 학교/클럽 중급자 수준<br />
                  <strong>특징</strong>: 안정적인 오프닝 전개와 전술적 기물 교환을 구사합니다.
                </div>
              </div>

              <div className="rating-level-card level-1700">
                <div className="rating-level-badge">🏆 Stockfish 17 · ELO 1700 (고급 / Class A)</div>
                <div className="rating-level-desc">
                  <strong>체감 수준</strong>: 지역 체스 대회 입상권 아마추어 상급자 수준<br />
                  <strong>특징</strong>: 단순 실수가 없으며, 깊은 수읽기와 포지션 운영, 엔드게임 심화 전술을 능숙하게 구사합니다.
                </div>
              </div>

              <div className="rating-level-card level-2000">
                <div className="rating-level-badge">👑 Stockfish 19 · ELO 2000+ (마스터 / NNUE)</div>
                <div className="rating-level-desc">
                  <strong>체감 수준</strong>: 전국 챔피언 및 프로 체스 기사 수준 (Candidate Master / Grandmaster)<br />
                  <strong>특징</strong>: 최신 Stockfish 19 NNUE 엔진의 정밀한 계산으로 사소한 실수도 용납하지 않는 최고 난이도입니다.
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="rating-modal-footer">
          <button className="rating-confirm-btn" onClick={onClose}>
            확인
          </button>
        </div>
      </div>
    </div>
  );
};
