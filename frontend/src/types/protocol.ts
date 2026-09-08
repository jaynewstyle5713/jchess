import { GameEndReason, GameResult, Move, PieceColor, PieceType, Square } from './game';

export type CommandType =
  | 'PLAY_MOVE'
  | 'RESIGN'
  | 'OFFER_DRAW'
  | 'RESPOND_DRAW'
  | 'SYNC_STATE';

export type EventType =
  | 'GAME_STATE_SNAPSHOT'
  | 'GAME_STATE_UPDATED'
  | 'MOVE_REJECTED'
  | 'GAME_ENDED'
  | 'PLAYER_CONNECTION_CHANGED'
  | 'ERROR';

export interface CommandEnvelope<T = unknown> {
  type: CommandType;
  requestId: string;
  gameId: string;
  expectedGameVersion: number;
  payload: T;
}

export interface PlayMovePayload {
  from: Square;
  to: Square;
  promotion?: PieceType | null;
}

export interface RespondDrawPayload {
  accept: boolean;
}

export interface EventEnvelope<T = unknown> {
  eventId: string;
  gameId: string;
  gameVersion: number;
  eventType: EventType;
  occurredAt: string;
  payload: T;
}

export interface GameStateUpdatedPayload {
  moveNumber: number;
  turn: PieceColor;
  lastMove: Move;
  fen: string;
  isCheck: boolean;
  whiteRemainingTimeMs: number;
  blackRemainingTimeMs: number;
}

export interface MoveRejectedPayload {
  requestId: string;
  errorCode: string;
  message: string;
  expectedGameVersion: number;
}

export interface GameEndedPayload {
  result: GameResult;
  reason: GameEndReason;
  finalFen: string;
  endedAt: string;
}

export interface ErrorPayload {
  code: string;
  message: string;
  requestId?: string;
  retryable: boolean;
}
