/**
 * jchess 종합 알파/베타 라이브 서버 검증 스크립트 (Node.js Native WebSocket & Fetch)
 */
const BASE_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080';

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function runTests() {
  console.log('========================================================');
  console.log('🚀 jchess 종합 알파/베타 사용자 라이브 서버 검증 시작');
  console.log('========================================================\n');

  // 1. Static Web Resource Servicing Test
  console.log('--- [테스트 1] WebFlux Static Frontend 서빙 검증 ---');
  const staticResp = await fetch(`${BASE_URL}/index.html`);
  const staticText = await staticResp.text();
  if (staticResp.status !== 200 || !staticText.includes('<!doctype html>')) {
    throw new Error('Static Frontend 서빙 실패: status=' + staticResp.status);
  }
  console.log('  ✅ Static Frontend HTML 서빙 정상 (Status: ' + staticResp.status + ', Size: ' + staticText.length + ' bytes)');

  // 2. PVC (AI 대국) 플레이어 착수 & AI 비동기 자동 응수 검증
  console.log('\n--- [테스트 2] PVC (AI 대국) 플레이어 착수 & AI 비동기 자동 응수 검증 ---');
  const pvcResp = await fetch(`${BASE_URL}/api/v1/games`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'pvc-p1', 'X-Player-Name': 'Human' },
    body: JSON.stringify({ gameMode: 'PVC', timeControl: { baseMinutes: 10, incrementSeconds: 0 }, preferredColor: 'WHITE' })
  });
  const pvcGame = await pvcResp.json();
  console.log('  1. PVC 대국 생성: id =', pvcGame.gameId, 'status =', pvcGame.status, 'black =', pvcGame.blackPlayer.name);
  if (pvcGame.status !== 'ACTIVE') throw new Error('PVC 대국은 생성 즉시 ACTIVE 상태여야 합니다.');

  // 플레이어 착수 e2 -> e4
  const playerMoveResp = await fetch(`${BASE_URL}/api/v1/games/${pvcGame.gameId}/moves`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'pvc-p1' },
    body: JSON.stringify({ from: 'e2', to: 'e4' })
  });
  const moveData = await playerMoveResp.json();
  console.log('  2. 플레이어 착수 확정: turn =', moveData.turn, 'fen =', moveData.fen);

  // AI 응수 대기
  let aiMoved = false;
  for (let i = 0; i < 15; i++) {
    await delay(300);
    const checkResp = await fetch(`${BASE_URL}/api/v1/games/${pvcGame.gameId}`);
    const g = await checkResp.json();
    if (g.turn === 'WHITE' && g.gameVersion >= 2 && g.lastMove && g.lastMove.from !== 'e2') {
      console.log('  ✅ AI(흑) 자동 응수 완료: gameVersion =', g.gameVersion, 'AI 착수 =', g.lastMove.notation);
      aiMoved = true;
      break;
    }
  }
  if (!aiMoved) throw new Error('AI 자동 응수 실패 (시간 초과)');

  // 3. WebSocket 실시간 대국 양방향 통신 검증
  console.log('\n--- [테스트 3] WebSocket 실시간 대국 양방향 통신 검증 ---');
  const pvpWsResp = await fetch(`${BASE_URL}/api/v1/games`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'ws-w', 'X-Player-Name': 'White' },
    body: JSON.stringify({ gameMode: 'PVP', timeControl: { baseMinutes: 5, incrementSeconds: 2 }, preferredColor: 'WHITE' })
  });
  const wsPvp = await pvpWsResp.json();

  await fetch(`${BASE_URL}/api/v1/games/${wsPvp.gameId}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'ws-b', 'X-Player-Name': 'Black' }
  });

  const wsWhite = new WebSocket(`${WS_URL}/ws/games/${wsPvp.gameId}?playerId=ws-w`);
  const wsBlack = new WebSocket(`${WS_URL}/ws/games/${wsPvp.gameId}?playerId=ws-b`);
  const whiteEvents = [], blackEvents = [];

  wsWhite.addEventListener('message', e => { try { whiteEvents.push(JSON.parse(e.data)); } catch(err){} });
  wsBlack.addEventListener('message', e => { try { blackEvents.push(JSON.parse(e.data)); } catch(err){} });
  await delay(600);

  // WebSocket 수 착수 전송
  wsWhite.send(JSON.stringify({
    type: 'PLAY_MOVE',
    gameId: wsPvp.gameId,
    playerId: 'ws-w',
    payload: { from: 'd2', to: 'd4' }
  }));
  await delay(600);

  const moveRcv = blackEvents.some(e => e.eventType === 'GAME_STATE_UPDATED' && e.payload && e.payload.lastMove && e.payload.lastMove.from === 'd2');
  console.log('  1. WebSocket 착수 브로드캐스트 (GAME_STATE_UPDATED):', moveRcv ? '✅ 정상 수신' : '❌ 수신 실패');
  if (!moveRcv) throw new Error('WebSocket 착수 이벤트 수신 실패');

  // WebSocket 기권 전송
  wsBlack.send(JSON.stringify({
    type: 'RESIGN',
    gameId: wsPvp.gameId,
    playerId: 'ws-b',
    payload: {}
  }));
  await delay(600);

  const overRcv = whiteEvents.some(e => e.eventType === 'GAME_ENDED');
  console.log('  2. WebSocket 기권 및 대국 종료 브로드캐스트 (GAME_ENDED):', overRcv ? '✅ 정상 수신' : '❌ 수신 실패');
  if (!overRcv) throw new Error('WebSocket 기권 이벤트 수신 실패');

  wsWhite.close();
  wsBlack.close();

  // 4. 보안 및 불법 수 차단 검증
  console.log('\n--- [테스트 4] 서버 권위형 불법 수 & 보안 검증 ---');
  const finishedMoveResp = await fetch(`${BASE_URL}/api/v1/games/${wsPvp.gameId}/moves`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'ws-w' },
    body: JSON.stringify({ from: 'e2', to: 'e4' })
  });
  console.log('  - 종료된 대국 착수 시도 차단 응답 코드:', finishedMoveResp.status, '(400 Bad Request 확인)');
  if (finishedMoveResp.status !== 400) throw new Error('종료 대국 차단 실패');

  console.log('\n========================================================');
  console.log('🎉 모든 알파/베타 사용자 시나리오 100% 정상 통과 (ALL GREEN)');
  console.log('========================================================\n');
}

runTests().catch(err => {
  console.error('❌ 검증 실패:', err);
  process.exit(1);
});

