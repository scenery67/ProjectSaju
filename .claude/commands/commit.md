---
description: 'CONTRIBUTING.md의 Conventional Commits 규칙으로 커밋을 생성합니다'
allowed-tools:
  [
    'Bash(git add:*)',
    'Bash(git status:*)',
    'Bash(git commit:*)',
    'Bash(git diff:*)',
    'Bash(git log:*)',
  ]
---

# Claude 명령어: Commit

이 저장소의 커밋 컨벤션([CONTRIBUTING.md](../../CONTRIBUTING.md) 2장)에 맞춰 커밋을 생성합니다.
**이모지를 붙이지 않습니다.**

## 사용법

```
/commit
```

## 프로세스

1. 스테이지된 파일 확인 — 있으면 그 파일만 커밋 대상
2. 스테이지된 게 없으면 `git status`/`git diff`로 변경사항 분석 후 논리적 단위로 스테이징 제안
3. 여러 관심사가 섞여 있으면 분할 제안 (포맷팅 vs 로직, 파일 패턴, 타입 혼재 등)
4. 아래 포맷으로 커밋 메시지 작성 후 커밋

## 커밋 포맷

```
<type>(<scope>): <subject>

<body>

<footer>
```

- **subject**: 영어, 명령형 현재시제, 50자 이내, 마침표 없음 (`add`, `fix` — `added`, `fixes` 아님)
- **body**: 한국어로 *왜* 바꿨는지. 72자에서 줄바꿈. 로직 변경에는 권장, 사소한 변경은 생략 가능
- **footer**: `Refs: #12`, `Closes: #12`, 호환성이 깨지면 `BREAKING CHANGE: ...`

### type

`feat` · `fix` · `refactor` · `perf` · `test` · `docs` · `build` · `ci` · `chore` · `revert`

### scope (이 저장소 기준)

`saju`(명리 계산) · `api` · `web` · `db` · `infra` · `deps` · `ci`

## 예시

```
feat(saju): add five-element distribution scoring

오행 분포를 0~100 점수로 환산해 궁합 결과에 노출한다.
기존 텍스트 템플릿 분기는 그대로 두고 점수만 추가.

Refs: #12
```

```
fix(api): reject lunar dates that do not exist

윤달이 없는 달에 leapMonth=true가 들어오면 500이 나던 문제.
LunarDateValidator에서 400으로 거른다.

Closes: #18
```

```
chore(deps): bump vite to 8.2.2
```

## 커밋 위생

- 한 커밋 = 한 가지 변경. 포맷팅과 로직 변경을 섞지 않는다.
- WIP 커밋은 머지 전 정리(squash)한다.
- **시크릿·개인정보가 담긴 파일은 절대 커밋하지 않는다.** 의심되면 커밋 전에 diff 내용을 다시 확인.
- **커밋에 Claude 서명을 추가하지 않는다.**
