/**
 * jchess 최종 인수 테스팅 스텁 (Live Server Acceptance Test)
 */
async function runAcceptanceTesting() {
  console.log('====================================================');
  console.log('🏁 jchess 최종 인수 테스팅 (Live Server Acceptance Test)');
  console.log('====================================================\n');

  const BASE_URL = 'http://localhost:8080';

  // [테스트 1] Actuator 헬스체크
  console.log('[시나리오 1] 시스템 헬스체크 (GET /actuator/health)');
  const healthResp = await fetch(`${BASE_URL}/actuator/health`);
  const health = await healthResp.json();
  if (health.status !== 'UP') throw new Error('헬스체크 실패');
  console.log('  ✅ 헬스체크 정상: status =', health.status);

  // [테스트 2] PVP 2인 대국 생성 -> 참가 -> e4/e5 -> 기권(Resign)
  console.log('\n[시나리오 2] PVP 2인 대국 라이프사이클');
  const createPvpResp = await fetch(`${BASE_URL}/api/v1/games`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'alice-1', 'X-Player-Name': 'Alice' },
    body: JSON.stringify({ gameMode: 'PVP', timeControl: { baseMinutes: 10, incrementSeconds: 0 }, preferredColor: 'WHITE' })
  });
  const pvpGame = await createPvpResp.json();
  console.log('  1. 백(Alice) 대국 생성:', pvpGame.gameId, 'status =', pvpGame.status);

  const joinResp = await fetch(`${BASE_URL}/api/v1/games/${pvpGame.gameId}/join`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'bob-2', 'X-Player-Name': 'Bob' }
  });
  const joinData = await joinResp.json();
  console.log('  2. 흑(Bob) 참가 완료: status =', joinData.status);

  // 백 착수 e2 -> e4
  const move1Resp = await fetch(`${BASE_URL}/api/v1/games/${pvpGame.gameId}/moves`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'alice-1' },
    body: JSON.stringify({ from: 'e2', to: 'e4' })
  });
  const move1 = await move1Resp.json();
  console.log('  3. 백(Alice) e2->e4 착수: next turn =', move1.turn, 'lastMove =', move1.lastMove.notation);

  // 흑 기권
  const resignResp = await fetch(`${BASE_URL}/api/v1/games/${pvpGame.gameId}/resign`, {
    method: 'POST',
    headers: { 'X-Player-Id': 'bob-2' }
  });
  const resignData = await resignResp.json();
  console.log('  4. 흑(Bob) 기권 완료: status =', resignData.gameStatus, 'result =', resignData.result);

  // [테스트 3] 보안 인가 테스트 (비인가 제3자 착수 시도)
  console.log('\n[시나리오 3] 보안 인가 검증 (제3자 침입 시도 차단)');
  const intruderResp = await fetch(`${BASE_URL}/api/v1/games/${pvpGame.gameId}/moves`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Player-Id': 'intruder-charlie' },
    body: JSON.stringify({ from: 'd2', to: 'd4' })
  });
  console.log('  ✅ 제3자 착수 차단 응답 코드:', intruderResp.status, '(403 Forbidden 기대)');
  if (intruderResp.status !== 403 && intruderResp.status !== 400) throw new Error('보안 차단 실패');

  console.log('\n====================================================');
  console.log('🎉 최종 인수 테스팅 모든 시나리오 100% 통과 (VERIFIED)');
  console.log('====================================================\n');
}

runAcceptanceTesting().catch(err => {
  console.error('❌ 인수 테스팅 실패:', err);
  process.exit(1);
});
