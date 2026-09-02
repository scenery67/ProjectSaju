# 사주 서비스 로드맵

`/update-roadmap` 명령어로 진행 상황을 관리합니다. Task 번호는 3자리(001, 002, ...).

**📅 최종 업데이트**: 2026-09-02
**📊 진행 상황**: Phase 5 완료, Phase 6 거의 완료 (8/13 Tasks 완료, 실제 API 키 연동만 남음)

**앞으로의 큰 순서** (2026-09-01 합의): 사주 내용 개선 → 로그인 추가 → 마이페이지 추가 → 과금 구조 추가 → 화면 개선(2차)

---

### Phase 1: 핵심 기능 완성 ✅

- **Task 001: 정밀 명리학 해석 로직 고도화** - 부분 완료 (형/파/해, 신살은 Phase 3-A로 이관)
  - ✅ 십성(十神) 계산을 궁합/이별사주 해석에 반영 — `LunarUtil.SHI_SHEN` 기반, 전 10종 관계를 교과서적 오행/음양 정의와 대조 검증(`tenGodOfGanCoversAllTenCategoriesRelativeToGap`)
  - ✅ 육합/충을 궁합에 반영 — 일지(배우자궁) 기준, 지지 인덱스 구조로 유도(`EarthlyBranchRelation`)
  - ✅ 대운(大運) 계산 및 표시 — 성별 기준 8개 주기(`daYunPeriods`), 생년월일 기준 "지금 지나고 있는 대운"을 계산해 텍스트로 설명
  - ✅ 원본 데이터 그대로 노출하던 결과 문구를 평문 해석으로 교체 — `FiveElementTraits`/`TenGodTraits`로 오행·십성을 성향 문장으로 풀어씀(실사용 중 "일반인은 이해 못 한다" 피드백 반영)
  - 형(刑)/파(破)/해(害), 년/시 십성의 해석 문장 반영 → Phase 3-A로 이관
- **Task 002: 절기 경계 케이스 테스트 보강** ✅ - 완료
  - ✅ 자시/야자시(23:00~01:00) 경계 테스트, 절기 경계일 테스트(2024 입춘 기준 라이브러리가 실제보다 약 59분 이르게 전환됨을 실측·고정), 윤년 2/29 케이스
- **Task 003: 캐릭터 디자인/네이밍 확정** - 부분 완료 (일러스트 아트는 Phase 6으로 이관)
  - ✅ 캐릭터명·정체성(톤) 확정 — "다정한 위로형": 이별사주 "다숨", 궁합사주 "설레"
  - ✅ 1차 UI 완성도 개선 — 참고 사이트(foxbunny.io/saju) 수준의 타이포(Pretendard)·여백·카드 그림자/라운드, 톤은 유지
  - 캐릭터 일러스트(썸네일 이미지)는 여전히 placeholder(🔮) — Phase 6에서 진행
- **Task 009: "내 사주" 로컬 히스토리 화면** ✅ - 완료
  - ✅ 서버 저장 없이 이 기기 `localStorage`에만 최근 20건 보관 (계정 도입 전 임시 방편)
  - ✅ 이 결정 덕분에 `reading_record`에 식별자를 추가할 필요가 없어짐
- **Task 010: 성격/연애/직업/재물/대인관계 프로필 + 4주 상세 정보** ✅ - 완료
  - ✅ `PersonalityProfile` — 십성 5대 분류(`TenGodGroup`) 기반, 성격은 일간 자체 오행으로 설명
  - ✅ 지장간·12운성·세운(현재 대운 구간 10년) — 전부 라이브러리 API 직접 활용, 12운성 한자 12종은 실측 후 매핑
  - ✅ 프론트: 4주 표(천간/십성/지지/12운성/지장간) + 오행 분포 막대 그래프 (dataviz 스킬 팔레트 검증 완료)

### Phase 2: 배포 ✅

- **Task 004: Flyway 마이그레이션 도입** ✅ - 완료
  - `ddl-auto: update` → `validate`, 초기 마이그레이션(`V1__create_reading_record.sql`), `detail` 컬럼을 `@Lob`(oid)에서 `text`로 수정
  - 참고: Spring Boot 4는 `flyway-core`만으로는 오토컨피그가 안 되고 `spring-boot-starter-flyway`가 따로 필요함(조용히 무시되는 함정)
- **Task 005: Fly.io 앱 / Neon DB 실제 프로비저닝** ✅ - 완료
  - `dasum-saju-api` 앱 생성, Neon DB(`noisy-salad-38808303`) 생성, Fly secrets(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`CORS_ALLOWED_ORIGINS`) 등록
