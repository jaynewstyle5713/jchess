# jchess - 온라인 실시간 체스 플랫폼

Java 25와 Spring Boot 4, React 19, TypeScript 기반의 실시간 온라인 체스 게임 교육용 프로젝트입니다.

---

## 🛠 기술 스택

- **Backend**: Java 25 (LTS), Spring Boot 4.1.1, Spring WebFlux, Spring Data JPA, PostgreSQL 18.6
- **Frontend**: React 19, TypeScript, Vite 6
- **Build Tool**: Gradle 9.7.1 Wrapper, npm
- **Testing**: JUnit 5, H2 In-Memory DB

---

## 📁 프로젝트 구조

```text
jchess/
├─ backend/       # Spring Boot 4 Backend (WebFlux + Data JPA)
├─ frontend/      # Vite + React + TypeScript Frontend
├─ docs/          # 아키텍처, 규칙, 계획서 및 검수 문서
├─ agent/         # 팀 역할 정의 (PM, TL, QM, SM, UX)
├─ init_SETUP.md  # 개발 환경 설치 및 재현 절차서
└─ README.md
```

---

## 🚀 시작하기

### 1. 전제 조건
- Java 25+ JDK
- Node.js v24+ & npm
- PostgreSQL 18+

### 2. Backend 실행
```powershell
cd backend
.\gradlew bootRun
```
> 서버 포트: `http://localhost:8080`

### 3. Frontend 실행
```powershell
cd frontend
npm install
npm run dev
```
> 클라이언트 포트: `http://localhost:3000`

---

## 📖 문서
- [초기 설정 가이드](init_SETUP.md)
- [시스템 아키텍처](docs/architecture.md)
- [프로젝트 진행 계획서](docs/project-plan.md)
- [체스 게임 규칙](docs/chess-game-rules.md)
