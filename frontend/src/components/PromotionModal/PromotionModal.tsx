import React from 'react';
import { PieceColor, PieceType } from '../../types/game';
import './PromotionModal.css';

interface PromotionModalProps {
  color: PieceColor;
  onSelect: (piece: PieceType) => void;
  onCancel: () => void;
}

export const PromotionModal: React.FC<PromotionModalProps> = ({ color, onSelect, onCancel }) => {
  const pieces: { type: PieceType; symbol: string; label: string }[] = [
    { type: 'QUEEN', symbol: color === 'WHITE' ? '♕' : '♛', label: '퀸 (Queen)' },
    { type: 'ROOK', symbol: color === 'WHITE' ? '♖' : '♜', label: '룩 (Rook)' },
    { type: 'BISHOP', symbol: color === 'WHITE' ? '♗' : '♝', label: '비숍 (Bishop)' },
    { type: 'KNIGHT', symbol: color === 'WHITE' ? '♘' : '♞', label: '나이트 (Knight)' },
  ];

  return (
    <div className="promotion-modal-backdrop" onClick={onCancel} role="dialog" aria-modal="true" aria-label="프로모션 기물 선택">
      <div className="promotion-modal-content" onClick={(e) => e.stopPropagation()}>
        <h3>프로모션할 기물을 선택하세요</h3>
        <p className="promotion-subtitle">폰이 마지막 칸에 도달했습니다.</p>
        <div className="promotion-pieces">
          {pieces.map((p) => (
            <button
              key={p.type}
              className="promotion-piece-btn"
              onClick={() => onSelect(p.type)}
              aria-label={p.label}
              autoFocus={p.type === 'QUEEN'}
            >
              <span className="piece-symbol">{p.symbol}</span>
              <span className="piece-name">{p.type}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};
