import { PieceColor, PieceType, Square } from '../types/game';

export interface CommentaryMessage {
  id: string;
  moveNumber: number;
  color: PieceColor;
  playerName: string;
  notation: string;
  fromSquare?: Square;
  toSquare?: Square;
  title: string;
  body: string;
  tag: 'OPENING' | 'BEST' | 'GOOD' | 'TACTICAL' | 'CASTLING' | 'CHECK' | 'CAPTURE' | 'PROMOTION' | 'ENDGAME' | 'MATE' | 'INFO';
  timestamp: string;
}

// 오프닝 북 데이터베이스 (훈련용 핵심 오프닝)
const OPENING_BOOK: Record<string, { name: string; desc: string }> = {
  'e2e4': { name: '킹스 폰 오프닝 (King\'s Pawn)', desc: '중앙 e4를 장악하고 비숍과 퀸의 길을 여는 가장 고전적이고 강력한 첫 수입니다.' },
  'd2d4': { name: '퀸스 폰 오프닝 (Queen\'s Pawn)', desc: 'd4로 견고한 중앙 진지를 구축하며 퀸의 보호를 받는 안정적인 전략적 수입니다.' },
  'c2c4': { name: '잉글리시 오프닝 (English Opening)', desc: '측면 폰으로 중앙 d5 칸을 간접 제어하는 깊이 있는 포지셔널 오프닝입니다.' },
  'g1f3': { name: '레티 오프닝 (Réti Opening)', desc: '나이트를 즉시 전개하며 중앙 d4, e5 칸을 통제하는 유연한 오프닝 수입니다.' },
  'e2e4_e7e5': { name: '오픈 게임 (Open Game)', desc: '흑이 대칭적으로 맞받아쳐 백의 중앙 독점을 방어하며 주도권 싸움을 선언합니다.' },
  'e2e4_c7c5': { name: '시실리안 디펜스 (Sicilian Defense)', desc: '흑의 가장 날카로운 카운터 오프닝입니다! d4 칸을 비대칭으로 공략하여 승리를 노립니다.' },
  'e2e4_e7e6': { name: '프렌치 디펜스 (French Defense)', desc: 'd5 전진을 준비하는 견고한 방어 체계로, 탄탄한 폰 사슬을 형성합니다.' },
  'e2e4_c7c6': { name: '카로-칸 디펜스 (Caro-Kann Defense)', desc: '폰 구조를 견고하게 유지하면서 다음 수 d5로 중앙 반격을 도모하는 단단한 수입니다.' },
  'e2e4_d7d5': { name: '스칸디나비안 디펜스 (Scandinavian Defense)', desc: '즉시 백의 e4 폰에 중앙 도전을 가하는 직접적이고 공격적인 수입니다.' },
  'd2d4_d7d5': { name: '클로즈드 게임 (Closed Game)', desc: '중앙 d-폰이 맞부딪히며 치열한 전략적 포지션 싸움이 예고됩니다.' },
  'd2d4_g8f6': { name: '인디언 디펜스 (Indian Defense)', desc: '나이트를 먼저 전개하여 백의 e4 중앙 장악을 견제하는 현대적인 오프닝입니다.' },
};
/**
 * 착수 정보를 바탕으로 실시간 전문 체스 아나운서 해설 생성
 */
