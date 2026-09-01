---
description: '작업 브랜치를 master 기준으로 최신화하고 충돌을 해결합니다'
allowed-tools:
  [
    'Bash(git merge:*)',
    'Bash(git status:*)',
    'Bash(git diff:*)',
    'Bash(git log:*)',
    'Bash(git branch:*)',
    'Bash(git fetch:*)',
    'Bash(git stash:*)',
  ]
---

# Claude 명령어: Merge

**주의**: 이 저장소는 `master`를 PR + squash merge로만 병합한다
([CONTRIBUTING.md](../../CONTRIBUTING.md) 3장, 브랜치 보호 설정으로 merge commit/rebase가 꺼져 있음).
이 명령어로 **`master`에 직접 merge하지 않는다.** 용도는 반대 방향 —
작업 브랜치를 최신 `master`로 따라잡는 것이다.

## 사용법

```
/merge              # 현재 작업 브랜치에 master를 merge (최신화)
/merge --abort      # 진행 중인 merge 중단
```

## 프로세스

1. **사전 점검**
   - uncommitted 변경사항 확인, 있으면 stash 또는 커밋 유도
   - `git fetch`로 원격 `master` 최신화
2. **머지 실행**
   - 현재 브랜치에 `origin/master`를 merge
   - 충돌 없으면 그대로 완료
3. **충돌 해결**
   - 충돌 파일 목록화, 파일별로 내용 비교 제시
   - 해결 후 `./gradlew build` (backend) / `npm run build` (frontend)로 빌드 확인
   - 확인되면 merge 커밋 생성

## `master`로의 최종 반영

- 기능 브랜치가 준비되면 **PR을 열고, 리뷰 승인 + CI 통과 후 GitHub에서 Squash merge**한다.
- 로컬에서 `git merge feature-branch` 형태로 `master`에 직접 합치지 않는다 — 브랜치 보호 설정과
  히스토리 정책(선형·squash) 때문이다. `/pr` 명령어를 사용할 것.

## 충돌 해결 가이드

| 유형 | 해결 옵션 |
| --- | --- |
| 내용 충돌 | `ours`(현재 브랜치 유지) / `theirs`(master 내용 채택) / 수동 편집 |
| 삭제/수정 충돌 | 삭제 유지 여부, 수정 내용 채택 여부 판단 |

## 안전 기능

- merge 전 uncommitted 변경사항은 stash로 백업 제안
- `git merge --abort`로 언제든 중단 가능
- 충돌 해결 후 반드시 빌드/테스트로 검증
