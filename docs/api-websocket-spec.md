# jchess REST & WebSocket 프로토콜 명세서

## 1. 개요

본 문서는 `jchess` 프로젝트의 프론트엔드와 백엔드 간 통신 규격(REST API 및 WebSocket 프로토콜), 공통 에러 모델, 그리고 UX 화면 상태와 서버 이벤트 간의 매핑 규칙을 정의한다.

- **기본 원칙**: 서버 권위형(Server-authoritative) 모델
- **메시지 형식**: JSON
- **인코딩**: UTF-8
- **시간 형식**: ISO-8601 UTC (`YYYY-MM-DDTHH:mm:ssZ`)

---

## 2. 공통 에러 모델 (Error Model)

### 2.1 에러 응답 규격

REST와 WebSocket 공통으로 사용하는 표준 에러 규격이다.

```json
{
  "code": "NOT_YOUR_TURN",
  "message": "현재 플레이어의 차례가 아닙니다.",
  "requestId": "req-12345",
  "gameId": "game-789",
  "gameVersion": 12,
  "retryable": false,
  "timestamp": "2026-09-08T12:00:00Z"
}
```

### 2.2 표준 에러 코드 카탈로그

| 에러 코드 | HTTP 상태 | 설명 | 재시도 가능 여부 |
|---|---|---|---|
| `INVALID_REQUEST_PAYLOAD` | 400 | 요청 파라미터 또는 JSON 구조가 올바르지 않음 | N |
| `GAME_NOT_FOUND` | 404 | 요청한 게임 ID를 찾을 수 없음 | N |
| `UNAUTHORIZED_PLAYER` | 403 | 해당 대국의 참가자가 아니거나 권한이 없음 | N |
| `NOT_YOUR_TURN` | 400 | 현재 본인의 차례가 아님 | N |
| `ILLEGAL_MOVE` | 400 | 체스 규칙상 허용되지 않는 불법 수 | N |
| `IN_CHECK_MUST_DEFEND` | 400 | 킹이 체크 상태이며 체크를 해소하지 못하는 수임 | N |
| `PROMOTION_PIECE_REQUIRED` | 400 | 폰이 8열/1열에 도달했으나 프로모션 기물이 지정되지 않음 | N |
| `INVALID_PROMOTION_PIECE` | 400 | 유효하지 않은 프로모션 기물(예: 폰이나 킹으로 승급 시도) | N |
| `GAME_VERSION_CONFLICT` | 409 | 클라이언트의 게임 버전이 서버 최신 버전과 불일치함 (Sync 필요) | Y (Sync 후) |
| `GAME_ALREADY_FINISHED` | 400 | 이미 종료된 대국에 대해 수 또는 조작을 시도함 | N |
| `DUPLICATE_REQUEST` | 400 | 동일한 requestId의 요청이 이미 처리 중이거나 완료됨 | N |
| `OPPONENT_DISCONNECTED` | 200/WS | 상대방 연결이 일시적으로 끊김 | Y |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 처리 오류 | Y |

---

## 3. REST API 명세

### 3.1 게임 생성 (Create Game)
- **POST** `/api/v1/games`
- **Request Body**:
```json
{
  "timeControl": {
    "baseMinutes": 10,
    "incrementSeconds": 0
  },
  "preferredColor": "WHITE" // "WHITE", "BLACK", "RANDOM"
}
```
- **Response** (201 Created):
```json
{
  "gameId": "game-a1b2c3d4",
  "status": "WAITING_FOR_OPPONENT",
  "whitePlayer": { "playerId": "user-white", "name": "Player 1" },
  "blackPlayer": null,
  "gameVersion": 0,
  "createdAt": "2026-09-08T12:00:00Z"
}
```

