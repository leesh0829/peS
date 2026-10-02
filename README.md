# peS

가상의 부품 제조 공장을 위한 소형 MES 포트폴리오 프로젝트입니다.

현재 저장소는 **Phase 0~4**, **Phase 5A(자재·생산 LOT 추적)** 및 **Phase 5B(검사·불량코드)** 를 구현했습니다. V6 적용, 실제 검사 API와 검사 목록·상세 화면을 확인했습니다. 전체 입력 폼·역할별 화면 점검은 최종 마무리 단계에 남겨둡니다. 참고 MES 소스는 포함하지 않았습니다.

- [Phase 0 분석 및 설계](docs/phase-0-analysis-and-design.md)
- [Phase 1 실행 환경](docs/phase-1-execution-environment.md)
- [Phase 2 인증과 기준정보](docs/phase-2-auth-and-master-data.md)
- [Phase 3 생산계획과 작업지시](docs/phase-3-planning-and-work-orders.md)
- [Phase 4 생산실적과 대시보드](docs/phase-4-production-results-and-dashboard.md)
- [Phase 5A 자재 LOT와 생산 LOT 추적](docs/phase-5a-lot-tracking.md) — 실행·통합·브라우저 검증 완료
- [Phase 5B 검사와 불량코드](docs/phase-5b-quality-inspections.md) — 자동 테스트·실제 API·목록/상세 브라우저 확인
- [Phase 6 LOT 라벨 발행 이력](docs/phase-6-label-history.md) — 구현, 실제 V7 적용·화면 확인 대기

## 목표 흐름

생산계획 → 작업지시 → 작업 시작 → 생산실적 등록 → 작업 완료 → 대시보드 집계

자재 LOT, 생산 LOT, 검사, 라벨 이력은 위 흐름을 먼저 완성한 뒤 독립된 단계로 확장합니다. 작업 완료와 검사 합격, 완제품 재고 반영은 서로 다른 사건으로 취급합니다.

## 공개 원칙

peS는 독립적으로 설계하고 구현합니다. 업무 개념을 이해하기 위해 검토한 외부 MES의 소스, SQL, 화면 문구, 식별자, 설정값, 계정 및 실제 데이터는 이 저장소에 복사하거나 배포하지 않습니다. 모든 시연 데이터는 가상 데이터만 사용합니다.

## 실행

필수 도구는 Docker Desktop 또는 Docker Engine과 Compose 플러그인입니다.

```bash
docker compose up --build
```

기동 후 다음 주소에서 확인할 수 있습니다.

- 웹: `http://localhost:5173`
- API 상태: `http://localhost:8080/api/health`
- Actuator 상태: `http://localhost:8080/actuator/health`

개발 환경 시연 계정은 `admin`, `manager`, `worker`이고 공통 비밀번호는 `pes-demo-1234`입니다. 이 계정은 Compose가 활성화하는 `dev` 프로필에서만 생성됩니다.

종료할 때는 `docker compose down`을 사용합니다. PostgreSQL 데이터는 `postgres-data` 볼륨에 유지되며, 완전히 초기화할 때만 `docker compose down --volumes`를 사용합니다.

## 로컬 개발과 검증

Compose에서 PostgreSQL만 실행한 뒤 백엔드와 프론트엔드를 각각 실행할 수도 있습니다.

```bash
docker compose up -d database
./backend/gradlew -p backend bootRun
npm --prefix frontend run dev
```

현재 백엔드 테스트와 프론트 린트·프로덕션 빌드는 하나의 명령으로 검증합니다.

```bash
bash scripts/validate.sh
```

`pes_local_password`는 외부 서비스에 쓰지 않는 로컬 Compose 전용 공개 개발값입니다. 실제 배포에서는 `DB_PASSWORD`를 런타임 비밀값으로 주입해야 합니다.

## 관리자 → 작업자 → 관리자 시연

1. `admin`으로 로그인해 가상의 품목(단위 EACH)과 공정을 등록합니다.
2. 생산계획에서 해당 품목의 목표수량 10과 납기를 입력하고 계획을 확정합니다.
3. 확정 계획을 바탕으로 공정과 `worker`를 배정한 목표수량 10의 작업지시를 생성합니다.
4. 로그아웃 후 `worker`로 로그인합니다. 내 작업지시에서 작업을 시작합니다.
5. 생산실적에서 생산 6 / 양품 5 / 불량 1을 등록하고, 생산 4 / 양품 4 / 불량 0을 추가합니다.
6. 내 작업지시에서 누적 생산 10, 잔여 0을 확인하고 작업을 완료합니다.
7. `admin`으로 다시 로그인해 실적 두 건과 대시보드를 확인합니다. 시연 전보다 생산 10, 양품 9, 불량 1 및 완료 작업지시 1건이 증가해야 합니다.

실적은 누적값을 덮어쓰지 않는 증분 기록입니다. 생산수량은 양품+불량과 같아야 하며, 목표 초과와 완료 후 추가를 서버에서 거부합니다. 작업 완료는 검사 합격이나 재고 증가를 의미하지 않습니다. 계획 1:N 작업지시 1:N 실적 관계와 행 잠금을 사용하는 쓰기 트랜잭션의 선택 이유는 [Phase 4 설계](docs/phase-4-production-results-and-dashboard.md)에 설명했습니다.

기동된 **개발 환경**에 기준정보를 등록한 뒤 Node.js로 API 통합 검증을 실행할 수 있습니다.

```bash
node scripts/verify-phase4.mjs
```

이 검증은 가상 계획·작업지시 각 한 건과 실적 두 건을 DB에 추가하고 그대로 보존합니다. 수량 오류, 권한, 목표 초과 동시 등록, 중복 완료, 완료 후 쓰기 거부 및 집계 증가량을 검사합니다. 단독으로 실행하세요. 다른 사용자가 동시에 실적을 변경하면 전체 집계 증가량 검사가 실패할 수 있습니다. `PES_API_URL`과 `PES_DEMO_PASSWORD`로 접속 주소와 개발용 비밀번호를 지정할 수 있습니다.
