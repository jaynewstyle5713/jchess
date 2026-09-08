# jchess 초기 설정 기록

이 문서는 교육용으로 배포할 jchess 프로젝트의 설치 및 초기 설정 과정을 기록한다.
프로젝트를 새로 내려받은 학습자가 이 문서만 보고 개발 환경을 재현할 수 있도록 실제 실행 명령, 버전, 검증 결과를 함께 작성한다.

## 1. 기록 원칙

- 설치 또는 설정 명령을 실행할 때마다 이 문서에 명령과 결과를 기록한다.
- 운영체제별 명령이 다르면 Windows 기준을 먼저 기록하고 필요한 경우 macOS/Linux 명령을 함께 기록한다.
- 설치 전제 조건, 환경 변수, 프로젝트 초기화, 의존성 설치, 실행 및 테스트 검증을 구분한다.
- 자동으로 설치되는 항목과 사용자가 직접 설치해야 하는 항목을 명확히 구분한다.
- 버전이 바뀌면 기존 기록을 삭제하지 않고 변경 일자와 이유를 추가한다.

## 2. 현재 상태

- 확인일: 2026-09-08
- 운영체제: Windows
- 프로젝트 경로: `c:\work\jchess`
- 현재 저장된 문서: `docs/chess-game-rules.md`, `docs/architecture.md`
- 소스 코드: 아직 없음

### 사전 설치 확인

다음 명령으로 개발 도구 설치 여부를 확인했다.

```powershell
java -version
mvn -version
gradle -version
```

초기 확인 결과 Java, Maven, Gradle이 없었으나 이후 Java 25 설치를 완료했다. Maven과 Gradle은 전역 설치하지 않고 Gradle Wrapper를 프로젝트에 포함할 예정이다.

## 3. 확정 아키텍처

상세 내용은 [`docs/architecture.md`](docs/architecture.md)에 기록한다.

- Java 25 이상
- Spring Boot 4
- Spring WebFlux 및 WebSocket
- React 및 TypeScript
- JPA/Hibernate와 관계형 데이터베이스
- MSA를 목표로 하되 초기에는 모듈형 모놀리스로 시작
- WebFlux 이벤트 루프와 JPA blocking 작업을 명시적으로 분리
- 서버 권위형 체스 규칙 및 게임 상태 판정
- MVP active game state는 PostgreSQL snapshot과 수 기록을 기준으로 복구
- Redis active state와 MSA 독립 배포는 MVP 이후 검토

아키텍처 결정에 따라 설치 순서는 JDK, 빌드 도구, Backend, Frontend, 데이터베이스, 테스트 도구 순으로 진행한다.

## 4. 설치 예정 항목

프로젝트 구조와 서버 기술을 확정한 뒤 아래 항목을 설치한다.

- [x] JDK LTS (Java 25)
- [x] Java 빌드 도구: Gradle Wrapper (Gradle 9.7.1)
- [x] 온라인 게임 서버 프레임워크: Spring Boot 4.1.1 (WebFlux + Data JPA)
- [x] 프론트엔드: React 19 + TypeScript + Vite 6
- [x] 데이터베이스: PostgreSQL 18.6
- [x] 테스트 도구: JUnit 5, H2 (Test Database)

## 5. 설치 기록

### 5.0 PM 최종 검토 전제

아래 모든 항목의 실제 버전 및 검증 결과를 확인하고 초기화 검증을 완료했다.

- [x] Java 배포판과 정확한 Java 25 이상 버전: Microsoft OpenJDK 25.0.4.1 LTS
- [x] Spring Boot 4 정확한 안정 버전: Spring Boot 4.1.1
- [x] Maven 또는 Gradle 선택, 버전, Wrapper 검증: Gradle 9.7.1 Wrapper
- [x] Node.js와 package manager 버전: Node.js v24.19.0, npm 11.17.0
- [x] React, TypeScript, Vite 버전: React 19.2.8, TypeScript 5.7.2, Vite 6.2.0
- [x] PostgreSQL 버전과 포트 검증: PostgreSQL 18.6 (포트 5432)
- [x] Backend/Frontend 빌드 및 테스트 결과: Backend `./gradlew check` 및 Frontend `npm run build` 통과

### 4.1 JDK

설치 완료.

- 패키지: Microsoft Build of OpenJDK 25
- 확인 버전: `25.0.4.1` LTS
- 검증 결과: `java -version`, `javac -version` 성공
- 설치 명령:

