# Java 온라인 체스 게임 규칙

> 이 문서는 온라인 체스 게임의 게임 로직을 구현하기 위한 요약 문서다.
> 공식 규칙 전체를 대체하지 않으며, 기본 규칙은 FIDE Laws of Chess를 기준으로 한다.

## 1. 기준 문서

- [FIDE Laws of Chess, effective 1 January 2023](https://handbook.fide.com/chapter/E012023)
- 적용 범위: 일반적인 표준 체스의 기본 플레이 규칙
- 온라인 게임에서는 대면 경기의 규칙 중 `touch-move`, 심판 절차, 실물 시계 조작 등은 제외하고 서버가 상태를 판정한다.

## 2. 게임 기본

- 체스판은 8 x 8, 총 64칸이다.
- 두 명의 플레이어가 White와 Black으로 플레이한다.
- White가 먼저 둔다.
- 이후 White와 Black이 한 번씩 번갈아 둔다.
- 목표는 상대 King을 공격하면서 상대가 합법적인 대응 수를 갖지 못하게 하는 것, 즉 Checkmate다.
- 자신의 King을 공격받는 상태로 남기는 수는 둘 수 없다.
- 상대 King을 실제로 잡는 행위는 하지 않는다. Checkmate가 되는 순간 게임이 끝난다.

## 3. 초기 배치

White 기준으로 첫 번째 줄의 배치는 다음과 같다.

```text
a1 R, b1 N, c1 B, d1 Q, e1 K, f1 B, g1 N, h1 R
a2~h2: Pawn
```

Black은 같은 파일의 8번째와 7번째 줄에 배치한다.

```text
a8 R, b8 N, c8 B, d8 Q, e8 K, f8 B, g8 N, h8 R
a7~h7: Pawn
```

- White의 오른쪽 아래 코너인 h1은 밝은 칸이어야 한다.
- 파일(file)은 `a`~`h`, 랭크(rank)는 `1`~`8`이다.
- 좌표는 파일과 랭크를 합쳐 표현한다. 예: `e4`, `a1`.

## 4. 기물 이동 규칙

같은 색 기물이 있는 칸으로 이동할 수 없다. 상대 기물이 있는 칸으로 이동하면 그 기물을 잡는다.

### King

- 모든 방향으로 한 칸 이동한다.
- 공격받는 칸으로 이동할 수 없다.
- 두 King은 서로 인접할 수 없다.
- Castling을 할 수 있다.

### Queen

- 같은 파일, 같은 랭크 또는 대각선 방향으로 원하는 칸 수만큼 이동한다.
- 이동 경로에 다른 기물이 있으면 안 된다.

### Rook

- 같은 파일 또는 같은 랭크로 원하는 칸 수만큼 이동한다.
- 이동 경로에 다른 기물이 있으면 안 된다.

### Bishop

- 대각선 방향으로 원하는 칸 수만큼 이동한다.
- 이동 경로에 다른 기물이 있으면 안 된다.
- 시작 칸의 색과 다른 색의 칸으로는 이동할 수 없다.

### Knight

- `L`자 형태로 이동한다: 한 방향으로 두 칸, 직각 방향으로 한 칸.
- 다른 기물을 뛰어넘을 수 있다.

### Pawn

White Pawn은 랭크가 증가하는 방향, Black Pawn은 랭크가 감소하는 방향으로 전진한다.

- 기본 전진: 비어 있는 앞 칸으로 한 칸 이동한다.
- 첫 이동: 두 칸 전진할 수 있으나 두 칸 모두 비어 있어야 한다.
- 잡기: 앞 대각선 한 칸에 있는 상대 기물을 잡는다.
- Pawn은 정면에 있는 기물을 잡을 수 없다.
- 마지막 랭크에 도착하면 즉시 Promotion한다.

## 5. 특수 수

### Castling

King과 Rook을 한 번의 수로 함께 이동하는 특수 수다.

- King과 해당 Rook이 이전에 움직인 적이 없어야 한다.
- King과 Rook 사이에 기물이 없어야 한다.
- 현재 King이 Check 상태가 아니어야 한다.
- King이 지나가는 칸과 도착하는 칸이 상대 기물의 공격을 받지 않아야 한다.
- King-side Castling: `e1 -> g1`, `h1 -> f1` 또는 Black의 동일한 형태.
- Queen-side Castling: `e1 -> c1`, `a1 -> d1` 또는 Black의 동일한 형태.
- Castling 권리는 King 또는 해당 Rook이 이동하면 영구적으로 사라진다. 잡혔다가 새로 생긴 Rook에는 권리가 없다.

표기:

- King-side: `O-O`
- Queen-side: `O-O-O`

### En passant

- 상대 Pawn이 시작 위치에서 한 번에 두 칸 전진하여 내 Pawn 옆에 도착한 직후에만 가능하다.
- 내 Pawn은 상대 Pawn이 한 칸만 전진한 것처럼 대각선으로 이동한다.
- 실제로는 이동 목적지의 뒤쪽에 있는 상대 Pawn을 제거한다.
- 기회는 바로 다음 한 수에만 유효하며, 지나가면 사라진다.

### Promotion

- Pawn이 상대편 마지막 랭크에 도착하면 같은 색의 Queen, Rook, Bishop, Knight 중 하나로 교체한다.
- 이미 잡힌 기물의 수량과 관계없이 선택할 수 있다.
- Promotion은 Pawn이 마지막 랭크에 도착하는 같은 수 안에서 완료되어야 한다.
- 일반적으로 Queen으로 자동 Promotion하는 방식도 가능하지만, 온라인 게임에서는 사용자가 선택할 수 있어야 한다.

## 6. Check와 합법적인 수

### Check

King이 상대 기물의 공격을 받고 있는 상태다.

### 합법적인 수

수는 다음 조건을 모두 만족해야 한다.

1. 해당 기물의 이동 규칙에 맞는다.
2. 같은 색 기물이 있는 칸으로 이동하지 않는다.
3. 필요한 경우 이동 경로가 비어 있다.
4. 자신의 King을 Check 상태로 만들거나, 이미 Check인 자신의 King을 그대로 두지 않는다.
5. Castling이라면 Castling의 모든 조건을 만족한다.

구현 시에는 후보 수를 만든 뒤 임시로 적용하고, 자신의 King이 공격받는지 검사한 다음 원상 복구하거나 확정하는 방식이 안전하다.

```text
legalMoves = pseudoLegalMoves - movesLeavingOwnKingInCheck
```

- `pseudoLegalMoves`: 기물 이동 방식만 기준으로 만든 후보 수
- `legalMoves`: 실제로 둘 수 있는 수

## 7. 게임 종료

### White 승리

- Black King이 Check 상태이고 Black에게 합법적인 수가 없으면 Checkmate다.
- 결과는 `1-0`이다.

### Black 승리

- White King이 Check 상태이고 White에게 합법적인 수가 없으면 Checkmate다.
- 결과는 `0-1`이다.

### 무승부

- Stalemate: 차례인 플레이어가 Check는 아니지만 합법적인 수가 없다.
- Dead position: 어느 쪽도 어떤 합법적인 수의 연속으로 상대를 Checkmate할 수 없다.
- 양쪽이 합의한 무승부.
- 동일한 포지션이 5번 발생하면 자동 무승부다.
- 각 플레이어가 Pawn 이동이나 Capture 없이 75수를 완료하면 자동 무승부다. 단, 마지막 수가 Checkmate면 Checkmate가 우선한다.
- 플레이어는 동일 포지션 3회 반복 또는 Pawn 이동과 Capture 없이 각 플레이어가 50수를 완료한 경우 무승부를 청구할 수 있다.

초기 온라인 버전에서는 다음 순서로 구현하는 것을 권장한다.

1. Checkmate
2. Stalemate
3. 합의 무승부
4. 50-move claim 및 3회 반복 청구
5. 75-move 및 5회 반복 자동 무승부
6. Dead position 판정

## 8. 기보와 수 표현

기본 Algebraic Notation은 다음과 같다.

| 기물 | 기호 |
| --- | --- |
| King | `K` |
| Queen | `Q` |
| Rook | `R` |
| Bishop | `B` |
| Knight | `N` |
| Pawn | 기호 없음 |

예시:

- `e4`: Pawn이 e4로 이동
- `Nf3`: Knight가 f3으로 이동
- `Bxe5`: Bishop이 e5의 기물을 Capture
- `exd5`: e파일의 Pawn이 d5에서 Capture
- `O-O`: King-side Castling
- `O-O-O`: Queen-side Castling
- `e8=Q`: e8에서 Queen으로 Promotion
- `+`: Check
- `#`: Checkmate
- `e.p.`: En passant

온라인 게임 내부 통신에서는 모호성을 줄이기 위해 `from`, `to`, `promotion`을 분리한 구조를 권장한다.

```json
{
  "from": "e2",
  "to": "e4",
  "promotion": null
}
```

## 9. Java 구현에 필요한 게임 상태

최소한 다음 상태를 `GameState`에 보관한다.

- 현재 보드의 64칸과 각 기물
- 현재 차례: `WHITE` 또는 `BLACK`
- White King의 Castling 권리
- Black King의 Castling 권리
- White Queen-side / King-side Castling 권리
- Black Queen-side / King-side Castling 권리
- 직전 수로 En passant가 가능한 목적지 칸 또는 `null`
- 반수 카운트: 마지막 Pawn 이동 또는 Capture 이후 지난 반수
- 전체 수 기록 또는 포지션 식별자별 반복 횟수
- 각 플레이어의 남은 시간과 게임 시간 설정
- 게임 상태: `ACTIVE`, `CHECKMATE`, `STALEMATE`, `DRAW`, `RESIGNED`, `TIMEOUT`
- 결과: `WHITE_WIN`, `BLACK_WIN`, `DRAW`, `UNDECIDED`

## 10. 수 처리 순서

서버에서 수를 처리할 때는 다음 순서를 지킨다.

1. 요청한 플레이어가 현재 차례인지 확인한다.
2. `from`, `to`, `promotion` 형식과 보드 범위를 검증한다.
3. 출발 칸에 현재 플레이어의 기물이 있는지 확인한다.
4. 일반 이동, Capture, Castling, En passant, Promotion 후보를 생성한다.
5. 후보 수가 기물 이동 규칙에 맞는지 확인한다.
6. 후보 수를 임시 적용한다.
7. 자신의 King이 Check인지 검사한다.
8. 합법적인 수일 때만 보드와 이력에 확정한다.
9. Castling 권리, En passant 칸, 반수 카운트, 수 번호를 갱신한다.
10. 상대의 Check, Checkmate, Stalemate, Draw 여부를 판정한다.
11. 게임이 끝나지 않았다면 차례를 변경하고 상대 시계를 시작한다.
12. 확정된 수와 새 게임 상태를 클라이언트에 전송한다.

## 11. 온라인 게임에서 추가로 정할 정책

FIDE 기본 규칙 외에 서비스 정책으로 명시해야 하는 항목이다.

- 제한 시간 형식: Bullet, Blitz, Rapid, Standard 등
- Increment 또는 Delay 적용 여부
- 시간 초과 처리와 상대가 Checkmate할 수 없는 경우의 무승부 처리
- 접속 끊김, 재접속, 일시정지 허용 여부
- 기권, 무승부 제안, 무승부 수락/거절
- 서버 권위형 판정: 클라이언트가 보낸 결과를 믿지 않고 서버에서 항상 합법성을 재검사
- 통신 순서 보장과 중복 요청 방지: `gameId`, `moveNumber`, `requestId` 사용
- 게임 재접속을 위한 전체 `GameState` 복구

## 12. 권장 Java 도메인 모델

```text
enum Color { WHITE, BLACK }
enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum GameStatus { ACTIVE, CHECK, CHECKMATE, STALEMATE, DRAW, RESIGNED, TIMEOUT }

record Position(int file, int rank) {}
record Move(Position from, Position to, PieceType promotion) {}
record Piece(Color color, PieceType type) {}
```

권장 책임 분리:

- `Board`: 64칸과 기물 배치 관리
- `MoveGenerator`: 후보 수와 합법적인 수 생성
- `MoveValidator`: 이동 규칙, Check, Castling, En passant 검증
- `GameState`: 차례, 권리, 카운터, 결과 등 상태 보관
- `GameService`: 서버 요청 검증과 수 확정
- `Clock`: 제한 시간과 Increment 처리
- `Notation`: UCI 스타일 입력 및 SAN 출력

## 13. 테스트 우선순위

1. 초기 배치와 White 선공
2. 각 기물의 정상 이동과 경로 차단
3. Pawn의 첫 2칸 이동과 Capture
4. 자신의 King을 노출시키는 수 거부
5. Checkmate와 Stalemate
6. King-side / Queen-side Castling의 모든 제한 조건
7. En passant의 한 수 유효 기간
8. 네 종류의 Promotion
9. Capture와 Pawn 이동에 따른 반수 카운트 갱신
10. 반복 포지션 및 50/75-move 규칙
11. 동시 요청, 잘못된 차례, 중복 수 요청
12. 시간 초과와 재접속 후 상태 복구

## 14. 규칙 엔진 테스트 fixture

규칙 엔진 테스트는 초기 배치만으로 충분하지 않다. 각 fixture는 FEN 또는 동등한 보드 표현과 기대 가능한 합법 수/게임 상태를 함께 저장한다.

- Castling-through-check: King이 지나가는 칸이 공격받으면 Castling을 거부한다.
- En passant expiry: 상대 Pawn의 두 칸 이동 직후가 아닌 다음 수에는 En passant를 거부한다.
- Promotion with check: Promotion 결과 기물에 따라 Check 여부와 `pendingPromotion` 처리를 검증한다.
- Stalemate: 차례인 King이 Check는 아니고 합법 수가 없으면 무승부다.
- Checkmate: 차례인 King이 Check이고 합법 수가 없으면 승리 상태다.
- Insufficient material: 어느 쪽도 Checkmate할 수 없는 최소 기물 상황을 검증한다.
- Repetition identity: 차례, 기물 배치, Castling 권리, En passant 가능성이 다른 포지션은 동일 포지션으로 세지 않는다.

추가 검증:

- 주요 초기 포지션에 대해 perft 또는 신뢰 가능한 reference-position 결과를 비교한다.
- 합법적으로 승인된 수는 이동한 쪽의 King을 Check 상태로 남기지 않는다는 불변식을 검증한다.
- 잘못된 수가 거부되면 보드, 차례, Castling 권리, En passant 칸, 반수 카운트가 변경되지 않는지 검증한다.
- 시간 제한 테스트는 실제 시간을 기다리지 않고 fake clock으로 timeout, Increment, 재접속과 수/timeout 경합을 재현한다.

## 참고

이 문서의 규칙 요약은 FIDE Laws of Chess의 다음 항목을 바탕으로 작성했다.

- Article 1: 게임의 목적
- Article 2: 초기 보드와 기물 배치
- Article 3: 기물 이동, 특수 수, Check
- Article 4: 수를 두는 행위 중 구현에 필요한 내용
- Article 5: 게임 종료
- Article 9: 무승부
- Appendix C: Algebraic Notation
