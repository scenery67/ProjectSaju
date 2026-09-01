# 사주 서비스 (초기 스캐폴드)

모바일 우선(mobile-first) 사주 서비스. 초기 페르소나 2종: **이별사주**, **연인 궁합 사주**.
캐릭터명/일러스트 등은 참고 사이트(foxbunny.io/saju)와 겹치지 않도록 추후 자체 제작 예정 — 현재는 placeholder.

## 기술 스택

- Frontend: React + TypeScript + Vite + Tailwind CSS v4, React Router
- Backend: Spring Boot 4 (Java 21), Spring Data JPA
- DB: PostgreSQL (로컬은 Docker Compose로 실행)
- 저장소: GitHub (프론트/백엔드 모노레포). 배포용 클라우드는 추후 결정.

## 폴더 구조

```
sj/
├── backend/     # Spring Boot 프로젝트 (Gradle, Java 21 toolchain)
├── frontend/    # React + Vite 프로젝트
├── scripts/     # 로컬 개발용 스크립트 (JDK21 설치/실행)
├── tools/       # 프로젝트 전용 JDK21 (git 미포함, 각자 로컬에 생성)
└── docker-compose.yml  # 로컬 Postgres
```

## 로컬 개발 환경 준비

### 1) 이 프로젝트 전용 Java 21 설치

시스템에 Java 8이 이미 설치되어 있어도 상관없습니다. 아래 스크립트는 `tools/jdk-21`에만
JDK21을 설치하며, 시스템 PATH/JAVA_HOME은 전혀 건드리지 않습니다.

```powershell
# PowerShell
.\scripts\setup-jdk21.ps1
```

```bash
# Git Bash
./scripts/setup-jdk21.sh
```

### 2) 로컬 Postgres 실행 (Docker 필요)

```bash
docker compose up -d
```

기본 접속 정보(로컬 전용, 운영 환경에는 그대로 쓰지 말 것): db=`saju`, user=`saju`, password=`saju`.
운영/클라우드 배포 시에는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 반드시 덮어써야 합니다.

### 3) 백엔드 실행

`run-backend` 스크립트가 위에서 설치한 JDK21을 이 프로세스에만 한정해서 사용하고,
`./gradlew bootRun`을 실행합니다.

```powershell
.\scripts\run-backend.ps1
```

```bash
./scripts/run-backend.sh
```

기본 포트: `http://localhost:8080` (API prefix: `/api`)

### 4) 프론트엔드 실행

```bash
cd frontend
npm install
cp .env.local.example .env.local   # 필요 시 API 주소 수정
npm run dev
```

기본 포트: `http://localhost:5173`

## 현재 구현 범위

- 홈: 페르소나 2종(이별사주 / 연인 궁합 사주) 카드 그리드 + 하단 탭바
- 각 페르소나: 생년월일/시간/양음력/성별 입력 폼 → 결과 화면
- 백엔드: `POST /api/saju/breakup`, `POST /api/saju/couple-compatibility`
  — 입력값 검증 후 `cn.6tail:lunar` 라이브러리로 사주팔자(년/월/일/시주, 오행, 일간, 십성, 대운 8개 주기) 계산, DB(`reading_record` 테이블)에 기록
  — 궁합은 오행 상생상극 + 일지(배우자궁) 육합/충 + 상호 십성("상대는 나에게 OO")까지 반영
  — 결과는 원본 데이터(간지/오행 카운트 등)를 그대로 노출하지 않고, 오행/십성 성향 설명(`FiveElementTraits`/`TenGodTraits`)과 생년월일 기준 현재 대운을 계산해 평문으로 풀어 설명
  — 기본 성격·연애·직업·재물·대인관계 5개 항목(`PersonalityProfile`)을 십성 5대 분류로 계산, 지장간·12운성·세운(현재 대운 10년)까지 노출 — 프론트에 참고 사이트 스타일 4주 표 + 오행 분포 막대 그래프로 표시
  — 12신살/도화살 등 개별 신살, 신강/신약 지수, 용신, 형(刑)/파(破)/해(害)는 아직 미구현 (TODO)
- "내 사주" 탭: 로그인 없이 이 기기 `localStorage`에만 최근 결과 20건 보관 (서버 미저장)
- "마이페이지" 탭: 카카오/구글/네이버 OAuth2 로그인 **동작만 검증된 상태** — 백엔드가 로그인 성공 시 자체 JWT를 발급해 프론트로 넘기고(`user_account` 테이블), 프론트는 `localStorage`에 토큰을 보관한다. 아직 다른 기능(사주 기록 등)과는 연결하지 않았고 실사용에도 안 열어뒀다 — 카카오/구글/네이버 개발자 콘솔에 실제 앱 등록 전이라 `client-id`가 `placeholder`임
- 반응형: `#root` 최대폭 480px로 모바일 뷰를 기준으로 하고, 큰 화면에서는 중앙 정렬됨

