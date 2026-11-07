# 오픈소스 의존성 CVE 진단 보고서

- 진단 기준일: 2026-11-07 (UTC)
- 대상 저장소: `jaynewstyle5713/jchess`
- 목적: 현재 의존성에서 Critical CVE가 확인되는지 진단하고, 수정 여부 결정에 필요한 결과를 제출한다.
- 조치 범위: 초기에는 진단만 수행했으며, 사용자 승인 후 확인된 Jackson advisory 매칭을 조치했다. 커밋은 하지 않았다.

## 요약

| 항목 | 결과 |
|---|---|
| Critical | 공개 advisory와 현재 조회한 의존성 버전의 매칭에서 확인되지 않음 |
| Frontend | `npm audit --json`: 전체 0건 (Critical 0건 포함) |
| Backend 진단 시점 | Jackson 계열에서 High 7건, Medium 8건의 advisory 매칭 확인 |
| Backend 조치 후 | OSV 재조회에서 155개 좌표 기준 advisory 매칭 0건 |
| 애플리케이션 악용 가능성 | advisory별 설정·입력 조건과 실제 코드 경로를 모두 재현 검증하지 않았으므로 확정하지 않음 |
| 결과 | 사용자의 승인 후 보고된 Jackson advisory 매칭을 패치 버전으로 갱신하고 테스트 통과 |

Critical 부재는 조회한 소프트웨어 구성요소와 공개 데이터베이스 기준의 결과이며, 모든 빌드 플러그인·운영체제·배포 이미지에 대한 전수 보증은 아니다.

## PM / SM / QM 검토

- **PM:** Frontend npm 잠금 파일과 Backend에서 실제 해석되는 애플리케이션 의존성을 진단했다. 사용자가 보고서 검토 후 전체 매칭 항목 조치를 요청해 Jackson 업데이트를 승인했다.
- **SM:** 기존 프런트엔드 수정사항 자체에서는 취약점을 확인하지 않았다. Backend Jackson advisory는 버전과 매칭되었으나, 일부 항목은 특정 직렬화 설정이나 입력 경로가 필요해 실제 악용 가능성을 확정하지 않았다.
- **QM:** 초기 Frontend audit 결과는 0건이었다. 조치 후 `npm audit`, Backend OSV 재조회 및 Gradle 테스트를 검증했다. Gradle의 전용 dependency-check 작업은 없고 GitHub Dependabot Alerts API도 인증이 필요해 접근할 수 없어 OSV 검사 범위 제한을 기록했다.

## 범위와 방법

1. `frontend/package-lock.json`에 대해 `cd frontend && npm audit --json` 실행.
2. Gradle의 `runtimeClasspath` 및 `testRuntimeClasspath` 의존성 그래프를 해석하고, 중복 제거된 Maven 좌표 155개를 OSV `querybatch`로 조회.
3. 현재 런타임에 포함된 Jackson 좌표를 Gradle `dependencyInsight`와 OSV의 개별 버전 조회로 재확인.
4. GitHub Security Advisories 및 OSV advisory의 CVE 식별자, 영향 버전, 수정 버전, CVSS 벡터를 대조.

### 조치 전 Backend 버전 (진단 시점)

| Maven 좌표 | 해석된 버전 | OSV advisory 매칭 수 |
|---|---:|---:|
| `com.fasterxml.jackson.core:jackson-databind` | 2.18.2 | 13 |
| `tools.jackson.core:jackson-databind` | 3.1.5 | 5 |
| `com.fasterxml.jackson.core:jackson-core` | 2.21.5 | 2 |
| `tools.jackson.core:jackson-core` | 3.1.5 | 2 |

합계 22건은 **좌표-버전별 advisory 매칭 수**이며, 중복을 제거하면 서로 다른 advisory 15건(High 7, Medium 8)이다. Jackson 2.x와 3.x 버전이 동시에 런타임 그래프에 존재하므로, 향후 수정 시 Spring Boot 관리 버전과 직접 선언한 Jackson 의존성 간 정합성을 먼저 확인해야 한다.

