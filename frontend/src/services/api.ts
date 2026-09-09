import { GameMode, PieceColor } from '../types/game';

export interface CreateGameParams {
  gameMode: GameMode;
  aiLevel: number;
  baseMinutes: number;
  incrementSeconds: number;
  preferredColor: PieceColor | 'RANDOM';
}

export async function createGameApi(params: CreateGameParams) {
  const resp = await fetch('/api/v1/games', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      gameMode: params.gameMode,
      aiLevel: params.aiLevel,
      timeControl: {
        baseMinutes: params.baseMinutes,
        incrementSeconds: params.incrementSeconds,
      },
      preferredColor: params.preferredColor,
    }),
  });
  if (!resp.ok) throw new Error('게임 생성에 실패했습니다.');
  return resp.json();
}

export async function joinGameApi(gameId: string) {
  const resp = await fetch(`/api/v1/games/${gameId}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  if (!resp.ok) throw new Error('대국 참가에 실패했습니다.');
  return resp.json();
}
