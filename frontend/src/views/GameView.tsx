import React from 'react';
import { GameSnapshot, PieceColor, PieceType, Square } from '../types/game';
import { Chessboard } from '../components/Chessboard/Chessboard';
import { GamePanel } from '../components/GamePanel/GamePanel';
import { PromotionModal } from '../components/PromotionModal/PromotionModal';

interface GameViewProps {
  gameState: GameSnapshot;
  myColor: PieceColor | null;
  promoMove: { from: Square; to: Square } | null;
  onMove: (from: Square, to: Square) => void;
  onRequestPromotion: (from: Square, to: Square) => void;
  onSelectPromotion: (piece: PieceType) => void;
  onCancelPromotion: () => void;
  onResign: () => void;
  onOfferDraw: () => void;
  onSync: () => void;
  onLeave: () => void;
}

export const GameView: React.FC<GameViewProps> = ({
  gameState,
  myColor,
  promoMove,
  onMove,
  onRequestPromotion,
  onSelectPromotion,
  onCancelPromotion,
  onResign,
  onOfferDraw,
  onSync,
  onLeave,
}) => {
  return (
    <div className="game-view-container">
      <Chessboard
        fen={gameState.fen}
        orientation={myColor || 'WHITE'}
        turn={gameState.turn}
        isCheck={gameState.isCheck}
        lastMove={gameState.lastMove}
        interactive={['ACTIVE', 'CHECK'].includes(gameState.gameStatus)}
        myColor={myColor}
        onMove={onMove}
        onRequestPromotion={onRequestPromotion}
      />
      <GamePanel
        status={gameState.gameStatus}
        turn={gameState.turn}
        isCheck={gameState.isCheck}
        whitePlayer={gameState.whitePlayer}
        blackPlayer={gameState.blackPlayer}
        myColor={myColor}
        result={gameState.result}
        endReason={gameState.endReason}
        onResign={onResign}
        onOfferDraw={onOfferDraw}
        onSync={onSync}
        onLeave={onLeave}
      />
      {promoMove && (
        <PromotionModal
          color={myColor || 'WHITE'}
          onSelect={onSelectPromotion}
          onCancel={onCancelPromotion}
        />
      )}
    </div>
  );
};
