# Phase 2 — 인증과 기준정보

## 1. 범위

Phase 2는 생산계획을 만들기 전 필요한 사용자 인증과 품목·공정 기준정보를 구현한다.

- `ADMIN`, `MANAGER`, `WORKER` 역할과 세션 로그인
- CSRF 보호와 BCrypt 비밀번호 해시
- 사용자 생성, 역할 변경, 활성/비활성 관리
- 품목과 공정의 생성, 조회, 수정, 사용 여부 관리
- 검색, 상태 필터, 페이지네이션
- 역할별 메뉴 노출과 서버 API 권한 검사

생산계획과 작업지시는 Phase 3 범위이므로 아직 만들지 않는다.

## 2. 인증 정책

브라우저 기반 단일 웹 애플리케이션이고 초기 배포가 단일 Spring Boot 인스턴스이므로 서버 세션 인증을 사용한다.

- 로그인 성공 시 `JSESSIONID` 쿠키를 발급하며 JavaScript에서는 읽을 수 없다.
- 세션 유휴 만료 시간은 30분이다.
- 로그인할 때 세션 ID를 교체해 session fixation을 방어한다.
- CSRF 토큰은 `/api/auth/csrf`에서 발급받아 변경 요청의 `X-XSRF-TOKEN` 헤더로 보낸다.
- 로그인과 로그아웃 직후에는 이전 CSRF 토큰이 제거되므로 프론트엔드가 새 토큰을 다시 발급받는다.
- 비밀번호는 BCrypt cost 12로 단방향 해시한 값만 저장한다.
- 다중 인스턴스 배포 전까지 세션 저장소는 애플리케이션 메모리이며, 서버 재시작 시 로그아웃된다.

토큰 인증을 선택하지 않았으므로 access/refresh token 저장·회수 정책은 필요하지 않다. 이후 프론트와 API를 별도 도메인이나 다중 인스턴스로 분리할 때 인증 방식을 다시 평가한다.

## 3. 역할과 권한

| 기능 | ADMIN | MANAGER | WORKER |
|---|---:|---:|---:|
| 사용자 조회/등록/수정 | 허용 | 거부 | 거부 |
| 품목·공정 조회 | 허용 | 허용 | 허용 |
| 품목·공정 등록/수정 | 허용 | 허용 | 거부 |
| health/CSRF 발급 | 허용 | 허용 | 허용 |

프론트 메뉴는 역할에 따라 숨기지만 보안 경계로 사용하지 않는다. 모든 권한은 Spring Security에서 HTTP method와 경로를 기준으로 다시 검사한다.

## 4. DB 변경

`V2__create_identity_and_master_data.sql`이 다음 테이블을 생성한다.

```text
app_user
  ├─ username UNIQUE
  ├─ password_hash
  ├─ role: ADMIN | MANAGER | WORKER
  └─ active, version, audit timestamps

product
  ├─ code UNIQUE
  ├─ name, unit: EACH | KILOGRAM
  └─ active, version, audit timestamps

production_process
  ├─ code UNIQUE
  ├─ name, description
  └─ active, version, audit timestamps
```

업무 이력에서 참조할 기준정보이므로 물리 삭제 대신 `active` 상태를 변경한다. 수정 요청은 클라이언트가 읽은 `version`을 보내며, 버전이 달라지면 409로 거부한다. 마지막 활성 `ADMIN`은 비활성화하거나 다른 역할로 변경할 수 없다.

## 5. API

| Method | Path | 권한 | 용도 |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | 공개 | CSRF 토큰 발급 |
| `POST` | `/api/auth/login` | 공개 + CSRF | 세션 로그인 |
| `POST` | `/api/auth/logout` | 로그인 + CSRF | 세션 로그아웃 |
| `GET` | `/api/auth/me` | 로그인 | 현재 사용자 |
| `GET/POST/PUT` | `/api/admin/users` | ADMIN | 사용자 관리 |
| `GET` | `/api/products` | 모든 역할 | 품목 조회 |
| `POST/PUT` | `/api/products` | ADMIN, MANAGER | 품목 등록·수정 |
| `GET` | `/api/processes` | 모든 역할 | 공정 조회 |
| `POST/PUT` | `/api/processes` | ADMIN, MANAGER | 공정 등록·수정 |

Entity는 API로 직접 반환하지 않으며 Request/Response DTO를 사용한다. 검증 실패, 중복, 미존재, 권한 오류, 동시 수정은 공통 오류 형태와 적절한 HTTP 상태로 응답한다.

## 6. 개발 전용 계정

Compose의 백엔드는 `dev` 프로필로 실행되며 계정이 없을 때만 다음 가상 계정을 생성한다.

| 아이디 | 역할 | 비밀번호 |
|---|---|---|
| `admin` | ADMIN | `pes-demo-1234` |
| `manager` | MANAGER | `pes-demo-1234` |
| `worker` | WORKER | `pes-demo-1234` |

계정 생성 코드는 `@Profile("dev")`로 제한되어 있으며 Flyway 마이그레이션과 분리된다. 운영 환경에서는 `dev` 프로필을 사용하지 않는다.

## 7. 트랜잭션 경계

- 목록 조회는 read-only 트랜잭션이다.
- 사용자, 품목, 공정의 생성·수정은 서비스 메서드 하나가 트랜잭션 하나를 구성한다.
- 중복 검사는 사용자에게 명확한 메시지를 주기 위해 서비스에서 먼저 수행하고, 최종 무결성은 DB 고유 제약이 보장한다.
- optimistic version을 요청값과 비교하고 JPA `@Version`도 함께 사용해 stale update와 동시에 발생한 갱신을 방어한다.

## 8. 검증 기준

- 개발 계정의 로그인·로그아웃과 세션 유지가 동작한다.
- WORKER의 사용자 관리 API 호출은 403으로 거부된다.
- ADMIN은 사용자 API를 호출할 수 있다.
- 마지막 활성 관리자 보호와 비밀번호 해시 테스트가 통과한다.
- 품목·공정의 등록, 조회, 수정과 역할별 읽기/쓰기 구분을 브라우저에서 확인한다.

## 9. 검증 상태

- 2026-09-29 WSL에서 `scripts/validate.sh`를 실행해 백엔드 테스트 6건, `bootJar`, Oxlint, Vite 프로덕션 빌드가 성공했다.
- 권한 테스트는 비로그인 401, WORKER 403, ADMIN 200을 검증한다.
- React 화면은 경로별 동적 import를 적용해 초기 번들을 500 kB 아래로 유지했다.
- shadcn/ui 생성 파일 두 곳의 Fast Refresh 권고 경고 외에 린트 오류는 없다.
- Docker에서 V2 마이그레이션, 로그인, 역할별 API와 기준정보 입력을 확인하면 Phase 2 런타임 완료 기준이 충족된다.