```powershell
$winget = "$env:LOCALAPPDATA\Microsoft\WindowsApps\winget.exe"
& $winget install --source winget --id Microsoft.OpenJDK.25 --accept-source-agreements --accept-package-agreements --disable-interactivity
```

### 4.2 빌드 도구 (Gradle Wrapper)

구성 완료.

- 방식: Gradle Wrapper 내장 (전역 설치 불필요)
- 버전: Gradle 9.7.1
- 위치: `backend/gradlew`, `backend/gradlew.bat`
- 검증 명령: `cd backend; .\gradlew --version`

### 4.3 Node.js 및 npm

설치 완료.

- 패키지: Node.js LTS
- 확인 버전: `v24.19.0`
- npm 버전: `11.17.0`
- 검증 결과: `node --version`, `npm --version` 성공

### 4.4 PostgreSQL

설치 완료.

- 패키지: PostgreSQL 18
- 확인 버전: `18.6`
- 서비스 상태: `postgresql-x64-18` (Running, Port: 5432)
- PATH 설정: `C:\Program Files\PostgreSQL\18\bin` 환경변수 등록 완료
- 검증 결과: `psql --version` (PostgreSQL 18.6) 성공 및 포트 5432 연결 검증

```powershell
psql --version
Test-NetConnection -ComputerName localhost -Port 5432
```

## 6. 프로젝트 초기화 기록

### 6.1 Backend (`backend/`)

- Java 버전: Java 25 (toolchain)
- 프레임워크: Spring Boot 4.1.1
- 패키지명: `com.jchess`
- 아티팩트명: `backend`
- 주요 의존성:
  - `spring-boot-starter-webflux`: 비동기 반응형 웹 및 WebSocket 지원
  - `spring-boot-starter-data-jpa`: 관계형 데이터 영속화
  - `spring-boot-starter-validation`: 입력값 검증
  - `spring-boot-starter-actuator`: 헬스체크 및 모니터링
  - `postgresql`: PostgreSQL JDBC Driver (런타임)
  - `h2`: 테스트 전용 인메모리 DB
- 빌드 및 테스트 검증:
  ```powershell
  cd c:\work\jchess\backend
  .\gradlew check
  ```
  결과: `BUILD SUCCESSFUL`

### 6.2 Frontend (`frontend/`)

- 번들러 및 도구: Vite 6.2.0
- 프레임워크: React 19.2.8
- 언어: TypeScript 5.7.2
- 주요 스크립트:
  - `npm run dev`: 로컬 개발 서버 구동 (포트 3000, `/api` 및 `/ws` 프록시 설정)
  - `npm run build`: TypeScript 컴파일(`tsc -b`) 및 프로덕션 번들 빌드
- 빌드 검증:
  ```powershell
  cd c:\work\jchess\frontend
  npm run build
  ```
  결과: `✓ built in 1.34s` (dist 산출물 생성 확인)

## 7. 교육용 재현 절차

프로젝트 초기 설정이 완료되면 다음 순서로 새 환경에서 재현할 수 있다.

1. **JDK 25 설치**: OpenJDK 25 설치 및 `JAVA_HOME` 확인
2. **Node.js LTS 설치**: Node.js v24+ 설치 및 npm 확인
3. **PostgreSQL 18 설치**: PostgreSQL 18 설치 및 서비스 구동
4. **저장소 복제**: `git clone <repository_url>`
5. **Backend 빌드 및 테스트**:
   ```powershell
   cd backend
   .\gradlew check
   ```
6. **Frontend 의존성 설치 및 빌드**:
   ```powershell
   cd frontend
   npm install
   npm run build
   ```
7. **서버 및 클라이언트 실행**:
   - Backend: `cd backend; .\gradlew bootRun` (포트 8080)
   - Frontend: `cd frontend; npm run dev` (포트 3000)

## 8. 변경 이력

| 날짜 | 내용 |
| --- | --- |
| 2026-09-08 | `init_SETUP.md` 생성, Java/Maven/Gradle 설치 여부 확인 |
| 2026-09-08 | `docs/architecture.md` 생성, Java 25+/Spring Boot 4/React/TypeScript/WebFlux/JPA 및 단계적 MSA 결정 |
| 2026-09-08 | Microsoft OpenJDK 25, Node.js LTS, PostgreSQL 18 설치 및 검증 완료 |
| 2026-09-08 | Spring Boot 4.1.1 Backend 및 Vite React/TypeScript Frontend 스켈레톤 초기화 및 빌드/테스트 검증 완료 |
