import { GameMode, PieceColor } from '../types/game';

export interface CreateGameParams {
  gameMode: GameMode;
  aiLevel: number;
  baseMinutes: number;
  incrementSeconds: number;
  preferredColor: PieceColor | 'RANDOM';
  playerId?: string;
  playerName?: string;
}

export async function createGameApi(params: CreateGameParams) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (params.playerId) {
    headers['X-Player-Id'] = params.playerId;
  }
  if (params.playerName) {
    headers['X-Player-Name'] = params.playerName;
  }

  const resp = await fetch('/api/v1/games', {
    method: 'POST',
    headers,
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
  if (!resp.ok) {
    const errorData = await resp.json().catch(() => ({}));
    throw new Error(errorData.message || '게임 생성에 실패했습니다.');
  }
  return resp.json();
}

export async function joinGameApi(gameId: string, playerId?: string, playerName?: string) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (playerId) {
    headers['X-Player-Id'] = playerId;
  }
  if (playerName) {
    headers['X-Player-Name'] = playerName;
  }

  const resp = await fetch(`/api/v1/games/${gameId}/join`, {
    method: 'POST',
    headers,
  });
  if (!resp.ok) {
    const errorData = await resp.json().catch(() => ({}));
    throw new Error(errorData.message || '대국 참가에 실패했습니다.');
  }
  return resp.json();
}