### 3.2 게임 참가 (Join Game)
- **POST** `/api/v1/games/{gameId}/join`
- **Response** (200 OK):
```json
{
  "gameId": "game-a1b2c3d4",
  "status": "ACTIVE",
### 3.3 게임 상태 조회 (Get Game State Snapshot)
- **GET** `/api/v1/games/{gameId}`
- **Response** (200 OK): 전체 GameState Snapshot 반환

---

## 4. WebSocket 실시간 프로토콜 명세

- **WebSocket 엔드포인트**: `/ws/games/{gameId}`
- **인증**: WebSocket Handshake 시 쿼리 파라미터 또는 헤더 토큰 (`?token=...`)

### 4.1 클라이언트 -> 서버 명령 (Command Envelope)

```json
{
  "type": "PLAY_MOVE",
  "requestId": "req-uuid-1234",
  "gameId": "game-a1b2c3d4",
  "expectedGameVersion": 12,
  "payload": {
    "from": "e2",
    "to": "e4",
    "promotion": null
  }
}
```

#### 지원 Command 타입 목록:
1. `PLAY_MOVE`: 기물 이동 요청 (`payload`: `{ "from": "e2", "to": "e4", "promotion": "QUEEN" | null }`)
2. `RESIGN`: 기권 요청 (`payload`: `{}`)
3. `OFFER_DRAW`: 무승부 제안 (`payload`: `{}`)
4. `RESPOND_DRAW`: 무승부 제안 수락/거절 (`payload`: `{ "accept": true | false }`)
5. `SYNC_STATE`: 최신 전체 상태 동기화 요청 (`payload`: `{}`)

---

### 4.2 서버 -> 클라이언트 이벤트 (Event Envelope)

```json
{
  "eventId": "evt-uuid-5678",
  "gameId": "game-a1b2c3d4",
  "gameVersion": 13,
  "eventType": "GAME_STATE_UPDATED",
  "occurredAt": "2026-09-08T12:02:15Z",
  "payload": { ... }
}
```

#### 주요 Event 타입:
- **`GAME_STATE_SNAPSHOT`**: 초기 접속 또는 재접속 시 전체 보드 포지션(FEN), 시계, 플레이어 정보 전송
- **`GAME_STATE_UPDATED`**: 합법 수가 확정되어 차례, 시계, FEN, 마지막 수가 갱신되었을 때 방송
- **`MOVE_REJECTED`**: 불법 수 또는 버전 충돌 시 요청자에게 단독 발송
- **`GAME_ENDED`**: 체크메이트, 기권, 무승부 등으로 게임 종료 시 방송
- **`PLAYER_CONNECTION_CHANGED`**: 플레이어 온라인/오프라인 상태 변경 시 방송

---

## 5. UX 화면 상태와 서버 이벤트 매핑표

| UX 화면 상태 | 활성화 조건 | 허용 사용자 액션 |
|---|---|---|
| **`LOBBY` / `MATCHING`** | 게임 시작 전 대기 | 대국 취소, 초대 링크 복사 |
| **`YOUR_TURN`** | `gameStatus === 'ACTIVE' \|\| 'CHECK'` 이고 `turn === myColor` | 기물 선택, 이동 후보지 클릭, 기권, 무승부 제안 |
| **`OPPONENT_TURN`** | `gameStatus === 'ACTIVE' \|\| 'CHECK'` 이고 `turn !== myColor` | 보드 입력 비활성화, 기권 가능 |
| **`PROMOTION_MODAL`** | 폰이 8열/1열에 도달하여 승급 대기 (`pendingPromotion !== null`) | Queen, Rook, Bishop, Knight 선택 |
| **`CHECK_ALERT`** | `isCheck === true` 및 내 차례 | 킹 칸 붉은색 강조, 체크 해소 수만 가능 |
| **`GAME_OVER`** | `gameStatus`가 `CHECKMATE`, `STALEMATE`, `DRAW`, `RESIGNED`, `TIMEOUT` | 대국 결과 모달 노출, 복기 및 새 대국 |
| **`RECONNECTING`** | WebSocket 연결 끊김 감지 | 재접속 스피너 노출, 보드 동결 |
| **`SYNCING`** | 재접속 성공 후 최신 `SNAPSHOT` 대기 | 상태 수신 후 최신 포지션으로 UI 복원 |

  "whitePlayer": { "playerId": "user-white", "name": "Player 1" },
  "blackPlayer": { "playerId": "user-black", "name": "Player 2" },
  "gameVersion": 1,
  "startedAt": "2026-09-08T12:01:00Z"
}
```
