import { Piece, PieceColor, PieceType, Square } from '../types/game';

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'] as const;
const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1'] as const;

export function coordsToSquare(col: number, row: number): Square {
  const file = FILES[col];
  const rank = RANKS[row];
  return `${file}${rank}` as Square;
}

export function squareToCoords(square: Square): { col: number; row: number } {
  const file = square[0];
  const rank = square[1];
  const col = FILES.indexOf(file as (typeof FILES)[number]);
  const row = RANKS.indexOf(rank as (typeof RANKS)[number]);
  return { col, row };
}

export function parseFen(fen: string): (Piece | null)[][] {
  const board: (Piece | null)[][] = Array(8)
    .fill(null)
    .map(() => Array(8).fill(null));

  const fenBoardPart = fen.split(' ')[0] || 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR';
  const ranks = fenBoardPart.split('/');

  for (let r = 0; r < 8 && r < ranks.length; r++) {
    const rankStr = ranks[r];
    let c = 0;
    for (let i = 0; i < rankStr.length && c < 8; i++) {
      const char = rankStr[i];
      if (char >= '1' && char <= '8') {
        c += parseInt(char, 10);
      } else {
        const isUpper = char === char.toUpperCase();
        const color: PieceColor = isUpper ? 'WHITE' : 'BLACK';
        let type: PieceType = 'PAWN';
        switch (char.toLowerCase()) {
          case 'p': type = 'PAWN'; break;
          case 'n': type = 'KNIGHT'; break;
          case 'b': type = 'BISHOP'; break;
          case 'r': type = 'ROOK'; break;
          case 'q': type = 'QUEEN'; break;
          case 'k': type = 'KING'; break;
        }
        board[r][c] = { type, color };
        c++;
      }
    }
  }

  return board;
}

export function getPieceSymbol(piece: Piece): string {
  if (piece.color === 'WHITE') {
    switch (piece.type) {
      case 'KING': return '♔';
      case 'QUEEN': return '♕';
      case 'ROOK': return '♖';
      case 'BISHOP': return '♗';
      case 'KNIGHT': return '♘';
      case 'PAWN': return '♙';
    }
  } else {
    switch (piece.type) {
      case 'KING': return '♚';
      case 'QUEEN': return '♛';
      case 'ROOK': return '♜';
      case 'BISHOP': return '♝';
      case 'KNIGHT': return '♞';
      case 'PAWN': return '♟';
    }
  }
}

export function isPawnPromotion(piece: Piece, _from: Square, to: Square): boolean {
  if (piece.type !== 'PAWN') return false;
  const toRank = to[1];
  return (piece.color === 'WHITE' && toRank === '8') || (piece.color === 'BLACK' && toRank === '1');
}

export function generateBasicLegalMoves(
  from: Square,
  piece: Piece,
  board: (Piece | null)[][]
): Square[] {
  const { col, row } = squareToCoords(from);
  const moves: Square[] = [];

  const isInside = (c: number, r: number) => c >= 0 && c < 8 && r >= 0 && r < 8;

  if (piece.type === 'PAWN') {
    const dir = piece.color === 'WHITE' ? -1 : 1;
    const startRow = piece.color === 'WHITE' ? 6 : 1;

    // 1칸 전진
    if (isInside(col, row + dir) && !board[row + dir][col]) {
      moves.push(coordsToSquare(col, row + dir));
      // 2칸 전진
      if (row === startRow && !board[row + 2 * dir][col]) {
        moves.push(coordsToSquare(col, row + 2 * dir));
      }
    }

    // 대각선 캡처
    for (const dCol of [-1, 1]) {
      const nc = col + dCol;
      const nr = row + dir;
      if (isInside(nc, nr)) {
        const target = board[nr][nc];
        if (target && target.color !== piece.color) {
          moves.push(coordsToSquare(nc, nr));
        }
      }
    }
  } else if (piece.type === 'KNIGHT') {
    const offsets = [
      [-2, -1], [-2, 1], [-1, -2], [-1, 2],
      [1, -2], [1, 2], [2, -1], [2, 1]
    ];
    for (const [dc, dr] of offsets) {
      const nc = col + dc;
      const nr = row + dr;
      if (isInside(nc, nr)) {
        const target = board[nr][nc];
        if (!target || target.color !== piece.color) {
          moves.push(coordsToSquare(nc, nr));
        }
      }
    }
  } else if (piece.type === 'BISHOP' || piece.type === 'ROOK' || piece.type === 'QUEEN') {
    const dirs: [number, number][] = [];
    if (piece.type === 'BISHOP' || piece.type === 'QUEEN') {
      dirs.push([-1, -1], [-1, 1], [1, -1], [1, 1]);
    }
    if (piece.type === 'ROOK' || piece.type === 'QUEEN') {
      dirs.push([-1, 0], [1, 0], [0, -1], [0, 1]);
    }

    for (const [dc, dr] of dirs) {
      let step = 1;
      while (true) {
        const nc = col + dc * step;
        const nr = row + dr * step;
        if (!isInside(nc, nr)) break;
        const target = board[nr][nc];
        if (!target) {
          moves.push(coordsToSquare(nc, nr));
        } else {
          if (target.color !== piece.color) {
            moves.push(coordsToSquare(nc, nr));
          }
          break;
        }
        step++;
      }
    }
  } else if (piece.type === 'KING') {
    const dirs = [
      [-1, -1], [-1, 0], [-1, 1],
      [0, -1],           [0, 1],
      [1, -1],  [1, 0],  [1, 1]
    ];
    for (const [dc, dr] of dirs) {
      const nc = col + dc;
      const nr = row + dr;
      if (isInside(nc, nr)) {
        const target = board[nr][nc];
        if (!target || target.color !== piece.color) {
          moves.push(coordsToSquare(nc, nr));
        }
      }
    }
    // 캐슬링 후보 (e1->g1/c1, e8->g8/c8)
    if (piece.color === 'WHITE' && from === 'e1') {
      if (!board[7][5] && !board[7][6] && board[7][7]?.type === 'ROOK') moves.push('g1');
      if (!board[7][3] && !board[7][2] && !board[7][1] && board[7][0]?.type === 'ROOK') moves.push('c1');
    } else if (piece.color === 'BLACK' && from === 'e8') {
      if (!board[0][5] && !board[0][6] && board[0][7]?.type === 'ROOK') moves.push('g8');
      if (!board[0][3] && !board[0][2] && !board[0][1] && board[0][0]?.type === 'ROOK') moves.push('c8');
    }
  }

  return moves;
}
