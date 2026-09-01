---
description: 'CONTRIBUTING.md 규칙에 맞춰 GitHub Pull Request를 생성합니다'
allowed-tools:
  [
    'Bash(gh pr:*)',
    'Bash(gh api:*)',
    'Bash(gh repo:*)',
    'Bash(git push:*)',
    'Bash(git status:*)',
    'Bash(git log:*)',
    'Bash(git diff:*)',
    'Bash(git branch:*)',
    'Bash(git fetch:*)',
  ]
---

# Claude 명령어: Pull Request

[CONTRIBUTING.md](../../CONTRIBUTING.md) 3장 규칙으로 PR을 생성합니다.
`master`는 PR로만 병합되고 **Squash merge만 허용**됩니다.

## 사용법

```
/pr                    # 현재 브랜치로 PR 생성 (대화형)
/pr "제목"              # 제목 지정하여 PR 생성
/pr --draft            # Draft PR 생성
```

## 프로세스

### 생성 전 점검

1. 현재 브랜치가 `master`가 아닌지, 커밋이 있는지 확인
2. uncommitted 변경사항 확인 (있으면 `/commit` 먼저 유도)
3. 원격에 push 안 됐으면 `git push -u origin <브랜치>`

### PR 제목

커밋 컨벤션과 동일하게 **Conventional Commits, 한국어 subject, 이모지 없이**:

```
<type>(<scope>): <subject>
```

브랜치명에서 type을 유추하되, 커밋 히스토리와 실제 변경 내용이 우선한다.

```
feat/12-couple-compatibility-score → feat(saju): 오행 궁합 점수 산출 기능 추가
fix/18-leap-month-validation       → fix(api): 존재하지 않는 음력 날짜 거부
```

### PR 본문

**`.github/PULL_REQUEST_TEMPLATE.md`를 그대로 채운다** — 별도 템플릿을 새로 만들지 않는다.
특히 다음 섹션은 빈칸으로 두지 않는다:

- 보안·개인정보 체크리스트 (API 키/DB 접속 정보 미포함, 로그에 생년월일시 등 미노출)
- 명리 계산을 바꾼 경우: 입력→기대 팔자 테스트, 경계값(자시/절기/윤달/윤년) 확인
- 인프라/DB 변경 시 롤백 방법

### PR 크기

- 변경 **400줄 이내** 목표. 넘으면 나눌 수 있는지 먼저 검토.
- 생성 파일(lock 파일 등)이 큰 비중이면 본문에 명시.

## 리뷰 · 병합

- 리뷰어 1명 이상 승인 + CI(`backend`/`frontend`/`secret-scan`) 통과가 머지 조건.
- 리뷰 코멘트는 `nit:`(사소함) / `question:` / `blocking:` 접두어로 강도 표시.
- 승인·CI 통과 후 **Squash merge**로만 병합 (병합 커밋/리베이스 사용 안 함).
- 머지 후 브랜치는 삭제한다 (저장소 설정에 자동 삭제 켜져 있음).

## 참고

- 리뷰어 자동 배정: 이 저장소엔 `CODEOWNERS`가 없으므로 자동 할당되지 않는다. 필요하면 PR 생성 시 `--reviewer`로 직접 지정.
- Jira/Linear 등 외부 이슈 트래커 연동 없음 — 관련 작업은 GitHub Issue(`Refs #`, `Closes #`)로만 연결한다.
- 보안 취약점 관련 PR/이슈는 공개하지 않는다 — 저장소 관리자에게 직접 알린다.

## 사용 예시

```
/pr
# 대화형으로 제목/본문 채워 PR 생성

/pr --draft
# 아직 리뷰 준비 안 됐을 때 Draft로 생성
```

## 문제 해결

- **GitHub CLI 인증 오류**: `gh auth login` 안내
- **push 거부**: 원격 브랜치 최신화(`git fetch` 후 `git merge`로 master 반영, `/merge` 참고) 후 재시도
- **중복 PR**: `gh pr list --head <브랜치>`로 기존 PR 확인
