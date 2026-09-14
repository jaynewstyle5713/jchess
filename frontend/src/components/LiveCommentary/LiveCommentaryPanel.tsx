import React, { useEffect, useRef } from 'react';
import { CommentaryMessage } from '../../utils/commentaryEngine';
import './LiveCommentaryPanel.css';

interface LiveCommentaryPanelProps {
  messages: CommentaryMessage[];
  whitePlayerName: string;
  blackPlayerName: string;
}

export const LiveCommentaryPanel: React.FC<LiveCommentaryPanelProps> = ({
  messages,
  whitePlayerName,
  blackPlayerName,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const getTagBadgeClass = (tag: CommentaryMessage['tag']) => {
    switch (tag) {
      case 'OPENING': return 'badge-opening';
      case 'BEST': return 'badge-best';
      case 'GOOD': return 'badge-good';
      case 'TACTICAL': return 'badge-tactical';
      case 'CASTLING': return 'badge-castling';
      case 'CHECK': return 'badge-check';
      case 'CAPTURE': return 'badge-capture';
      case 'PROMOTION': return 'badge-promotion';
      case 'MATE': return 'badge-mate';
      case 'ENDGAME': return 'badge-endgame';
      default: return 'badge-info';
    }
  };

  const getTagLabel = (tag: CommentaryMessage['tag']) => {
    switch (tag) {
      case 'OPENING': return '📖 정석 오프닝';
      case 'BEST': return '✨ 핵심 요충지';
      case 'GOOD': return '🎯 좋은 전개';
      case 'TACTICAL': return '♟️ 전술 전개';
      case 'CASTLING': return '🏰 캐슬링';
      case 'CHECK': return '⚡ 체크';
      case 'CAPTURE': return '⚔️ 기물 획득';
      case 'PROMOTION': return '👑 폰 승급';
      case 'MATE': return '🏆 체크메이트';
      case 'ENDGAME': return '🤝 엔드게임';
      default: return 'ℹ️ 안내';
    }
  };

  return (
    <div className="live-commentary-panel">
      {/* 중계석 헤더 */}
      <div className="commentary-header">
        <div className="commentary-header-title">
          <span className="live-icon">🎙️</span>
          <h3>실시간 체스 중계석</h3>
          <span className="live-pill"><span className="pulse-dot" />LIVE</span>
        </div>
        <div className="commentary-matchup">
          <span className="matchup-player white">♔ {whitePlayerName}</span>
          <span className="matchup-vs">VS</span>
          <span className="matchup-player black">♚ {blackPlayerName}</span>
        </div>
      </div>

      {/* 훈련 가이드 배너 */}
      <div className="commentary-training-banner">
        <span>💡 <strong>체스 훈련 해설</strong>: 대국자의 기물 행마와 전술적 의미를 전문 아나운서가 실시간 중계합니다.</span>
      </div>

      {/* 실시간 중계 메시지 피드 (채팅 스트림) */}
      <div className="commentary-feed" ref={scrollRef}>
        {messages.length === 0 ? (
          <div className="commentary-empty">
            <div className="empty-mic">🎙️</div>
            <p className="empty-title">중계 방송 준비 완료</p>
            <p className="empty-desc">
              기물을 착수하시면 전문 AI 해설위원의 수 분석과 실시간 중계가 이곳에 생생하게 방송됩니다.
            </p>
          </div>
        ) : (
          messages.map((msg, idx) => (
            <div
              key={msg.id || idx}
              className={`commentary-card ${msg.color.toLowerCase()}-card`}
            >
              <div className="card-top-meta">
                <div className="meta-left">
                  <span className={`player-color-pill ${msg.color.toLowerCase()}`}>
                    {msg.color === 'WHITE' ? '♔ 백' : '♚ 흑'}
                  </span>
                  <span className="move-number-tag">#{msg.moveNumber}</span>
                  <span className="move-notation-tag">{msg.notation}</span>
                  <span className={`tag-badge ${getTagBadgeClass(msg.tag)}`}>
                    {getTagLabel(msg.tag)}
                  </span>
                </div>
                <span className="commentary-time">{msg.timestamp}</span>
              </div>

              <div className="card-title">{msg.title}</div>
              <div className="card-body">{msg.body}</div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
