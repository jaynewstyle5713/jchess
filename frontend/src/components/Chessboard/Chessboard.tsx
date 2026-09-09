import React, { useState, useMemo } from 'react';
import { Move, PieceColor, Square } from '../../types/game';
import { coordsToSquare, getPieceSymbol, parseFen, squareToCoords } from '../../utils/chess';
import './Chessboard.css';

interface ChessboardProps {
  fen: string;
  orientation?: PieceColor;
  turn: PieceColor;
  isCheck?: boolean;
  lastMove?: Move | null;
  interactive?: boolean;
  myColor?: PieceColor | null;
  onMove: (from: Square, to: Square) => void;
  onRequestPromotion?: (from: Square, to: Square) => void;
}

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1'];

export const Chessboard: React.FC<ChessboardProps> = ({
  fen,
  orientation = 'WHITE',
  turn,
  isCheck = false,
  lastMove = null,
  interactive = true,
  myColor = null,
  onMove,
  onRequestPromotion,
}) => {
  const [selectedSquare, setSelectedSquare] = useState<Square | null>(null);
  const [draggedSquare, setDraggedSquare] = useState<Square | null>(null);

  const board = useMemo(() => parseFen(fen), [fen]);
  const rows = useMemo(() => orientation === 'WHITE' ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0], [orientation]);
  const cols = useMemo(() => orientation === 'WHITE' ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0], [orientation]);

  const checkKingSquare = useMemo(() => {
    if (!isCheck) return null;
    for (let r = 0; r < 8; r++) {
      for (let c = 0; c < 8; c++) {
        const piece = board[r][c];
        if (piece?.type === 'KING' && piece.color === turn) {
          return coordsToSquare(c, r);
        }
      }
    }
    return null;
  }, [isCheck, board, turn]);

  const executeMove = (from: Square, to: Square) => {
    const { col: fCol, row: fRow } = squareToCoords(from);
    const piece = board[fRow][fCol];
    if (!piece) return;

    const isPromo = piece.type === 'PAWN' &&
      ((piece.color === 'WHITE' && to[1] === '8') || (piece.color === 'BLACK' && to[1] === '1'));

    if (isPromo && onRequestPromotion) {
      onRequestPromotion(from, to);
    } else {
      onMove(from, to);
    }
    setSelectedSquare(null);
  };

  const onSquareClick = (square: Square) => {
    if (!interactive || (myColor && myColor !== turn)) return;
    const { col, row } = squareToCoords(square);
    const piece = board[row][col];

    if (selectedSquare) {
      if (selectedSquare === square) {
        setSelectedSquare(null);
      } else if (piece && piece.color === turn && (!myColor || piece.color === myColor)) {
        setSelectedSquare(square);
      } else {
        executeMove(selectedSquare, square);
      }
    } else if (piece && piece.color === turn && (!myColor || piece.color === myColor)) {
      setSelectedSquare(square);
    }
  };

  return (
    <div className="chessboard-wrapper">
      <div className="chessboard" role="grid" aria-label="체스 보드">
        {rows.map((r) => (
          <div key={`row-${r}`} className="board-row" role="row">
            {cols.map((c) => {
              const square = coordsToSquare(c, r);
              const piece = board[r][c];
              const isLight = (r + c) % 2 === 0;
              const isSelected = selectedSquare === square;
              const isLast = lastMove?.from === square || lastMove?.to === square;
              const isKingInCheck = checkKingSquare === square;

              let cls = `square ${isLight ? 'light' : 'dark'}`;
              if (isSelected) cls += ' selected';
              if (isLast) cls += ' last-move';
              if (isKingInCheck) cls += ' in-check';

              const rankLbl = orientation === 'WHITE' ? (c === 0 ? RANKS[r] : null) : (c === 7 ? RANKS[r] : null);
              const fileLbl = orientation === 'WHITE' ? (r === 7 ? FILES[c] : null) : (r === 0 ? FILES[c] : null);

              return (
                <div
                  key={square}
                  className={cls}
                  role="gridcell"
                  aria-label={`${square} ${piece ? `${piece.color} ${piece.type}` : 'empty'}`}
                  tabIndex={interactive ? 0 : -1}
                  onClick={() => onSquareClick(square)}
                  onDragOver={(e) => { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; }}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (draggedSquare && draggedSquare !== square) executeMove(draggedSquare, square);
                    setDraggedSquare(null);
                  }}
                >
                  {rankLbl && <span className="coordinate-rank">{rankLbl}</span>}
                  {fileLbl && <span className="coordinate-file">{fileLbl}</span>}

                  {piece && (
                    <div
                      className={`chess-piece ${piece.color.toLowerCase()}`}
                      draggable={interactive && piece.color === turn && (!myColor || piece.color === myColor)}
                      onDragStart={(e) => {
                        setDraggedSquare(square);
                        setSelectedSquare(square);
                        e.dataTransfer.setData('text/plain', square);
                      }}
                    >
                      {getPieceSymbol(piece)}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
};
