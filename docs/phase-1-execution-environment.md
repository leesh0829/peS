# Phase 1 — 실행 환경

## 1. 범위와 결과

Phase 1은 업무 도메인을 구현하지 않고 다음 실행 기반만 만든다.

- React + TypeScript + Vite 단일 페이지 애플리케이션
- React Router와 TanStack Query 기반의 API 상태 화면
- Tailwind CSS와 shadcn/ui 기본 테마
- Spring Boot health API와 Actuator
- PostgreSQL 연결 및 Flyway 최초 마이그레이션
- 프론트엔드, 백엔드, DB를 묶는 Docker Compose

인증, 사용자, 품목, 공정 엔티티와 실제 MES 메뉴는 Phase 2 이후 범위이므로 만들지 않았다.

## 2. 버전 선택

2026-09-29에 공식 문서와 로컬 환경을 확인해 다음 조합을 선택했다.

| 구분 | 선택 버전 | 판단 |
|---|---:|---|
| Java | 21 LTS | Spring Boot 4.1 요구 범위 안의 LTS이며 WSL에 설치됨 |
| Spring Boot | 4.1.1 | 생성 시점의 Spring Initializr 안정 버전 |
| Gradle | Wrapper 고정 | 전역 Gradle 설치 없이 동일한 빌드 도구 사용 |
| Node.js | 24 | Vite 8의 Node 요구사항을 충족하며 WSL에 설치됨 |
| React | 19.2.x | Vite React 템플릿과 React Router 8의 요구 범위를 충족 |
| Vite | 8.3.x | 생성 시점의 안정 버전 |
| PostgreSQL | 18 | Flyway PostgreSQL 모듈의 지원 범위 안에서 Compose 이미지 고정 |
| Tailwind CSS | 4.3.x | shadcn/ui Vite 구성과 함께 사용 |

세부 JavaScript 패키지 버전은 `frontend/package-lock.json`, Java 의존성은 Gradle dependency management와 wrapper에 고정된다. Recharts는 실제 KPI 차트를 만드는 Phase 4에서 추가한다.

## 3. 구성

```text
browser :5173
  └─ nginx / React SPA
       └─ /api, /actuator 프록시
            └─ Spring Boot :8080
                 └─ PostgreSQL :5432
```

- 개발 모드에서는 Vite가 `/api`와 `/actuator`를 `localhost:8080`으로 프록시한다.
- Compose에서는 nginx가 같은 경로를 `backend:8080`으로 프록시한다.
- 브라우저에서 백엔드 호스트를 직접 바꾸지 않아 CORS 설정을 불필요하게 늘리지 않았다.
- 애플리케이션은 환경 변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`로 DB 연결값을 받는다.

## 4. DB 변경

`V1__initialize_application_metadata.sql`은 독립적으로 작성한 최초 마이그레이션이며 `application_metadata` 테이블만 생성한다. 이는 Flyway가 빈 PostgreSQL DB에서 실제로 적용되는 최소 기반을 확인하기 위한 인프라 테이블이다.

- PostgreSQL 18 공식 이미지의 `PGDATA`는 `/var/lib/postgresql/18/docker`이며, 영속 볼륨은 상위 경로 `/var/lib/postgresql`에 마운트한다.
- JPA 스키마 생성은 `ddl-auto: validate`로 막는다.
- 이후 모든 업무 테이블 변경은 새 Flyway 파일로 누적한다.
- 시연용 데이터는 스키마 마이그레이션에 넣지 않는다.

## 5. API

| Method | Path | 인증 | 용도 |
|---|---|---|---|
| `GET` | `/api/health` | 불필요 | 프론트 연결 확인 |
| `GET` | `/actuator/health` | 불필요 | 컨테이너 healthcheck |

그 외 요청은 Spring Security가 인증을 요구한다. 실제 로그인, 세션, 역할 정책은 Phase 2에서 구현한다.

## 6. 변경 파일 요약

- `backend/`: Gradle 기반 Spring Boot 애플리케이션, 설정, health API, 테스트, Flyway, Dockerfile
- `frontend/`: Vite React 애플리케이션, 상태 화면, 프록시, shadcn/ui, nginx, Dockerfile
- `compose.yaml`: PostgreSQL → backend → frontend 순서와 healthcheck
- `scripts/validate-phase1.sh`: 백엔드 테스트와 프론트 빌드 검증 진입점

## 7. 검증 상태

- 2026-09-29 WSL에서 `scripts/validate-phase1.sh`를 실행해 Gradle 테스트와 `bootJar`, Oxlint, Vite 프로덕션 빌드가 성공했다.
- Oxlint는 shadcn/ui가 컴포넌트와 variant를 함께 내보내는 두 파일에 Fast Refresh 권고 경고를 냈지만 오류는 없었다.
- 최초 Windows 기동에서 PostgreSQL 17 이하용 볼륨 경로(`/var/lib/postgresql/data`) 때문에 PostgreSQL 18 컨테이너가 종료되는 문제를 확인하고, 18 이상 공식 경로(`/var/lib/postgresql`)로 수정했다.
- Docker Compose 전체 기동, Flyway의 실제 PostgreSQL 적용, 브라우저 연결 확인은 현재 WSL 배포판에 Docker CLI/엔진 연결이 없어 실행하지 못했다.
- Docker Desktop WSL 통합을 활성화한 환경에서 `docker compose up --build` 후 세 healthcheck와 웹 상태 배지를 확인해야 Phase 1의 런타임 완료 기준이 최종 충족된다.
