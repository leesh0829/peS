# Phase 7 — 검증·시연·배포 준비

이 문서는 완료 선언이 아닌 출고 점검표다. 실제 배포, GitHub Actions 실행 및 전체 역할별 브라우저 점검은 별도 확인이 필요하다.

## CI와 로컬 진입점

`.github/workflows/ci.yml`은 main push, pull request 및 수동 실행에서 Java 21/Node 24를 준비하고 lockfile 기반 npm ci 후 `bash scripts/validate.sh`를 실행한다. 백엔드 테스트·bootJar, 프론트엔드 lint·타입 검사·빌드와 사용 가능한 Linux Docker의 Compose config 검사를 포함한다. 운영 비밀값·배포 권한은 요구하지 않는다. 이 CI는 실제 PostgreSQL 통합 및 브라우저 E2E를 대체하지 않는다.

기존 프로젝트 버전을 유지하며 [GitHub Gradle CI 안내](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle), [setup-node](https://github.com/actions/setup-node), [setup-java](https://github.com/actions/setup-java)를 확인했다. `.gitattributes`는 셸·Gradle wrapper·YAML·SQL·Node 검증 스크립트의 LF를 지정한다. 기존 파일 전체의 일괄 정규화는 하지 않는다.

## 개발 환경 통합 검증

Docker Compose로 dev 환경을 기동하고 가상 EACH 품목·공정이 등록된 상태에서 아래를 순서대로 실행한다. 모든 스크립트는 가상 데이터를 추가하고 보존하며, 다른 작성자 없이 단독 실행해야 한다. 운영 DB에서는 실행하지 않는다.

```bash
node scripts/verify-phase4.mjs
node scripts/verify-phase5a.mjs
node scripts/verify-phase5b.mjs
```

Phase 5B는 미검사 6/5/1, 4/4/0 생산 LOT가 필요하므로 5A 뒤 실행한다. 기본 URL은 localhost:8080이며 PES_API_URL, PES_DEMO_PASSWORD를 지정할 수 있다. 이 변수에 실제 비밀값을 넣어 로그·스크린샷·커밋으로 공유하지 않는다.

Phase 6의 실제 API 확인은 2026-10-02에 완료했다: 최초 발행 동시 요청 201/409, 동시 재출력 순번 2/3, 사유 검증, 스냅샷 보존, 작업자 쓰기 거부·이력 조회 및 생산 집계 불변. 이때 가상 LOT 하나에 이력 세 건을 보존했다. 브라우저 라벨 화면은 도구 승인 제한으로 확인하지 못했다.

## 역할별 수동 화면 점검

| 역할 | 확인할 흐름 | 상태 |
| --- | --- | --- |
| ADMIN | 품목·공정·사용자, 계획 확정·작업지시 생성, 실적·대시보드 | 기존 부분 확인, 전체 재점검 대기 |
| MANAGER | 허용된 기준정보·계획·작업지시·검사·라벨, 사용자 관리 접근 거부 | 전체 재점검 대기 |
| WORKER | 본인 지시 시작·실적·완료, LOT·검사·라벨 읽기, 관리 쓰기 거부 | API 일부 확인, 전체 화면 재점검 대기 |
| 공통 | 로그인 실패, 로그아웃, 검색·빈 목록·페이지 이동, 필드 오류, 좁은 화면 | 전체 재점검 대기 |

라벨은 LOT 추적 → 생산 LOT → 역방향 조회에서 최초 발행 → 미리보기 → 사유 입력 후 재출력을 확인한다. 이력 조회는 새 발행을 만들지 않아야 한다. 인쇄 성공·합격·재고 의미가 없음을 화면에서 확인한다.

## 공개 배포 전 필수 조건

- 현재 `compose.yaml`은 dev 전용이다. 공개 기본 비밀번호와 dev 계정을 사용하므로 인터넷에 노출하지 않는다.
- 운영은 신규 DB/볼륨과 별도 비밀값을 사용한다. dev DB를 운영으로 재사용하면 dev 프로필을 꺼도 기존 시연 계정은 남는다.
- dev 프로필을 비활성화하고 안전한 최초 ADMIN 발급 절차를 구현·검증해야 한다. 현재 자동 운영 관리자 생성 기능은 없다.
- HTTPS 종료 지점을 정하고 SESSION_COOKIE_SECURE=true로 설정한다. 세션 기반 인증이므로 프론트와 API는 동일 출처로 제공한다. 단일 인스턴스의 메모리 세션은 재시작 시 로그아웃된다.
- DB_URL, DB_USERNAME, DB_PASSWORD를 런타임 주입하고 공개 개발값을 사용하지 않는다. DB·백엔드 포트는 내부망에 두고 웹 프록시만 공개한다.
- 마이그레이션 전 DB 백업 및 복구 시연, 지속 볼륨, 프록시·헬스체크·로그의 개인정보 제외를 확인한다. 기존 Flyway 파일 수정이나 볼륨 삭제를 배포 방법으로 사용하지 않는다.
- DB 스키마 변경 롤백은 이미지 되돌리기만으로 해결되지 않는다. 적용 전 호환성과 복구 방식을 정한다.

대상 서버·도메인·TLS·운영 계정 발급이 결정되기 전에는 배포 완료로 표기하지 않는다.
