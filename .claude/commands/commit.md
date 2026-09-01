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
4. CONTRIBUTING.md 2장 포맷으로 커밋 메시지 작성 후 커밋

## 커밋 포맷

포맷·type·scope·예시는 [CONTRIBUTING.md](../../CONTRIBUTING.md) 2장이 원본이다.
여기서 다시 나열하지 않는다 — 커밋 작성 전에 그 문서를 확인할 것.

## 커밋 위생

- 한 커밋 = 한 가지 변경. 포맷팅과 로직 변경을 섞지 않는다.
- WIP 커밋은 머지 전 정리(squash)한다.
- **시크릿·개인정보가 담긴 파일은 절대 커밋하지 않는다.** 의심되면 커밋 전에 diff 내용을 다시 확인.
- **커밋에 Claude 서명을 추가하지 않는다.**
