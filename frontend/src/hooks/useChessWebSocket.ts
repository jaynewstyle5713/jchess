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

  // 콜백 함수들을 ref에 저장하여 connect 함수의 의존성에서 분리 (무한 재연결 루프 방지)
  const onSnapshotRef = useRef(onSnapshot);
  const onStateUpdatedRef = useRef(onStateUpdated);
  const onMoveRejectedRef = useRef(onMoveRejected);
  const onGameEndedRef = useRef(onGameEnded);
  const onErrorRef = useRef(onError);

  useEffect(() => {
    onSnapshotRef.current = onSnapshot;
    onStateUpdatedRef.current = onStateUpdated;
    onMoveRejectedRef.current = onMoveRejected;
    onGameEndedRef.current = onGameEnded;
    onErrorRef.current = onError;
  });

  const setGameVersion = useCallback((version: number) => {
    latestGameVersionRef.current = version;
  }, []);

  const connect = useCallback(() => {
    if (!gameId) return;

    if (wsRef.current && (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING)) {
      return; // 이미 연결되어 있거나 연결 중이면 중복 연결 방지
    }

    setConnectionStatus('RECONNECTING');

    const isDev = window.location.port === '3000';
    const targetHost = isDev ? `${window.location.hostname}:8080` : window.location.host;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${targetHost}/ws/games/${gameId}?playerId=${encodeURIComponent(playerId)}`;

    console.log('[jchess] WebSocket connecting to:', wsUrl);

    try {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('[jchess] WebSocket connected successfully');
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
              onSnapshotRef.current?.(envelope.payload as GameSnapshot);
              break;
            case 'GAME_STATE_UPDATED':
              onStateUpdatedRef.current?.(envelope.payload as GameStateUpdatedPayload, envelope.gameVersion);
              break;
            case 'MOVE_REJECTED':
              onMoveRejectedRef.current?.(envelope.payload as MoveRejectedPayload);
              break;
            case 'GAME_ENDED':
              onGameEndedRef.current?.(envelope.payload as GameEndedPayload);
              break;
            case 'ERROR':
              onErrorRef.current?.((envelope.payload as { message: string }).message || '알 수 없는 오류');
              break;
          }
        } catch (err) {
          console.error('[jchess] Failed to parse WebSocket message:', err);
        }
      };

      ws.onclose = (ev) => {
        console.warn('[jchess] WebSocket closed:', ev.code, ev.reason);
        setConnectionStatus('DISCONNECTED');
        wsRef.current = null;
      };

      ws.onerror = (err) => {
        console.error('[jchess] WebSocket error occurred:', err);
        setConnectionStatus('DISCONNECTED');
      };
    } catch (e) {
      console.error('[jchess] WebSocket connection failed to initialize:', e);
      setConnectionStatus('DISCONNECTED');
      wsRef.current = null;
    }
  }, [gameId, playerId]);

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
        onErrorRef.current?.('서버와 연결되어 있지 않습니다.');
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
    [gameId]
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
