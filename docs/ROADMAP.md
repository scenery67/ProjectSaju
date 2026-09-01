# 사주 서비스 로드맵

`/update-roadmap` 명령어로 진행 상황을 관리합니다. Task 번호는 3자리(001, 002, ...).

**📅 최종 업데이트**: 2026-09-01
**📊 진행 상황**: Phase 2 진행 중 (4/10 Tasks 완료)

---

### Phase 1: 핵심 기능 완성

- **Task 001: 정밀 명리학 해석 로직 고도화** - 부분 완료 (형/파/해 남음)
  - ✅ 십성(十神) 계산을 궁합/이별사주 해석에 반영 — `LunarUtil.SHI_SHEN` 기반, 전 10종 관계를 교과서적 오행/음양 정의와 대조 검증(`tenGodOfGanCoversAllTenCategoriesRelativeToGap`)
  - ✅ 육합/충을 궁합에 반영 — 일지(배우자궁) 기준, 지지 인덱스 구조로 유도(`EarthlyBranchRelation`)
  - 형(刑)/파(破)/해(害) 반영 — 구조적으로 유도되지 않는 개별 규칙이라 오류 검증 부담이 커서 보류. 정확한 표를 확보하면 진행
  - ✅ 대운(大運) 계산 및 표시 — 성별 기준 8개 주기(`daYunPeriods`), 생년월일 기준 "지금 지나고 있는 대운"을 계산해 텍스트로 설명
  - ✅ 원본 데이터 그대로 노출하던 결과 문구를 평문 해석으로 교체 — `FiveElementTraits`/`TenGodTraits`로 오행·십성을 성향 문장으로 풀어씀(2026-09-01, 실사용 중 "일반인은 이해 못 한다" 피드백 반영)
  - 년/시 십성은 아직 해석 문장에 미반영 (데이터에는 있음, `SajuChart.yearTenGod`/`timeTenGod`)
- **Task 002: 절기 경계 케이스 테스트 보강** ✅ - 완료
  - ✅ 자시/야자시(23:00~01:00) 경계 테스트 (`lateZiHourKeepsCurrentDayPillarUnderDefaultSect`)
  - ✅ 절기 경계일(년주가 바뀌는 날) 테스트 고정 — 2024 입춘 기준 라이브러리가 실제보다 약 59분 이르게 전환됨을 실측·고정 (`yearPillarChangesAtSolarTermBoundaryNotLunarNewYear`)
  - ✅ 윤년 2/29 케이스 테스트 (`leapDayFebruary29DoesNotThrow`)
- **Task 003: 캐릭터 디자인/네이밍 확정** - 부분 완료 (일러스트 아트는 다음으로 미룸)
  - ✅ 캐릭터명·정체성(톤) 확정 — "다정한 위로형": 이별사주 "다숨", 궁합사주 "설레" (`frontend/src/data/personas.ts`)
  - ✅ UI 완성도 개선 — 참고 사이트(foxbunny.io/saju) 수준의 타이포(Pretendard 실제 로드)·여백·카드 그림자/라운드 적용, 우리 톤은 유지 (색상 팔레트는 안 바꿈)
  - 캐릭터 일러스트(썸네일 이미지)는 여전히 placeholder(🔮) — 참고 사이트(foxbunny.io/saju)와 겹치지 않는 독자 아트로 추후 제작, 실제 아트 제작은 미착수
- **Task 009: "내 사주" 로컬 히스토리 화면** ✅ - 완료
  - ✅ 식별 방식 결정: 서버 저장 없이 이 기기 `localStorage`에만 최근 20건 보관 (계정 도입 전 임시 방편) — Task 007 참고
  - ✅ 결과 조회 시 자동 저장(`saveReadingToHistory`), 목록/재조회/삭제 구현(`MySajuPage`)
  - ✅ 이 결정 덕분에 `reading_record`에 식별자를 추가할 필요가 없어짐 — Task 004(마이그레이션) 스키마에 영향 없음
- **Task 010: 성격/연애/직업/재물/대인관계 프로필 + 4주 상세 정보** ✅ - 완료
  - ✅ `PersonalityProfile`(성격/연애/직업/재물/대인관계) — 십성 5대 분류(`TenGodGroup`) 기반, 인성(학문/안정)은 5항목에 안 맞아 미사용
  - ✅ 지장간·12운성·세운(현재 대운 구간 10년) — 전부 라이브러리 API 직접 활용, 12운성 한자 12종은 실측 후 매핑
  - ✅ 프론트: 참고 사이트 스타일 4주 표(천간/십성/지지/12운성/지장간) + 오행 분포 막대 그래프 (dataviz 스킬 팔레트 검증 완료)
  - 미착수: 12신살·도화살/역마살 등 개별 신살, 신강/신약 지수, 용신 — 신강신약·용신은 유파마다 계산법이 달라 정확도 검증이 훨씬 어려워 별도 논의 필요

### Phase 2: 배포

- **Task 004: Flyway 마이그레이션 도입** ✅ - 완료
  - ✅ `ddl-auto: update` → `validate`로 변경, Flyway가 스키마 소유
  - ✅ 초기 마이그레이션 작성 (`V1__create_reading_record.sql`)
  - ✅ 겸사겸사 `detail` 컬럼을 `@Lob`(Postgres `oid`)에서 `text`로 수정 — `oid`는 대용량 객체용이라 일반 텍스트 저장엔 부적합했음
  - ✅ 로컬 DB를 완전히 새로 만들어 마이그레이션이 빈 스키마에서 정상 적용되는지, 전체 API가 새 스키마로도 잘 동작하는지 확인
  - 참고: Spring Boot 4는 `flyway-core`만으로는 오토컨피그가 안 되고 `spring-boot-starter-flyway`가 따로 필요함(예외 없이 조용히 무시되는 함정) — `build.gradle.kts` 주석에 남겨둠
- **Task 005: Fly.io 앱 / Neon DB 실제 프로비저닝** - 우선순위: 높음
  - `flyctl apps create`로 앱 생성, `backend/fly.toml`의 `app` 값 교체
  - Neon 프로젝트 생성 후 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 확보
- **Task 006: GitHub Secrets/Variables 등록 및 첫 배포 실행** - 우선순위: 중간
  - `FLY_API_TOKEN` 시크릿 등록 (프론트는 GitHub Pages라 저장소 자체 권한만 있으면 됨, 별도 계정/토큰 불필요)
  - `VITE_API_BASE_URL` Actions Variable 등록
  - `deploy-backend.yml` / `deploy-frontend.yml` 첫 실행 확인

### Phase 3: 사용자 / 수익화

- **Task 007: 인증(OAuth 소셜 로그인) 도입** - 우선순위: 낮음
  - 방식은 OAuth 소셜 로그인으로 결정됨(2026-09-01) — 그 전까지 "마이페이지"는 로그인 없이 유지 (Task 009 참고)
  - 도입 시 `reading_record`에 사용자 식별자 컬럼 추가 마이그레이션 필요
  - 도입 전까지는 이름/연락처 등 개인정보 수집 금지 (CLAUDE.md 3.2 참고)
- **Task 008: 결제(프리미엄 상품) 여부 결정** - 우선순위: 낮음
  - 유료 상품 범위 정의
  - PG사 연동 방식 검토