- **Task 006: GitHub Secrets/Variables 등록 및 첫 배포 실행** ✅ - 완료
  - `FLY_API_TOKEN` 시크릿, `VITE_API_BASE_URL` Variable 등록
  - 프론트 배포를 Cloudflare Pages → **GitHub Pages**로 전환 (별도 계정/토큰 불필요, `HashRouter` + `base: /ProjectSaju/`)
  - CI에 Postgres 서비스 추가, `gradlew` 실행권한 비트 수정 (리눅스 CI에서만 터지던 버그 — Windows 로컬에선 재현 안 됨)
  - 실제 프로덕션(GitHub Pages + Fly.io + Neon)에서 폼 제출 end-to-end 확인 완료
  - 배포 주소: 프론트 https://scenery67.github.io/ProjectSaju/ · 백엔드 https://dasum-saju-api.fly.dev

### Phase 3-A: 사주 내용 개선 (진행 중)

- **Task 011: 12신살/개별 신살, 신강/신약, 용신** - 보류 (원인 확인만 완료)
  - 조사 결과: `cn.6tail:lunar`의 `EightChar`엔 신살(神煞) 관련 메서드가 전혀 없음(도화살/역마살/괴강살/천을귀인 등 미지원) — 라이브러리 설명의 "吉神宜趋凶煞宜忌"는 일진(日辰) 단위 황도길흉 기능이지 사주 신살과는 다른 기능
  - 직접 구현하면 형/파/해와 같은 문제: 구조적으로 유도 안 되는 개별 표를 하드코딩해야 해서 출처 검증 부담이 큼. 신강/신약·용신은 그보다 더 유파마다 계산법이 갈려 정확도 검증이 훨씬 어려움
  - 다음에 진행한다면: 역마살·도화살처럼 지지 삼합 그룹 기준으로 구조적으로 유도되는 것부터(육합/충과 같은 방식) 우선 시도
- 형(刑)/파(破)/해(害) 반영한 정밀 궁합 — Task 001에서 이관, 위와 같은 이유로 보류
- 년/시 십성을 해석 문장에도 반영 (현재는 4주 표에만 노출)

### Phase 4: 로그인 (OAuth) - 진행 중

- **Task 007: 인증(OAuth 소셜 로그인) 도입** - 부분 완료 (기능 검증까지, 실사용 연결은 의도적으로 보류)
  - ✅ 카카오/구글/네이버 3개 provider 모두 `spring-boot-starter-oauth2-client`로 구현 — 카카오/네이버는 Spring 기본 제공 목록에 없어 `provider` 블록에 엔드포인트 직접 지정, 속성 구조도 제각각(카카오는 `kakao_account.profile.nickname`, 네이버는 `response.nickname`)이라 `OAuthUserInfo`로 정규화
  - ✅ 프론트(GitHub Pages)와 백엔드(Fly.io)가 다른 오리진이라 쿠키 세션 대신 로그인 성공 후 백엔드가 자체 JWT 발급 → `#/auth/callback?token=...`로 리다이렉트하는 방식 채택
  - ✅ `user_account` 테이블(`V2__create_user_account.sql`) — provider+provider_user_id+닉네임만 저장, 이메일 등은 저장 안 함
  - ✅ `/api/auth/me`에 `defaultAuthenticationEntryPointFor` 적용 — 안 하면 미인증 요청이 401 대신 302(OAuth 로그인 페이지)로 응답해서 fetch 호출자가 처리 못 함
  - ✅ 검증: 3개 provider 로그인 URL이 각각 실제 카카오/구글/네이버 인가 엔드포인트로 정확히 리다이렉트되는 것 확인(client-id는 아직 placeholder라 실제 로그인 완주는 안 됨), 실제 발급 로직과 동일한 방식의 JWT로 `/api/auth/me` → 프론트 로그인 상태 표시까지 브라우저로 end-to-end 확인
  - ✅ `OAuthUserInfo`/`JwtService` 단위 테스트로 속성 정규화·토큰 발급/검증 고정
  - 남음: 카카오/구글/네이버 개발자 콘솔에 실제 앱 등록 → `KAKAO_CLIENT_ID` 등 Fly secrets 등록 → 프로덕션에서 실제 로그인 완주 확인
  - **의도적으로 안 한 것**(2026-09-01 결정): 로그인을 아직 다른 기능과 연결하지 않음. `reading_record`에도 사용자 식별자를 아직 추가하지 않음 — LLM 사주 상담(질문 횟수 기준 과금) 설계가 먼저 끝나야 어떤 사용량/과금 데이터를 사용자와 연결할지 정해짐 (Task 008 참고)

### Phase 5: 마이페이지 ✅

