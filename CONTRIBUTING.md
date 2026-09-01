# 기여 가이드 (Contributing)

사주 서비스 모노레포의 브랜치·커밋·PR 규칙입니다.
Claude에게 적용되는 규칙은 [CLAUDE.md](CLAUDE.md)를 참고하세요.

---

## 1. 브랜치 전략 — Trunk-based (짧은 수명 브랜치)

소규모 팀 + 자동 배포 구조이므로 Git Flow 대신 **트렁크 기반**을 씁니다.

```
master ──●──●──────●──────●──   (항상 배포 가능 상태, 보호 브랜치)
          \        /      /
           feat/…─●      /      ← 1~3일 안에 머지
                  fix/…─●
```

- `master` : 기본 브랜치. 직접 push 금지, PR로만 병합. 머지 = 배포 트리거.
- 작업 브랜치는 `master`에서 만들고, **3일 이내**에 머지하는 크기로 자릅니다.
- `release/*`, `develop`은 쓰지 않습니다. 릴리스가 필요하면 태그(`v0.1.0`)로 표시합니다.
- 급한 수정도 같은 흐름(`fix/*` → PR). 리뷰를 건너뛸 상황이면 사후 리뷰를 이슈로 남깁니다.

### 브랜치 이름

```
<type>/<이슈번호>-<짧은-설명>
```

| type | 용도 | 예시 |
| --- | --- | --- |
| `feat` | 기능 추가 | `feat/12-couple-compatibility-score` |
| `fix` | 버그 수정 | `fix/18-leap-month-validation` |
| `refactor` | 동작 변화 없는 구조 개선 | `refactor/24-saju-service-split` |
| `docs` | 문서 | `docs/30-api-readme` |
| `chore` | 빌드/설정/의존성 | `chore/31-bump-spring-boot` |
| `test` | 테스트 추가·보강 | `test/33-solar-term-boundary` |

- 소문자 + 하이픈. 한글·공백 금지. 이슈가 없으면 번호는 생략 가능.

---

## 2. 커밋 컨벤션 — Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **subject**: 한국어, 명령형 현재시제, 50자 이내, 마침표 없음. (`추가`, `수정` — `추가함`, `수정했음` 아님)
- **body**: 한국어로 *왜* 바꿨는지. 72자에서 줄바꿈. 선택이지만 로직 변경에는 권장.
- **footer**: `Refs: #12`, `Closes: #12`, 호환성 깨짐이면 `BREAKING CHANGE: ...`.

### type

`feat` · `fix` · `refactor` · `perf` · `test` · `docs` · `build` · `ci` · `chore` · `revert`

### scope (이 저장소 기준)

`saju`(명리 계산) · `api` · `web` · `db` · `infra` · `deps` · `ci`

### 예시

```
feat(saju): 오행 분포 점수 산출 로직 추가

오행 분포를 0~100 점수로 환산해 궁합 결과에 노출한다.
기존 텍스트 템플릿 분기는 그대로 두고 점수만 추가.

Refs: #12
```

```
fix(api): 존재하지 않는 음력 날짜 거부

윤달이 없는 달에 leapMonth=true가 들어오면 500이 나던 문제.
LunarDateValidator에서 400으로 거른다.

Closes: #18
```

```
chore(deps): vite 8.2.2로 업데이트
```

### 커밋 위생

- 한 커밋 = 한 가지 변경. 포맷팅과 로직 변경을 섞지 않습니다.
- WIP 커밋은 머지 전에 정리(squash)합니다.
- **시크릿·개인정보가 담긴 파일은 절대 커밋하지 않습니다.** 실수로 올렸다면 되돌리는 것으로 끝내지 말고 해당 키를 즉시 폐기·재발급하세요.

---

## 3. Pull Request

1. 이슈를 먼저 만들거나(권장) PR 본문에 배경을 적습니다.
2. `master`에서 브랜치를 만들고 작업합니다.
3. 로컬에서 통과시킨 뒤 PR을 엽니다.

   ```bash
   cd backend && ./gradlew build
   cd frontend && npm run lint && npm run build
   ```

4. [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)의 체크리스트를 채웁니다. 특히 **개인정보·시크릿** 항목.
5. 리뷰어 1명 이상 승인 + CI 통과 후 **Squash merge**.
6. 머지 후 브랜치는 삭제합니다.

### PR 크기

- 변경 400줄 이내를 목표로 합니다. 넘으면 나눌 수 있는지 먼저 검토하세요.
- 생성 파일(lock 파일 등)이 큰 비중이면 PR 본문에 명시합니다.

### 리뷰 기준

리뷰어는 아래를 확인합니다.

- 외부 입력에 검증이 있는가
- 로그·응답에 생년월일시 등 개인정보가 새지 않는가
- 시크릿·접속 정보가 하드코딩되지 않았는가
- 명리 계산 변경에 테스트가 붙었는가 (경계값 포함)
- 주석이 영어+한글로 달렸는가
- 되돌리기 쉬운가 (마이그레이션·설정 변경 시)

리뷰 코멘트는 `nit:`(사소함) / `question:` / `blocking:` 접두어로 강도를 표시합니다.

---

## 4. CI / 브랜치 보호

### 워크플로

| 파일 | 트리거 | 내용 |
| --- | --- | --- |
| `.github/workflows/ci.yml` | PR, `master` push | 백엔드 `gradlew build`, 프론트 `lint` + `build`, gitleaks 시크릿 스캔 |
| `.github/workflows/deploy-backend.yml` | `backend/**` push | Fly.io 배포 |
| `.github/workflows/deploy-frontend.yml` | `frontend/**` push | GitHub Pages 배포 |

### 브랜치 보호 설정 (Settings → Branches → `master`)

- [ ] Require a pull request before merging (승인 1명 이상)
- [ ] Require status checks to pass: `backend`, `frontend`, `secret-scan`
- [ ] Require branches to be up to date before merging
- [ ] Require conversation resolution before merging
- [ ] Do not allow bypassing the above settings
- [ ] Allow squash merging만 켜기 (merge commit / rebase 끄기)
- [ ] Automatically delete head branches (Settings → General)

### 필요한 Secrets / Variables

| 이름 | 종류 | 용도 |
| --- | --- | --- |
| `FLY_API_TOKEN` | Secret | Fly.io 배포 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Fly secret | 운영 DB 접속 |
| `VITE_API_BASE_URL` | Actions **Variable** | 프론트 빌드 시 API 주소 (시크릿 아님) |

---

## 5. 로컬 개발 환경

설치·실행 절차는 [README.md](README.md)를 따릅니다. 요약:

```bash
./scripts/setup-jdk21.sh     # 프로젝트 전용 JDK21 (tools/jdk-21)
docker compose up -d         # 로컬 Postgres
./scripts/run-backend.sh     # :8080
cd frontend && npm ci && npm run dev   # :5173
```

`tools/`는 커밋하지 않습니다. 각자 로컬에서 스크립트로 받습니다.

---

## 6. 이슈

- 버그: [bug report 템플릿](.github/ISSUE_TEMPLATE/bug_report.yml) — 재현 절차와 입력값(가공한 값)을 적습니다.
- 기능: [feature request 템플릿](.github/ISSUE_TEMPLATE/feature_request.yml)
- **보안 취약점은 공개 이슈로 올리지 마세요.** 저장소 관리자에게 직접 알립니다.
