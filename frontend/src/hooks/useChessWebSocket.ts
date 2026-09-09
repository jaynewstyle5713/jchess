import { useCallback, useEffect, useRef, useState } from 'react';
import { ConnectionStatus, GameSnapshot, PieceType, Square } from '../types/game';
import { CommandEnvelope, EventEnvelope, GameEndedPayload, GameStateUpdatedPayload, MoveRejectedPayload, PlayMovePayload } from '../types/protocol';

interface UseChessWebSocketProps {
  gameId: string | null;
  playerId: string;
  onSnapshot?: (snapshot: GameSnapshot) => void;
  onStateUpdated?: (update: GameStateUpdatedPayload, gameVersion: number) => void;
  onMoveRejected?: (rejected: MoveRejectedPayload) => void;
  onGameEnded?: (ended: GameEndedPayload) => void;
  onError?: (message: string) => void;
}

export function useChessWebSocket({
  gameId,
  playerId,
  onSnapshot,
  onStateUpdated,
  onMoveRejected,
  onGameEnded,
  onError,
}: UseChessWebSocketProps) {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const latestGameVersionRef = useRef<number>(0);

  const setGameVersion = useCallback((version: number) => {
    latestGameVersionRef.current = version;
  }, []);

  const connect = useCallback(() => {
    if (!gameId) return;

    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }

    setConnectionStatus('RECONNECTING');
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws/games/${gameId}?playerId=${encodeURIComponent(playerId)}`;

    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnectionStatus('CONNECTED');
    };

    ws.onmessage = (event) => {
      try {
        const envelope: EventEnvelope = JSON.parse(event.data);
        if (envelope.gameVersion !== undefined) {
          latestGameVersionRef.current = envelope.gameVersion;
        }

        switch (envelope.eventType) {
          case 'GAME_STATE_SNAPSHOT':
            onSnapshot?.(envelope.payload as GameSnapshot);
            break;
          case 'GAME_STATE_UPDATED':
            onStateUpdated?.(envelope.payload as GameStateUpdatedPayload, envelope.gameVersion);
            break;
          case 'MOVE_REJECTED':
            onMoveRejected?.(envelope.payload as MoveRejectedPayload);
            break;
          case 'GAME_ENDED':
            onGameEnded?.(envelope.payload as GameEndedPayload);
            break;
          case 'ERROR':
            onError?.((envelope.payload as { message: string }).message || '알 수 없는 오류');
            break;
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message:', err);
      }
    };

    ws.onclose = () => {
      setConnectionStatus('DISCONNECTED');
    };

    ws.onerror = () => {
      setConnectionStatus('DISCONNECTED');
    };
  }, [gameId, playerId, onSnapshot, onStateUpdated, onMoveRejected, onGameEnded, onError]);

  useEffect(() => {
    if (gameId) {
      connect();
    }
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, [gameId, connect]);

  const sendCommand = useCallback(
    <T,>(type: CommandEnvelope['type'], payload: T) => {
      if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN || !gameId) {
        onError?.('서버와 연결되어 있지 않습니다.');
        return;
      }

      const envelope: CommandEnvelope<T> = {
        type,
        requestId: `req-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
        gameId,
        expectedGameVersion: latestGameVersionRef.current,
        payload,
      };

      wsRef.current.send(JSON.stringify(envelope));
    },
    [gameId, onError]
  );

  const sendMove = useCallback(
    (from: Square, to: Square, promotion?: PieceType | null) => {
      sendCommand<PlayMovePayload>('PLAY_MOVE', { from, to, promotion });
    },
    [sendCommand]
  );

  const sendResign = useCallback(() => {
    sendCommand('RESIGN', {});
  }, [sendCommand]);

  const sendOfferDraw = useCallback(() => {
    sendCommand('OFFER_DRAW', {});
  }, [sendCommand]);

  const sendSyncState = useCallback(() => {
    sendCommand('SYNC_STATE', {});
  }, [sendCommand]);

  return {
    connectionStatus,
    sendMove,
    sendResign,
    sendOfferDraw,
    sendSyncState,
    setGameVersion,
    reconnect: connect,
  };
}