- **Task 012: 로그인 연동 마이페이지** ✅ - 완료
  - ✅ 로그아웃, 로그인 계정 표시(닉네임/provider) — 기존 기능
  - ✅ 크레딧 잔액, 상점(패키지 목록), 결제내역 UI 추가 — Task 008에서 만든 `BillingController` API를 처음으로 소비
  - ✅ 상용화 전 임시 관리자 우회 로그인(`/api/auth/dev-admin-login`) — `ADMIN_BYPASS_ENABLED` 플래그 + `?key=` 쿼리파라미터가 `ADMIN_BYPASS_SECRET`과 일치해야만 동작(상수시간 비교, fail-closed). 실제 OAuth 앱 등록 없이 팀이 관리자 권한으로 써볼 수 있게 함. URL은 프론트에 노출하지 않고 팀이 직접 북마크해서 사용
  - ✅ "내 사주" localStorage 히스토리와 서버 저장 기록 연동 — 보관 정책(2026-09-02 결정: 계정 삭제 시 `ON DELETE CASCADE`로 기록도 삭제, 별도 보관기간 없음, 비로그인 요청은 계속 식별자 없이 저장) 확정 후 `reading_record.user_account_id` 추가. 전체 결과(`result_json`)도 함께 저장해 다시 열어봤을 때 로컬 기록과 동일하게 재현됨. 마이그레이션 없이 서버 기록과 기기 로컬 기록을 병행 표시
  - 부수 발견 + 수정: permitAll 경로에서 발생한 에러가 컨테이너 `/error` 포워딩을 타면서 Spring Security가 401로 덮어쓰던 버그(`GET /api/saju/breakup`이 405 대신 401 반환하던 원인) — `/error`를 permitAll에 추가해 해결
  - 보안 발견 + 조치(2026-09-02): 프로덕션 JWT 서명 키가 공개 저장소의 하드코딩된 기본값으로 방치돼 있던 것을 발견해 즉시 로테이션

### Phase 6: 과금 구조 (백엔드 완료, 프론트는 마이페이지에 일부 반영)

- **Task 008: LLM 사주 상담 + 질문 횟수 기반 과금** - 백엔드 완료(2026-09-01), 프론트 일부 반영(2026-09-02)
  - ✅ 고정 크레딧 패키지 4종(10/20/50/110회) 스키마 + 시드 데이터, `credit_transaction` 원장(모든 증감 감사 가능)
  - ✅ `CreditService` — 원자적 SQL(`UPDATE ... WHERE balance >= ?`)로 동시 요청 이중 차감 방지, consume/grantFree/purchase/refund/adminAdjust
  - ✅ 관리자 API(`/api/admin/**`, `ROLE_ADMIN`) — 전체 결제 내역 조회, 사용자별 원장 조회, 환불, 수동 크레딧 지급/회수 (2026-09-01 관리자 요구사항 대응)
  - ✅ 마이페이지에 크레딧 잔액/상점/결제내역 UI 연결 — PG 연동 전이라 결제는 PENDING만 생성됨(Task 012에서 완료)
  - ✅ 관리자 전용 화면(`/admin`) — 전체 결제 내역, 사용자별 크레딧 원장 조회, 환불, 수동 크레딧 지급/회수. `/api/auth/me`에 `isAdmin` 추가해 마이페이지에 관리자 계정만 진입 링크 노출(실제 인가는 백엔드 `ROLE_ADMIN`이 강제)
  - ✅ LLM 사주 상담 기능(2026-09-02) — 사주 결과 화면에서 캐릭터(다숨/설레)에게 이어서 물어보는 채팅. 질문 1건 = 크레딧 1개, 세션 단위 연속 대화(이전 메시지를 LLM에 같이 넣음). OpenAI 연동, `consultation_session`/`consultation_message` 테이블(V6, 관리자 감사용으로 대화 내용 실제 저장 — 계정 삭제 시 CASCADE). LLM 호출 실패 시 소비한 크레딧 자동 환급
  - 남음: **실제 `OPENAI_API_KEY` 등록**(현재 미설정 — 미설정 상태에선 502로 안전하게 실패하고 크레딧 자동 환급되는 것까지만 확인함, 실제 응답 품질은 키 등록 후 사용자가 직접 확인 필요), PG사 실제 연동. 환불 버튼은 완료 상태 결제가 아직 없어(PG 미연동) 브라우저 클릭까지는 검증 못 함(백엔드 로직은 `CreditServiceTest`로 커버)
  - 캐릭터 일러스트/아트 제작(Task 003 이관분)도 이 시점에 프리미엄 상품 톤에 맞춰 함께 진행 검토

### Phase 7: 화면 개선 (2차)

- **Task 013: 종합 UI/UX 리뉴얼** - 위 기능들이 붙은 뒤 전체 화면 재정비
  - 로그인/마이페이지/과금 화면까지 포함한 일관된 디자인 패스
