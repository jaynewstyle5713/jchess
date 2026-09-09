# ♟️ jchess 프론트엔드 목업 프리뷰 (Mockup Preview)

본 문서는 `jchess` 프로젝트의 프론트엔드 화면 디자인과 대국 사용자 경험을 Chrome 브라우저에서 즉시 확인하고 시연하기 위한 가이드입니다.

---

## 🚀 1. 가장 빠른 확인 방법 (별도 설치/서버 구동 불필요)

### 🔹 방법 A: Windows 탐색기 또는 터미널에서 즉시 실행 (가장 추천)
PowerShell 또는 명령 프롬프트에서 아래 명령을 실행하면 기본 브라우저(Chrome)에서 즉시 열립니다:

```powershell
start c:\work\jchess\preview\index.html
```

또는 Chrome 브라우저 주소창에 다음 파일 URL을 입력하세요:
```text
file:///c:/work/jchess/preview/index.html
```

---

## 💻 2. React 19 실제 개발 서버 구동 방법 (인터랙티브 대국)

실제 React 19 컴포넌트 환경에서 소스코드를 확인하고 클릭 상호작용을 테스트하려면:

```bash
cd c:\work\jchess\frontend
npm run dev
```

브라우저에서 접속:
👉 **`http://localhost:5173`**

---

## 🎨 3. 프리뷰 화면 구성 및 시연 관찰 포인트

상단 탭 버튼을 클릭하여 3가지 핵심 화면을 1초 만에 전환하며 확인할 수 있습니다.

### [탭 1] 대국 로비 화면 (Lobby)
- **AI 대전 (Stockfish 2000+)** 및 **2인 대전 (PVP)** 모드 전환 UI
- 시간 프리셋 (Bullet 1+0, Blitz 3+2, Rapid 10+0, Classical 15+10) 선택
- 선호 색상 (백 / 무작위 / 흑) 및 닉네임 입력

### [탭 2] 실시간 대국 진행 화면 (Game View)
- **8x8 체스보드**: 유니코드 기물 심볼, 마지막 착수(e2-e4) 금색 하이라이트, 보드 좌표(a-h, 1-8)
- **대국 패널**:
  - 상단: `Stockfish AI` (ELO 2000 배지, 실시간 카운트다운 시계)
  - 중앙: 현재 차례 배너 및 `AI가 최선의 수를 연산하고 있습니다...` 회전 스피너
  - 하단: 플레이어 카드 및 시계
  - 하단 조작 버튼: `동기화`, `무승부 제안`, `기권`

### [탭 3] 폰 프로모션 모달 (Promotion Modal)
- 폰이 8열(상대 끝 칸)에 도달했을 때 뜨는 반투명 백드롭 다이얼로그
- 퀸(Queen), 룩(Rook), 비숍(Bishop), 나이트(Knight) 선택 카드
