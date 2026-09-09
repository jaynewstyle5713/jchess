# jchess - 온라인 실시간 체스 플랫폼 (Java 25 & React 19)

Java 25, Spring Boot 4, React 19, TypeScript 기반의 **서버 권위형(Server-Authoritative) 실시간 온라인 체스 및 ELO 2000+ AI 대전(PVC) 플랫폼**입니다.

---

## 🏆 프로젝트 완성도 현황 (100% 달성)

- **전체 공정 진척도**: **100% (CP-01 ~ CP-08 전 마일스톤 완수)**
- **품질 지표**: 자동화 테스트 **82종 100% 통과 (All Green)**, TypeScript 번들링 통과

---

## 🛠 기술 스택

- **Backend**: Java 25 (LTS), Spring Boot 4.1.1, Spring WebFlux (Reactive WebSocket), Spring Data JPA, PostgreSQL 18.6
- **AI Engine**: 하이브리드 엔진 (`Stockfish UCI Adapter` ELO 2000+ & 순수 Java `Minimax/Alpha-Beta` Fallback 엔진)
- **Frontend**: React 19, TypeScript, Vite 6
- **Build Tool**: Gradle 9.7.1 Wrapper, npm
- **Testing**: JUnit 5, H2 In-Memory DB, Awaitility, WebTestClient

---

## 📁 프로젝트 구조

```text
jchess/
├─ backend/       # Spring Boot 4 Backend (WebFlux + Data JPA + AI Engine)
├─ frontend/      # React 19 + TypeScript 체스보드 UI & 실시간 연동
├─ preview/       # Chrome 브라우저 즉시 열람용 스탠드얼론 목업 (index.html)
├─ docs/          # 아키텍처, 규칙, 계획서 및 단계별(01~08) 검수 보고서
├─ agent/         # 팀 페르소나 정의 (PM, TL, QM, SM, UX)
├─ init_SETUP.md  # 개발 환경 설치 및 재현 절차서
└─ README.md
```

---

## 🚀 빠른 시작 가이드

### 🌟 1. Chrome에서 UI 목업 즉시 확인 (서버/설치 불필요)
```powershell
start preview\index.html
```

### 💻 2. Backend 실행
```powershell
cd backend
.\gradlew bootRun
```
> REST & WebSocket 포트: `http://localhost:8080`

### 🎨 3. Frontend 실행
```powershell
cd frontend
npm install
npm run dev
```
> 프론트엔드 포트: `http://localhost:5173`

### 🧪 4. 전체 자동화 테스트 실행 (총 82종)
```powershell
cd backend
.\gradlew check
```

---

## 📖 핵심 문서 목록
- [초기 설정 가이드](init_SETUP.md)
- [시스템 아키텍처](docs/architecture.md)
- [프로젝트 진행 계획서](docs/project-plan.md)
- [체스 게임 규칙](docs/chess-game-rules.md)
- [REST & WebSocket 명세서](docs/api-websocket-spec.md)
- [08단계 최종 릴리스 검수서 (100% 완료)](docs/08단계검수.md)