버전 출처: [backend/build.gradle](../backend/build.gradle#L3) 및 [Jackson 선언](../backend/build.gradle#L25), Gradle에서 해석한 현재 런타임 의존성 그래프.

## 확인된 High 매칭

| # | 심각도 | CVE / Advisory | 현재 매칭 컴포넌트 | 요약 및 공개 수정 버전 | 신뢰도 |
|---:|---|---|---|---|---|
| 1 | HIGH (CVSS 8.1) | [CVE-2026-54512 / GHSA-j3rv-43j4-c7qm](https://github.com/advisories/GHSA-j3rv-43j4-c7qm) | `jackson-databind` 2.18.2 | 다형성 타입 검증기가 generic type 인자를 검증하지 않아 allowlist 우회 가능. 2.x 수정 버전 2.18.8 이상 | 9/10 (버전 매칭; 코드 경로 미확정) |
| 2 | HIGH (CVSS 8.1) | [CVE-2026-54513 / GHSA-rmj7-2vxq-3g9f](https://github.com/advisories/GHSA-rmj7-2vxq-3g9f) | `jackson-databind` 2.18.2 | 배열 하위 타입의 allowlist 검증 우회. 2.x 수정 버전 2.18.8 이상 | 9/10 (버전 매칭; 설정 조건 미확정) |
| 3 | HIGH (CVSS 7.5) | [CVE-2026-91777 / GHSA-cxp5-3px4-pw24](https://github.com/advisories/GHSA-cxp5-3px4-pw24) | `jackson-databind` 2.18.2, 3.1.5 | JSON identity reference 역순 해소 시 quadratic CPU 사용. 수정 버전 2.18.11 / 3.1.7 이상 | 9/10 (버전 매칭; DTO·입력 조건 미확정) |
| 4 | HIGH (CVSS 7.5) | [CVE-2026-91776 / GHSA-wv8q-qhhj-9h54](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) | `jackson-databind` 2.18.2, 3.1.5 | 이름 기반 type ID fallback의 미제한 캐시 증가. 수정 버전 2.18.11 / 3.1.7 이상 | 9/10 (버전 매칭; 설정 조건 미확정) |
| 5 | HIGH (CVSS 7.5) | [CVE-2026-68497 / GHSA-q4xh-88c3-wmh7](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) | `jackson-databind` 2.18.2, 3.1.5 | XML `Duration`/`XMLGregorianCalendar` 파싱의 비제한 숫자 입력에 따른 DoS. 수정 버전 2.18.10 / 3.1.6 이상 | 9/10 (버전 매칭; DTO 노출 여부 미확정) |
| 6 | HIGH (CVSS 7.5) | [CVE-2026-89425 / GHSA-7hhh-6rmp-j9qf](https://github.com/advisories/GHSA-7hhh-6rmp-j9qf) | `jackson-core` 2.21.5, 3.1.5 | `DataInput` JSON parser의 오류 토큰 문자열이 제한 없이 증가할 수 있음. 수정 버전 2.18.11 / 2.21.7 / 3.1.7 이상 | 9/10 (버전 매칭; DataInput 사용 여부 미확정) |
| 7 | HIGH (CVSS 7.5) | [CVE-2026-89407 / GHSA-p6pp-m3f8-5c89](https://github.com/advisories/GHSA-p6pp-m3f8-5c89) | `jackson-core` 2.21.5, 3.1.5 | 숫자 판정 정규식의 quadratic backtracking에 따른 DoS. 수정 버전 2.18.11 / 2.21.7 / 3.1.7 이상 | 9/10 (버전 매칭; 입력 도달성 미확정) |

## 확인된 Medium 매칭

| CVE / Advisory | 현재 매칭 컴포넌트 | 요약 |
|---|---|---|
| [CVE-2026-59888 / GHSA-3pjw-73gf-8qr5](https://github.com/advisories/GHSA-3pjw-73gf-8qr5) | `jackson-databind` 2.18.2 | Record와 naming strategy 조합에서 `@JsonIgnore` 우회 가능 |
| [CVE-2026-59889 / GHSA-5gvw-p9qm-jgwh](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) | `jackson-databind` 2.18.2 | 특정 `@JsonView` / unwrapped deserialization 경로 우회 |
| [CVE-2026-54515 / GHSA-5jmj-h7xm-6q6v](https://github.com/advisories/GHSA-5jmj-h7xm-6q6v) | `jackson-databind` 2.18.2 | 대소문자 무시 설정에서 property ignore 우회 가능 |
| [CVE-2026-83557 / GHSA-gx83-3vf8-gh7j](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) | `jackson-databind` 2.18.2, 3.1.5 | 기본 polymorphic validator에서 `Comparable` 기반 타입 제한 미흡 |
| [CVE-2026-54514 / GHSA-hgj6-7826-r7m5](https://github.com/advisories/GHSA-hgj6-7826-r7m5) | `jackson-databind` 2.18.2 | `InetSocketAddress` 역직렬화 시 DNS 조회로 SSRF 위험 가능 |
| [GHSA-mhm7-754m-9p8w](https://github.com/advisories/GHSA-mhm7-754m-9p8w) | `jackson-databind` 2.18.2 | `@JsonView`와 external type property 조합의 creator property 제한 우회 |
| [CVE-2026-77310 / GHSA-vvgp-rfg2-7rr6](https://github.com/advisories/GHSA-vvgp-rfg2-7rr6) | `jackson-databind` 2.18.2 | DNS resolution 관련 선행 수정의 불완전성 |
| [CVE-2026-19032 / GHSA-wjgm-6hv5-3cvf](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) | `jackson-databind` 2.18.2, 3.1.5 | `Path` 역직렬화 시 파일시스템 provider 선택 제한 미흡 |

## 범위 제한 및 해석

- `npm audit`은 현재 프런트엔드 잠금 파일 기준이며 Backend Java 의존성을 포함하지 않는다.
- OSV 검사는 해석된 Gradle runtime/test-runtime Maven 좌표 기준이다. Gradle 플러그인 classpath, JDK/OS, 컨테이너 이미지 및 저장소 내 별도 배포물은 포함하지 않았다.
- 저장소에 Gradle dependency lockfile이 없으므로 이번 Backend 결과는 현재 해석 그래프의 스냅샷이지 고정 재현 가능한 잠금 결과는 아니다.
- GitHub Dependabot Alerts API는 인증되지 않아 접근하지 못했다. 공개 GitHub Security Advisories와 OSV를 이용해 대조했다.
- 버전과 advisory의 매칭은 확인했지만, 실제 취약한 기능의 사용 여부·공격자 제어 입력 도달성·배포 환경 노출 여부는 별도 분석이 필요하다. 따라서 이 보고서는 exploitability나 침해 사실을 주장하지 않는다.
- Critical 기준은 공개된 CVSS 3.1/Advisory 심각도에서 Critical로 식별되는 항목으로 보았다. 조회 결과 중 가장 높은 점수는 CVSS 8.1 High이며, Critical 매칭은 없었다.

## 조치 및 재검증 결과

- 사용자 승인 후 `backend/build.gradle`에서 `com.fasterxml.jackson` 2.x core, databind, jsr310을 2.21.7로, `tools.jackson` 3.x core와 databind를 3.1.7로 갱신했다.
- Gradle `dependencyInsight`에서 런타임 선택 버전이 각각 2.21.7 및 3.1.7임을 확인했다.
- OSV 재조회: Backend `testRuntimeClasspath`에서 중복 제거한 Maven 좌표 155개, advisory 매칭 0건.
- Frontend `npm audit --json`: info/low/moderate/high/critical 모두 0건.
- Backend `bash gradlew test --console=plain`: `BUILD SUCCESSFUL`.
- Backend 서비스 종료 및 재기동: 기존 JVM을 정상 종료하고 `bash gradlew bootRun --args=--spring.profiles.active=local --console=plain`으로 다시 시작했다. 시작 로그에서 Spring Boot 4.1.1, local 프로필, 포트 8080 기동 완료를 확인했다.
- 실행 JVM classpath: `jackson-core`, `jackson-databind`, `jackson-datatype-jsr310` 2.21.7 및 `tools.jackson` core/databind 3.1.7 포함을 확인했다.
- 재기동 후 서비스 확인: 프런트엔드 HTTP 200, Backend `/actuator/health` HTTP 200 및 `status: UP`.
- 보고된 15개 advisory와 일치하는 버전 매칭은 조치 후 해소됐다. 이는 조사한 앱 의존성 좌표에 대한 결과이며, 빌드 플러그인·JDK/OS·컨테이너 이미지는 여전히 범위 밖이다.

## 완료 상태

진단, 승인된 Jackson 의존성 업데이트, Backend 재기동 및 검증을 마쳤다. 애플리케이션 코드나 보안 설정은 변경하지 않았다.
