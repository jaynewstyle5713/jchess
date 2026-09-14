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
  tag:
    | 'MACHINE'
    | 'BRILLIANCY'
    | 'THEORY'
    | 'TACTICAL'
    | 'POSITIONAL'
    | 'CASTLING'
    | 'CHECK'
    | 'CAPTURE'
    | 'PROMOTION'
    | 'ENDGAME'
    | 'MATE'
    | 'INFO';
  timestamp: string;
}

// FIDE / Grandmaster Opening Theory Database (SWT Agent)
const OPENING_BOOK: Record<string, { name: string; eco: string; desc: string }> = {
  'e2e4': {
    name: "킹스 폰 오프닝 (King's Pawn Opening)",
    eco: 'B00',
    desc: 'FIDE 챔피언십에서 가장 높은 빈도로 등장하는 백의 표준 공격 개시입니다. e4로 중앙을 타격하며 비숍과 퀸의 길을 전면 개방합니다.',
  },
  'e2e4_e7e5': {
    name: '오픈 게임 (Open Game / King\'s Pawn Game)',
    eco: 'C20',
    desc: '흑이 대칭적으로 d4 진입을 차단하며 클래식한 중앙 주도권 쟁탈전을 선언합니다. 치열한 전술 공방이 예고됩니다.',
  },
  'e2e4_c7c5': {
    name: '시실리안 디펜스 (Sicilian Defense)',
    eco: 'B20',
    desc: '흑의 승률 1위 비대칭 카운터 오프닝! 중앙 d4를 측면 폰으로 공략하여 백에게 불균형과 날카로운 전술 난타전을 강요합니다.',
  },
  'e2e4_e7e6': {
    name: '프렌치 디펜스 (French Defense)',
    eco: 'C00',
    desc: '견고한 e6-d5 폰 사슬을 구축하여 백의 중앙 진군을 저지하고 퀸사이드 반격을 노리는 탄탄한 방어 체계입니다.',
  },
  'e2e4_c7c6': {
    name: '카로-칸 디펜스 (Caro-Kann Defense)',
    eco: 'B10',
    desc: '‘난공불락의 요새’라 불리는 견고함의 극치! 폰 구조의 약점 없이 안정적인 포지셔널 엔드게임을 도모합니다.',
  },
  'e2e4_d7d5': {
    name: '스칸디나비안 디펜스 (Scandinavian Defense)',
    eco: 'B01',
    desc: '1수부터 백의 e4를 직접 타격하는 공격적 수입니다. 이른 퀸 전개에 따른 템포 관리가 승부의 핵심입니다.',
  },
  'd2d4': {
    name: "퀸스 폰 오프닝 (Queen's Pawn Opening)",
    eco: 'D00',
    desc: '퀸의 보호 속에 d4를 단단히 선점하여 포지셔널한 전략적 주도권을 가져오는 마스터급 개시 수입니다.',
  },
  'd2d4_d7d5': {
    name: '클로즈드 게임 (Closed Game)',
    eco: 'D00',
    desc: '양측의 d-폰이 정면 충돌하며 기동성과 폰 구조의 치밀한 두뇌 싸움이 시작됩니다.',
  },
  'd2d4_g8f6': {
    name: '인디언 디펜스 (Indian Defense)',
    eco: 'A45',
    desc: 'd4를 직접 막지 않고 나이트로 e4를 견제하는 하이퍼모던 전략입니다. 현대 GM 대국의 핵심 라인입니다.',
  },
  'c2c4': {
    name: '잉글리시 오프닝 (English Opening)',
    eco: 'A10',
    desc: '측면 c-폰으로 중앙 d5 칸을 간접 제압하며 유연한 미들게임을 설계하는 고품격 전략 오프닝입니다.',
  },
  'g1f3': {
    name: '레티 오프닝 (Réti Opening)',
    eco: 'A04',
    desc: '상대의 폰 구조를 유도한 뒤 측면 피앙케토로 중앙을 공략하는 하이퍼모던주의의 진수입니다.',
  },
};
/**
 * SWT (Scenario Writer Agent) 전문 IM/GM급 실시간 체스 해설 생성기
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
  isAiMove?: boolean;
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
    isAiMove = false,
  } = params;

  const now = new Date();
  const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
  const moveKey = `${from}${to}`;
  const isAi = isAiMove || playerName.includes('Stockfish') || playerName.includes('AI');

  // 1. 체크메이트 (외통수)
  if (isCheckmate || notation.includes('#')) {
    const mateBody = isAi
      ? `🤖 [Machine Precision] Stockfish 엔진이 한 치의 오차도 없는 외통수 라인을 완성했습니다. ${playerName}의 완벽한 계산 승리입니다!`
      : `👑 [Grandmaster Finish] ${playerName} 선수가 ${notation} 착수로 킹의 퇴로를 전면 차단하며 체크메이트를 성공시켰습니다!`;
    return {
      id: `comm-mate-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '👑 외통수! 체크메이트 (Checkmate)',
      body: mateBody,
      tag: 'MATE',
      timestamp: timeStr,
    };
  }

  // 2. 스테일메이트
  if (isStalemate) {
    return {
      id: `comm-stale-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🤝 극적 스테일메이트 (Stalemate)',
      body: `체크 상태가 아님에도 둘 수 있는 합법적인 수가 완전히 고갈되었습니다. 규정에 따라 무승부로 결착됩니다.`,
      tag: 'ENDGAME',
      timestamp: timeStr,
    };
  }

  // 3. 폰 승급 (Promotion)
  if (promotionPiece) {
    const pieceKo =
      promotionPiece === 'QUEEN'
        ? '퀸 (Queen)'
        : promotionPiece === 'KNIGHT'
        ? '나이트 (Knight)'
        : promotionPiece === 'ROOK'
        ? '룩 (Rook)'
        : '비숍 (Bishop)';
    return {
      id: `comm-promo-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: `👑 폰 승급! 강력한 ${pieceKo} 탄생`,
      body: `${playerName}의 폰이 적진 끝선(${to})에 침투하여 ${pieceKo}으로 승급했습니다! 국면의 전력 균형이 완전히 요동칩니다.`,
      tag: 'PROMOTION',
      timestamp: timeStr,
    };
  }

  // 4. 캐슬링 (Castling)
  if (
    notation === 'O-O' ||
    notation === 'O-O-O' ||
    (from === 'e1' && (to === 'g1' || to === 'c1')) ||
    (from === 'e8' && (to === 'g8' || to === 'c8'))
  ) {
    const isKingSide = notation === 'O-O' || to === 'g1' || to === 'g8';
    const sideName = isKingSide ? '킹사이드 (O-O)' : '퀸사이드 (O-O-O)';
    return {
      id: `comm-castle-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: `🏰 ${sideName} 캐슬링 단행`,
      body: `${playerName} 선수가 캐슬링으로 킹의 안전(King Safety)을 확고히 다지고 룩을 중앙 오픈 파일로 활성화했습니다.`,
      tag: 'CASTLING',
      timestamp: timeStr,
    };
  }

  // 5. 체크 (Check)
  if (isCheck || notation.includes('+')) {
    const checkTitle = isAi ? '⚡ Stockfish의 정밀 체크 (Tactical Check)' : '⚡ 날카로운 체크! (Check)';
    const checkBody = isAi
      ? `🤖 [Engine Refutation] AI 엔진이 ${notation}로 킹을 직접 타격하며 상대 진영의 구조적 약점을 파고듭니다. 정밀한 수비가 강요됩니다.`
      : `${playerName} 선수가 ${notation}로 킹을 직접 겨냥했습니다! 상대는 즉각적인 응수를 취해야 합니다.`;
    return {
      id: `comm-check-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: checkTitle,
      body: checkBody,
      tag: 'CHECK',
      timestamp: timeStr,
    };
  }

  // 6. 오프닝 북 이론 (Opening Theory)
  if (moveNumber <= 3) {
    const op = OPENING_BOOK[moveKey];
    if (op) {
      return {
        id: `comm-theory-${Date.now()}-${Math.random()}`,
        moveNumber,
        color,
        playerName,
        notation,
        fromSquare: from as Square,
        toSquare: to as Square,
        title: `📖 [${op.eco}] ${op.name}`,
        body: `${playerName} 선수의 ${notation} 착수! ${op.desc}`,
        tag: 'THEORY',
        timestamp: timeStr,
      };
    }
  }

  // 7. AI의 전형적인 Machine Move
  if (isAi && (moveNumber >= 4 || notation.includes('x') || to.endsWith('5') || to.endsWith('4'))) {
    const machineComments = [
      {
        title: '🤖 [Machine Move] 완벽한 엔진 정밀도 (Engine Precision)',
        body: `Stockfish NNUE의 전형적인 'Machine Move'입니다! 인간의 직관을 뛰어넘는 비인간적 정밀 수읽기로 미세한 전술적 우위를 구축합니다.`,
      },
      {
        title: '🤖 [Tactical Refutation] 포지셔널 압박과 공간 장악',
        body: `AI가 ${notation}를 통해 상대의 미세한 약점 칸(${to})을 정밀하게 타격했습니다. 빈틈을 허용하지 않는 강력한 압박입니다.`,
      },
      {
        title: '🤖 [Deep Evaluation] 엔진의 깊은 심도 수읽기',
        body: `${from} ➔ ${to} 기동은 20수 이상을 계산한 Stockfish 특유의 포지션 강화 수입니다. 매우 정교한 행마입니다.`,
      },
    ];
    const picked = machineComments[moveNumber % machineComments.length];
    return {
      id: `comm-machine-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: picked.title,
      body: picked.body,
      tag: 'MACHINE',
      timestamp: timeStr,
    };
  }

  // 8. 기물 포획 및 전술 교환 (Captures)
  if (capturedPiece || notation.includes('x')) {
    const isQueenTrade = notation.includes('Qx') || notation.includes('xq') || notation.includes('xQ');
    const capTitle = isQueenTrade ? '⚔️ 퀸 교환 (Queen Trade) 단행' : '⚔️ 기물 획득 및 전술적 교환';
    const capBody = isQueenTrade
      ? `${playerName} 선수가 퀸을 교환하며 복잡한 미들게임을 단순화하고 테크니컬 엔드게임으로 국면을 전환합니다.`
      : `${playerName} 선수가 ${from} ➔ ${to}로 상대 기물을 포획하며(${notation}) 텐션을 정리하고 포지셔널 우위를 확보했습니다.`;
    return {
      id: `comm-cap-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: capTitle,
      body: capBody,
      tag: 'CAPTURE',
      timestamp: timeStr,
    };
  }

  // 9. 중앙 요충지 장악 (Center Control)
  if (to === 'e4' || to === 'd4' || to === 'e5' || to === 'd5') {
    return {
      id: `comm-center-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🎯 핵심 요충지 장악 (Center Outpost)',
      body: `${playerName} 선수가 보드의 심장부인 ${to} 칸에 기물을 단단히 배치했습니다. 중앙 지배력이 대폭 강화되며 공격의 교두보가 마련됩니다.`,
      tag: 'BRILLIANCY',
      timestamp: timeStr,
    };
  }

  // 10. 마이너 피스 전개 및 포지셔널 기동
  if (
    from.startsWith('b1') ||
    from.startsWith('g1') ||
    from.startsWith('b8') ||
    from.startsWith('g8') ||
    from.startsWith('c1') ||
    from.startsWith('f1') ||
    from.startsWith('c8') ||
    from.startsWith('f8')
  ) {
    return {
      id: `comm-dev-${Date.now()}-${Math.random()}`,
      moveNumber,
      color,
      playerName,
      notation,
      fromSquare: from as Square,
      toSquare: to as Square,
      title: '🐎 기물 기동 및 주도권 전개 (Initiative)',
      body: `${playerName} 선수가 마이너 피스(${notation})를 이상적인 전진 거점으로 기동하며 전개 속도(Tempo)의 우위를 점하고 있습니다.`,
      tag: 'POSITIONAL',
      timestamp: timeStr,
    };
  }

  // 11. 일반 포지셔널 조율
  const positionalNotes = [
    `${playerName} 선수가 ${from} ➔ ${to}로 차분히 폰 구조의 밸런스를 유지하며 장기전을 준비하고 있습니다.`,
    `${playerName} 선수의 정교한 포지셔널 조율(${notation}). 상대의 다음 의도를 예리하게 견제합니다.`,
    `${playerName} 선수의 침착한 진영 정비. 보드의 긴장감이 최고조에 달하고 있습니다.`,
  ];
  const posBody = positionalNotes[moveNumber % positionalNotes.length];

  return {
    id: `comm-pos-${Date.now()}-${Math.random()}`,
    moveNumber,
    color,
    playerName,
    notation,
    fromSquare: from as Square,
    toSquare: to as Square,
    title: `♟️ ${color === 'WHITE' ? '백' : '흑'} ${notation} 포지션 조율`,
    body: posBody,
    tag: 'TACTICAL',
    timestamp: timeStr,
  };
}