## 배포 / 인프라

- 백엔드: Fly.io 실배포 완료 (`dasum-saju-api`, https://dasum-saju-api.fly.dev) — Neon PostgreSQL 연결 확인됨
- 프론트: GitHub Pages (`frontend/dist` 정적 배포, `HashRouter` + `base: /ProjectSaju/`) — 저장소 Settings → Pages → Source를 **GitHub Actions**로 한 번 켜야 함 (최초 1회, 수동)
- DB: Neon PostgreSQL (서버리스, 무료 티어) — 접속 정보는 Fly secrets로 주입 (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- CI/CD: GitHub Actions (`.github/workflows/deploy-backend.yml`, `deploy-frontend.yml`) — 각각 `backend/`, `frontend/` 경로 변경 시에만 트리거
- 필요한 GitHub Secrets/Variables: `FLY_API_TOKEN`(Secret), `VITE_API_BASE_URL`(Actions Variable, `https://dasum-saju-api.fly.dev/api`) — GitHub Pages 배포는 저장소 자체 권한(`GITHUB_TOKEN`)만 쓰므로 별도 계정/토큰 불필요
- DB 스키마는 Flyway가 관리 (`backend/src/main/resources/db/migration/`), `ddl-auto: validate`로 Hibernate는 검증만 함
- **주의**: 프로덕션 Fly 앱에는 아직 `JWT_SECRET`/`FRONTEND_URL`/`KAKAO_CLIENT_ID` 등 OAuth 관련 Fly secrets를 등록하지 않았음 — 로그인은 현재 로컬에서만 동작 검증됨(README "현재 구현 범위" 참고)

## TODO / 다음 단계

앞으로 진행 순서(2026-09-01 합의): **사주 내용 개선 → 로그인 → 마이페이지 → 과금 구조 → 화면 개선(2차)**. 상세는 [docs/ROADMAP.md](docs/ROADMAP.md) 참고.

- [ ] 형(刑)/파(破)/해(害), 12신살/개별 신살(도화살·역마살 등), 신강/신약, 용신 — 라이브러리 미지원 확인함, 직접 구현 시 출처 검증 부담 큼(특히 신강신약·용신은 유파마다 계산법이 갈림)
- [ ] 년/시 십성을 해석 문장에도 반영 (현재는 4주 표에만 노출)
- [ ] OAuth 소셜 로그인 실제 연동 — 카카오/구글/네이버 개발자 콘솔에 앱 등록 후 `KAKAO_CLIENT_ID`/`KAKAO_CLIENT_SECRET` 등 Fly secrets 등록, `JWT_SECRET`/`FRONTEND_URL`도 운영값으로 설정 필요
- [ ] 로그인을 실제 기능(내 사주 서버 저장 등)과 연결
- [ ] LLM 기반 사주 상담 기능 — 계산된 사주 데이터를 LLM에 입력해 상담 응답 생성, **질문 횟수 기준 과금** 예정 (2026-09-01 방향 확정, 과금 구조 설계 시 사용량 카운팅 스키마부터 고려)
- [ ] 로그인 연동 마이페이지 (현재는 로그인 없는 `localStorage` 기반 "내 사주"만 있음)
- [ ] 결제(프리미엄 상품) 도입 — 이 시점에 캐릭터 일러스트/아트 제작도 함께 검토
- [ ] 종합 UI/UX 리뉴얼(2차) — 로그인/마이페이지/과금까지 포함해서

### 알려진 한계
- `cn.6tail:lunar`의 절기(24節氣) 시각은 전통 동아시아(중국 기원) 만세력 기준이며 한국 경도 기준으로 재계산하지 않음 — 절기 경계 부근 출생자는 월주/년주가 미세하게 어긋날 수 있음
  - 실측(2024년 입춘 기준): 실제 한국 입춘은 17:27 KST인데 라이브러리는 16:27~16:28 사이에서 년주를 전환함 — 약 59분 이르게 절기가 바뀜 (`SajuChartCalculatorTest.yearPillarChangesAtSolarTermBoundaryNotLunarNewYear`로 고정)
  - 자시/야자시(23:00~01:00) 경계는 라이브러리 기본 유파(sect=2, 야자시=당일)를 그대로 사용 (`SajuChartCalculatorTest.lateZiHourKeepsCurrentDayPillarUnderDefaultSect`로 고정)
