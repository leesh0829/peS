# Phase 4 — 생산실적과 대시보드

## 1. 범위

Phase 4는 작업 중인 작업지시에 증분 생산실적을 등록하고 목표수량 달성 후 완료하는 흐름을 구현한다.

- 생산수량·양품수량·불량수량의 증분 실적 등록
- 역할 범위에 따른 생산실적 조회
- 작업지시별 누적수량과 잔여수량 조회
- 목표수량을 정확히 달성한 작업지시 완료
- 확정 계획, 작업지시, 생산실적을 연결한 관리자 대시보드 집계

자재 LOT, 생산 LOT, 검사 판정, 불량코드, 라벨, 재고는 Phase 5 이후 범위다. 작업 완료는 생산 수행의 종료일 뿐 검사 합격이나 완제품 재고 반영을 의미하지 않는다.

## 2. 환경과 라이브러리 확인

- 프로젝트는 Spring Boot 4.1.1, Java 21, Gradle Wrapper 9.7.1을 유지한다. Spring Boot 4.1.1 공식 요구사항은 Java 17 이상과 Gradle 8.14 이상 또는 9.x이며 현재 구성은 이 범위에 포함된다.
- 결과 등록과 완료는 Spring Data JPA의 `@Lock`이 지원하는 repository query lock을 사용한다.
- Phase 0 결정대로 대시보드는 집계 정합성을 보여주는 카드와 표를 먼저 사용한다. 시계열 비교 요구가 아직 없으므로 Recharts 의존성은 추가하지 않는다.

공식 문서:

- [Spring Boot System Requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Data JPA Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html)
- [Recharts Guide](https://recharts.github.io/en-US/guide/)

## 3. 수량 정책

- 실적 한 건의 생산수량은 1 이상인 정수다.
- 양품수량과 불량수량은 0 이상인 정수다.
- `생산수량 = 양품수량 + 불량수량`을 요청 검증, 서비스 규칙, DB 체크 제약에서 확인한다.
- 실적은 기존 값을 덮어쓰지 않는 append-only 증분 기록이다.
- 작업지시의 누적 생산수량은 목표수량을 초과할 수 없다.
- 누적 생산수량이 목표수량과 정확히 같아야 완료할 수 있다.
- 완료된 작업지시에는 실적을 추가하거나 시작·완료 명령을 다시 적용할 수 없다.

## 4. 상태 전환

| 현재 상태 | 명령 | 다음 상태 | 조건 |
|---|---|---|---|
| `WAITING` | 작업 시작 | `IN_PROGRESS` | 배정된 WORKER |
| `IN_PROGRESS` | 실적 등록 | `IN_PROGRESS` | 수량 불변식 충족, 누적수량 목표 이하 |
| `IN_PROGRESS` | 작업 완료 | `COMPLETED` | 누적수량과 목표수량 일치 |
| `COMPLETED` | 시작·실적·완료 | 거부 | 409 Conflict |

MVP 쓰기 권한은 실제 시연 흐름에 맞춰 배정된 `WORKER`로 제한한다. ADMIN과 MANAGER는 전체 작업지시와 실적을 조회하고 대시보드 집계를 확인한다.

## 5. DB 변경

`V4__create_production_results.sql`이 `production_result`를 추가한다.

```text
work_order 1 ─── N production_result N ─── 1 app_user(recorded_by)
```

각 행은 작업지시 UUID, 생산·양품·불량수량, 등록자 UUID와 감사시각을 가진다. 작업지시별 시간순 상세조회와 전체 최신순 조회를 위해 `(work_order_id, created_at)`과 `created_at` 인덱스를 둔다.

## 6. API와 권한

| Method | Path | ADMIN/MANAGER | WORKER |
|---|---|---:|---:|
| `GET` | `/api/production-results` | 전체 조회 | 본인 배정분 조회 |
| `POST` | `/api/work-orders/{id}/results` | 거부 | 배정 건 등록 |
| `POST` | `/api/work-orders/{id}/complete` | 거부 | 배정 건 완료 |
| `GET` | `/api/dashboard/summary` | 허용 | 거부 |

작업지시 응답에는 목표, 누적 생산·양품·불량, 잔여수량을 함께 제공한다. Entity는 직접 반환하지 않고 Response DTO로 변환한다.

## 7. 트랜잭션과 동시성

- 실적 등록은 작업지시 행을 `PESSIMISTIC_WRITE`로 잠근 뒤 상태·소유권·현재 누적수량을 다시 확인하고 실적 한 건을 저장한다.
- 같은 작업지시에 동시에 실적을 등록해도 잠금 획득 후 누적합을 다시 계산하므로 목표 초과 요청 중 하나는 거부된다.
- 완료도 같은 작업지시 행 잠금 안에서 상태와 누적합을 재확인하고 완료시각을 기록한다.
- 동시에 완료를 요청하면 첫 요청만 성공하고 다음 요청은 이미 변경된 상태를 확인해 409를 받는다.
- 대시보드는 여러 aggregate query를 실행하는 read-only 트랜잭션이다. 업무 쓰기 트랜잭션과 결합하지 않는다.

## 8. 대시보드 정의

대시보드는 전체 누적 기준으로 다음을 표시한다.

- 확정 계획 건수와 계획수량
- 작업지시 목표수량과 대기·작업 중·완료 건수
- 누적 생산·양품·불량수량
- 계획 달성률: `생산수량 / 확정 계획수량 × 100`
- 양품률: `양품수량 / 생산수량 × 100`

분모가 0이면 비율은 `null`로 응답하고 화면은 `-`로 표시한다. 실적 데이터가 쌓인 뒤 일자별 추이를 비교할 필요가 확인되면 Recharts 시계열 차트를 별도 확장한다.

## 9. Phase 완료 기준

- 작업자가 여러 번의 증분 실적을 등록할 수 있다.
- 수량 등식 불일치, 음수, 목표 초과, 완료 후 실적 추가가 거부된다.
- 목표수량 달성 전 완료와 중복 완료가 거부된다.
- 관리자 목록과 대시보드 집계가 같은 실적 합계를 반환한다.
- 작업 완료가 검사나 재고 반영을 만들지 않는다.
- 백엔드 테스트·패키징과 프론트 린트·프로덕션 빌드가 통과한다.
- Compose에서 Flyway V4와 작업자 실적 등록 → 완료 → 관리자 집계 흐름을 확인한다.
