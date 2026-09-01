# 사주 서비스 로드맵

`/update-roadmap` 명령어로 진행 상황을 관리합니다. Task 번호는 3자리(001, 002, ...).

**📅 최종 업데이트**: 2026-09-01
**📊 진행 상황**: Phase 1 진행 중 (1/8 Tasks 완료)

---

### Phase 1: 핵심 기능 완성

- **Task 001: 정밀 명리학 해석 로직 고도화** - 우선순위: 높음
  - 십성(十神) 계산을 궁합/이별사주 해석에 반영
  - 합충형파해를 반영한 궁합 스코어링 (현재는 오행 상생상극만 반영)
  - 대운(大運) 계산 및 표시
- **Task 002: 절기 경계 케이스 테스트 보강** ✅ - 완료
  - ✅ 자시/야자시(23:00~01:00) 경계 테스트 (`lateZiHourKeepsCurrentDayPillarUnderDefaultSect`)
  - ✅ 절기 경계일(년주가 바뀌는 날) 테스트 고정 — 2024 입춘 기준 라이브러리가 실제보다 약 59분 이르게 전환됨을 실측·고정 (`yearPillarChangesAtSolarTermBoundaryNotLunarNewYear`)
  - ✅ 윤년 2/29 케이스 테스트 (`leapDayFebruary29DoesNotThrow`)
- **Task 003: 캐릭터 디자인/네이밍 확정** - 우선순위: 중간
  - `frontend/src/data/personas.ts`의 placeholder 캐릭터 교체
  - 참고 사이트(foxbunny.io/saju)와 겹치지 않는 독자 캐릭터로 확정

### Phase 2: 배포

- **Task 004: Flyway 마이그레이션 도입** - 우선순위: 높음 (배포 선결 조건)
  - `ddl-auto: update` 제거
  - 초기 스키마 마이그레이션 스크립트 작성 (`reading_record` 테이블)
- **Task 005: Fly.io 앱 / Neon DB 실제 프로비저닝** - 우선순위: 높음
  - `flyctl apps create`로 앱 생성, `backend/fly.toml`의 `app` 값 교체
  - Neon 프로젝트 생성 후 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 확보
- **Task 006: GitHub Secrets/Variables 등록 및 첫 배포 실행** - 우선순위: 중간
  - `FLY_API_TOKEN`, `CF_API_TOKEN`, `CF_ACCOUNT_ID` 시크릿 등록
  - `VITE_API_BASE_URL` Actions Variable 등록
  - `deploy-backend.yml` / `deploy-frontend.yml` 첫 실행 확인

### Phase 3: 사용자 / 수익화

- **Task 007: 인증 방식 결정 및 구현** - 우선순위: 낮음
  - 로그인 필요 여부 및 방식(소셜/이메일) 결정
  - 결정 전까지는 이름/연락처 등 개인정보 수집 금지 (CLAUDE.md 3.2 참고)
- **Task 008: 결제(프리미엄 상품) 여부 결정** - 우선순위: 낮음
  - 유료 상품 범위 정의
  - PG사 연동 방식 검토
