# 로컬 빠른 시작 가이드

이 문서는 Linux, Windows PowerShell 또는 VS Code Dev Container에서 jchess를 처음 실행하는 절차입니다. 저장소 루트에서 시작하며 PostgreSQL 설치 없이 프런트엔드와 백엔드를 실행합니다.

## 사전 조건

- Java 25 이상
- Node.js 24 이상 및 npm
- 저장소 루트에서 명령을 실행할 터미널 2개

버전을 확인합니다.

```bash
java -version
node --version
npm --version
```

## 1. 프런트엔드 의존성 설치

저장소 루트에서 실행합니다. 두 OS 모두 `npm ci`는 반드시 `frontend/` 안에서 실행해야 합니다.

Linux / Dev Container:

```bash
cd frontend
npm ci
```

Windows PowerShell:

```powershell
cd frontend
npm ci
```

같은 터미널에서 다음 단계를 진행한다면 `cd ..`로 저장소 루트로 돌아옵니다. 서버를 실행할 터미널은 각각 저장소 루트에서 시작합니다.

## 2. 백엔드 실행

첫 번째 터미널에서 저장소 루트의 `backend/`로 이동해 실행합니다. `local` 프로필은 H2 메모리 DB를 사용하므로 PostgreSQL 설치나 별도 DB 설정이 필요하지 않습니다.

Linux / Dev Container:

```bash
cd backend
bash ./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell:

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Linux의 Gradle Wrapper 파일에 실행 권한이 없는 경우 `./gradlew` 대신 `bash ./gradlew`를 사용합니다. Windows에서는 저장소에 포함된 `gradlew.bat`을 실행합니다. 시작 로그에 `Netty started on port 8080`이 표시되면 백엔드가 준비된 것입니다.

## 3. 프런트엔드 실행

두 번째 터미널에서 저장소 루트의 `frontend/`로 이동해 실행합니다. 아래 명령은 Linux, Windows PowerShell 모두 동일합니다.

```bash
cd frontend
npm run dev -- --host 0.0.0.0
```

시작 로그의 주소 또는 아래 URL을 브라우저에서 엽니다.

- 앱: [http://localhost:3000](http://localhost:3000)
- 백엔드 상태 확인: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

현재 Vite 설정의 포트는 `3000`이며, 프런트엔드의 `/api` 및 `/ws` 요청은 `localhost:8080` 백엔드로 전달됩니다.

## 실행 확인

Linux / Dev Container에서는 다음 명령으로 확인합니다.

```bash
curl -sS http://localhost:3000/
curl -sS http://localhost:8080/actuator/health
```

Windows PowerShell에서는 다음과 같이 확인합니다.

```powershell
(Invoke-WebRequest http://localhost:3000/).StatusCode
(Invoke-RestMethod http://localhost:8080/actuator/health).status
```

프런트엔드는 HTML을 반환하고, 백엔드 상태 응답에 `"status":"UP"`가 있으면 정상입니다.

## 종료 및 참고

각 서버가 실행 중인 터미널에서 `Ctrl+C`를 눌러 종료합니다. `local` 프로필의 H2 데이터베이스는 메모리 기반이라 백엔드를 재시작하면 게임 데이터가 초기화됩니다. 기본 프로필은 PostgreSQL을 사용하므로 이 가이드의 백엔드 명령에서 `local` 프로필을 생략하지 마세요.

프런트엔드 포트 `3000` 또는 백엔드 포트 `8080`이 이미 사용 중이면 기존 프로세스를 확인해 종료하거나 포트 설정과 프록시 설정을 함께 변경해야 합니다.