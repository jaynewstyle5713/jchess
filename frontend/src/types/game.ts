export type PieceColor = 'WHITE' | 'BLACK';

export type PieceType = 'PAWN' | 'KNIGHT' | 'BISHOP' | 'ROOK' | 'QUEEN' | 'KING';

export type Square = `${'a'|'b'|'c'|'d'|'e'|'f'|'g'|'h'}${1|2|3|4|5|6|7|8}`;

export type GameMode = 'PVP' | 'PVC';

export type GameStatus =
  | 'WAITING_FOR_OPPONENT'
  | 'ACTIVE'
  | 'CHECK'
  | 'CHECKMATE'
  | 'STALEMATE'
  | 'DRAW'
  | 'RESIGNED'
  | 'TIMEOUT';

export type GameResult = 'WHITE_WON' | 'BLACK_WON' | 'DRAW';

export type GameEndReason =
  | 'CHECKMATE'
  | 'STALEMATE'
  | 'RESIGNATION'
  | 'TIMEOUT'
  | 'AGREED_DRAW'
  | 'INSUFFICIENT_MATERIAL'
  | 'THREEFOLD_REPETITION'
  | 'FIFTY_MOVE_RULE';

export type ConnectionStatus = 'CONNECTED' | 'RECONNECTING' | 'SYNCING' | 'DISCONNECTED';

export interface Piece {
  type: PieceType;
  color: PieceColor;
}

export interface PlayerInfo {
  id: string;
  name: string;
  remainingTimeMs: number;
  isOnline: boolean;
  isAi?: boolean;
}

export interface Move {
  from: Square;
  to: Square;
  piece: PieceType;
  captured?: PieceType | null;
  promotion?: PieceType | null;
  notation: string;
}

export interface PendingPromotion {
  color: PieceColor;
  from: Square;
  to: Square;
}

export interface GameSnapshot {
  gameId: string;
  gameMode?: GameMode;
  aiLevel?: number;
  gameStatus: GameStatus;
  gameVersion: number;
  turn: PieceColor;
  fen: string;
  whitePlayer: PlayerInfo;
  blackPlayer: PlayerInfo | null;
  lastMove?: Move | null;
  pendingPromotion?: PendingPromotion | null;
  isCheck: boolean;
  result?: GameResult | null;
  endReason?: GameEndReason | null;
}

