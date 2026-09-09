import React, { useState } from 'react';
import { GameMode, PieceColor } from '../../types/game';
import './Lobby.css';

interface LobbyProps {
  onCreateGame: (options: {
    gameMode: GameMode;
    aiLevel: number;
    baseMinutes: number;
    incrementSeconds: number;
    preferredColor: PieceColor | 'RANDOM';
    playerName: string;
  }) => void;
  onJoinGame: (gameId: string, playerName: string) => void;
  isLoading: boolean;
}

const TIME_PRESETS = [
  { name: 'Bullet 1+0', base: 1, inc: 0 },
  { name: 'Blitz 3+2', base: 3, inc: 2 },
  { name: 'Rapid 10+0', base: 10, inc: 0 },
  { name: 'Classical 15+10', base: 15, inc: 10 },
];

export const Lobby: React.FC<LobbyProps> = ({ onCreateGame, onJoinGame, isLoading }) => {
  const [gameMode, setGameMode] = useState<GameMode>('PVC');
  const [selectedTimeIdx, setSelectedTimeIdx] = useState(2); // Rapid 10+0
  const [preferredColor, setPreferredColor] = useState<PieceColor | 'RANDOM'>('WHITE');
  const [playerName, setPlayerName] = useState('플레이어');
  const [joinGameId, setJoinGameId] = useState('');

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const time = TIME_PRESETS[selectedTimeIdx];
    onCreateGame({
      gameMode,
      aiLevel: 2000,
      baseMinutes: time.base,
      incrementSeconds: time.inc,
      preferredColor,
      playerName: playerName.trim() || '플레이어',
    });
  };

  const handleJoin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinGameId.trim()) return;
    onJoinGame(joinGameId.trim(), playerName.trim() || '플레이어');
  };

  return (
    <div className="lobby-container">
      <div className="lobby-card">
        <h2 className="lobby-title">체스 대국 시작</h2>
        <p className="lobby-subtitle">새 게임을 생성하거나 참가할 방 ID를 입력하세요.</p>

        {/* 닉네임 입력 */}
        <div className="form-group">
          <label htmlFor="player-name">플레이어 닉네임</label>
          <input
            id="player-name"
            type="text"
            className="lobby-input"
            value={playerName}
            onChange={(e) => setPlayerName(e.target.value)}
            placeholder="닉네임을 입력하세요"
            maxLength={20}
          />
        </div>

        {/* 대국 모드 선택 */}
        <div className="form-group">
          <label>대국 모드</label>
          <div className="mode-toggle">
            <button
              type="button"
              className={`mode-btn ${gameMode === 'PVC' ? 'active' : ''}`}
              onClick={() => setGameMode('PVC')}
            >
              🤖 AI 대전 (Stockfish 2000+)
            </button>
            <button
              type="button"
              className={`mode-btn ${gameMode === 'PVP' ? 'active' : ''}`}
              onClick={() => setGameMode('PVP')}
            >
              👥 2인 대전 (PVP)
            </button>
          </div>
        </div>

        {/* 시간 형식 선택 */}
        <div className="form-group">
          <label>시간 제한</label>
          <div className="time-grid">
            {TIME_PRESETS.map((t, idx) => (
              <button
                key={t.name}
                type="button"
                className={`time-btn ${selectedTimeIdx === idx ? 'active' : ''}`}
                onClick={() => setSelectedTimeIdx(idx)}
              >
                {t.name}
              </button>
            ))}
          </div>
        </div>

        {/* 색상 선택 */}
        <div className="form-group">
          <label>색상 선택</label>
          <div className="color-toggle">
            <button
              type="button"
              className={`color-btn ${preferredColor === 'WHITE' ? 'active' : ''}`}
              onClick={() => setPreferredColor('WHITE')}
            >
              ♔ 백 (White)
            </button>
            <button
              type="button"
              className={`color-btn ${preferredColor === 'RANDOM' ? 'active' : ''}`}
              onClick={() => setPreferredColor('RANDOM')}
            >
              🎲 무작위 (Random)
            </button>
            <button
              type="button"
              className={`color-btn ${preferredColor === 'BLACK' ? 'active' : ''}`}
              onClick={() => setPreferredColor('BLACK')}
            >
              ♚ 흑 (Black)
            </button>
          </div>
        </div>

        {/* 새 게임 생성 버튼 */}
        <button
          type="button"
          className="create-game-btn"
          onClick={handleCreate}
          disabled={isLoading}
        >
          {isLoading ? '생성 중...' : gameMode === 'PVC' ? 'AI 대국 즉시 시작' : '새 대국 방 만들기'}
        </button>

        <div className="divider"><span>또는 기존 대국 참가</span></div>

        {/* 대국 ID 참가 폼 */}
        <form onSubmit={handleJoin} className="join-game-form">
          <input
            type="text"
            className="lobby-input"
            placeholder="초대받은 Game ID 입력"
            value={joinGameId}
            onChange={(e) => setJoinGameId(e.target.value)}
          />
          <button
            type="submit"
            className="join-game-btn"
            disabled={isLoading || !joinGameId.trim()}
          >
            참가
          </button>
        </form>
      </div>
    </div>
  );
};
