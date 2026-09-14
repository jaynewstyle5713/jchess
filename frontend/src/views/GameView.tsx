import React from 'react';
import { GameSnapshot, PieceColor, PieceType, Square } from '../types/game';
import { Chessboard } from '../components/Chessboard/Chessboard';
import { GamePanel } from '../components/GamePanel/GamePanel';
import { PromotionModal } from '../components/PromotionModal/PromotionModal';
import { LiveCommentaryPanel } from '../components/LiveCommentary/LiveCommentaryPanel';
import { CommentaryMessage } from '../utils/commentaryEngine';

interface GameViewProps {
  gameState: GameSnapshot;
  myColor: PieceColor | null;
  promoMove: { from: Square; to: Square } | null;
  hintMove?: { from: Square; to: Square } | null;
  commentaryMessages?: CommentaryMessage[];
  onRequestHint?: () => void;
  onRequestUndo?: () => void;
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
  hintMove = null,
  commentaryMessages = [],
  onRequestHint,
  onRequestUndo,
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
        hintMove={hintMove}
        interactive={['ACTIVE', 'CHECK'].includes(gameState.gameStatus)}
        myColor={myColor}
        onMove={onMove}
        onRequestPromotion={onRequestPromotion}
      />
      <GamePanel
        fen={gameState.fen}
        status={gameState.gameStatus}
        turn={gameState.turn}
        isCheck={gameState.isCheck}
        whitePlayer={gameState.whitePlayer}
        blackPlayer={gameState.blackPlayer}
        myColor={myColor}
        result={gameState.result}
        endReason={gameState.endReason}
        isAiGame={gameState.gameMode === 'PVC'}
        remainingHints={gameState.remainingHints ?? 3}
        maxUndos={gameState.maxUndos ?? 3}
        remainingUndos={gameState.remainingUndos ?? 3}
        onRequestHint={onRequestHint}
        onRequestUndo={onRequestUndo}
        onResign={onResign}
        onOfferDraw={onOfferDraw}
        onSync={onSync}
        onLeave={onLeave}
      />
      <LiveCommentaryPanel
        messages={commentaryMessages}
        whitePlayerName={gameState.whitePlayer.name}
        blackPlayerName={gameState.blackPlayer?.name || '상대 대기 중...'}
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