export function generateCommentary(params: {
  moveNumber: number;
  color: PieceColor;
  playerName: string;
  from: string;
  to: string;
  notation: string;
  promotionPiece?: PieceType | null;
  isCheck: boolean;
  isCheckmate?: boolean;
  isStalemate?: boolean;
  capturedPiece?: string | null;
  historyMoves?: string[];
}): CommentaryMessage {
  const {
    moveNumber,
    color,
    playerName,
    from,
    to,
    notation,
    promotionPiece,
    isCheck,
    isCheckmate,
    isStalemate,
    capturedPiece,
    historyMoves = [],
  } = params;

  const now = new Date();
  const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  const moveKey = `${from}${to}`;
  const colorKo = color === 'WHITE' ? '백' : '흑';

  // 1. 체크메이트 (외통수)
  if (isCheckmate || notation.includes('#')) {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '👑 외통수! 체크메이트 (Checkmate)',
      body: `${playerName}님이 ${notation} 착수로 상대 킹을 완벽히 포위하며 체크메이트를 성공시켰습니다! 이번 대국의 눈부신 명승부가 승리로 결착납니다!`,
      tag: 'MATE',
      timestamp: timeStr,
    };
  }

  // 2. 스테일메이트
  if (isStalemate) {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🤝 스테일메이트 (Stalemate)',
      body: `킹이 체크 상태는 아니지만 둘 수 있는 합법적인 수가 전혀 없습니다. 규정에 따라 극적인 무승부로 경기가 마무리됩니다.`,
      tag: 'ENDGAME',
      timestamp: timeStr,
    };
  }

  // 3. 프로모션 (승급)
  if (promotionPiece) {
    const pieceNameKo = promotionPiece === 'QUEEN' ? '퀸(Queen)' : promotionPiece === 'KNIGHT' ? '나이트(Knight)' : promotionPiece === 'ROOK' ? '룩(Rook)' : '비숍(Bishop)';
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: `👑 폰 승급! ${pieceNameKo} 탄생`,
      body: `${playerName}님의 폰이 상대 진영 끝선 ${to}에 도달하여 막강한 ${pieceNameKo}으로 승급하셨습니다! 전력 균형이 크게 요동칩니다.`,
      tag: 'PROMOTION',
      timestamp: timeStr,
    };
  }

  // 4. 캐슬링
  if (notation === 'O-O' || notation === 'O-O-O' || (from === 'e1' && (to === 'g1' || to === 'c1')) || (from === 'e8' && (to === 'g8' || to === 'c8'))) {
    const isKingSide = notation === 'O-O' || to === 'g1' || to === 'g8';
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: `🏰 ${isKingSide ? '킹사이드' : '퀸사이드'} 캐슬링 단행`,
      body: `${playerName}님이 캐슬링으로 킹의 안전을 단단히 확보하고, 룩을 중앙 열로 배치하여 활성화를 준비하셨습니다. 정석적인 수비와 공격의 조화입니다.`,
      tag: 'CASTLING',
      timestamp: timeStr,
    };
  }

  // 5. 체크 위협
  if (isCheck || notation.includes('+')) {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '⚡ 날카로운 체크! (Check)',
      body: `${playerName}님이 ${notation}로 상대 킹을 직접 타격하며 체크를 선언하셨습니다! 상대는 킹 이동, 기물 방어, 또는 포획 중 즉각적인 응수를 취해야 합니다.`,
      tag: 'CHECK',
      timestamp: timeStr,
    };
  }

  // 6. 오프닝 초반 1~2수 해설
  if (moveNumber === 1 && color === 'WHITE') {
    const op = OPENING_BOOK[moveKey];
    if (op) {
      return {
        id: `comm-${Date.now()}-${Math.random()}`,
        moveNumber,
        color,
        playerName,
        notation,
        fromSquare: from as Square,
        toSquare: to as Square,
        title: `📖 ${op.name}`,
        body: `${playerName}님이 첫 수로 ${notation}를 착수하셨습니다. ${op.desc}`,
        tag: 'OPENING',
        timestamp: timeStr,
      };
    }
  }

  if (moveNumber === 1 && color === 'BLACK') {
    const firstWhiteMove = historyMoves[0] || 'e2e4';
    const comboKey = `${firstWhiteMove}_${moveKey}`;
    const op = OPENING_BOOK[comboKey] || OPENING_BOOK[moveKey];
    if (op) {
      return {
        id: `comm-${Date.now()}-${Math.random()}`,
        moveNumber,
        color,
        playerName,
        notation,
        fromSquare: from as Square,
        toSquare: to as Square,
        title: `🛡️ ${op.name}`,
        body: `${playerName}님이 ${notation}로 맞대응하셨습니다. ${op.desc}`,
        tag: 'OPENING',
        timestamp: timeStr,
      };
    }
  }

  // 7. 기물 포획(Capture)
  if (capturedPiece || notation.includes('x')) {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '⚔️ 전술적 기물 포획 및 교환',
      body: `${playerName}님이 ${from}에서 ${to}로 이동하며 상대 기물을 잡아내셨습니다(${notation}). 중앙의 텐션이 폭발하며 기물 가치 교환이 전개됩니다.`,
      tag: 'CAPTURE',
      timestamp: timeStr,
    };
  }

  // 8. 중앙 지배 및 기물 전개
  if (to === 'e4' || to === 'd4' || to === 'e5' || to === 'd5') {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🎯 강력한 중앙 장악 (Center Control)',
      body: `${playerName}님이 핵심 요충지인 ${to} 칸을 차지하며 보드 중앙의 주도권을 장악하셨습니다. 체스 훈련의 기본 원칙에 매우 충실한 좋은 수입니다.`,
      tag: 'BEST',
      timestamp: timeStr,
    };
  }

  if (from.startsWith('b1') || from.startsWith('g1') || from.startsWith('b8') || from.startsWith('g8') || from.startsWith('c1') || from.startsWith('f1') || from.startsWith('c8') || from.startsWith('f8')) {
    return {
      id: `comm-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🐎 기물 기동 및 전개 활성화',
      body: `${playerName}님이 ${notation}로 마이너 피스(나이트/비숍)를 공격적인 전진 기지로 배치하셨습니다. 아군 기물 간의 유기적 연계가 돋보입니다.`,
      tag: 'GOOD',
      timestamp: timeStr,
    };
  }

  // 9. 일반 포지셔널 무브
  return {
    id: `comm-${Date.now()}-${Math.random()}`,
    moveNumber,
    color,
    playerName,
    notation,
    fromSquare: from as Square,
    toSquare: to as Square,
    title: `♟️ ${colorKo} ${notation} 포지션 전개`,
    body: `${playerName}님이 ${from} ➔ ${to}로 침착하게 기물을 이동시키며 진영의 균형을 유지하고 계십니다. 상대방의 다음 응수를 기다립니다.`,
    tag: 'TACTICAL',
    timestamp: timeStr,
  };
}
