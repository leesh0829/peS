# peS

가상의 부품 제조 공장을 위한 소형 MES 포트폴리오 프로젝트입니다.

현재 저장소는 **Phase 0(자료 분석과 설계)**, **Phase 1(실행 환경)**, **Phase 2(인증과 기준정보)**, **Phase 3(생산계획과 작업지시)** 를 완료했습니다. 다음 구현 범위는 Phase 4의 생산실적과 대시보드입니다. 참고 MES 소스는 포함하지 않았습니다.

- [Phase 0 분석 및 설계](docs/phase-0-analysis-and-design.md)
- [Phase 1 실행 환경](docs/phase-1-execution-environment.md)
- [Phase 2 인증과 기준정보](docs/phase-2-auth-and-master-data.md)
- [Phase 3 생산계획과 작업지시](docs/phase-3-planning-and-work-orders.md)

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
