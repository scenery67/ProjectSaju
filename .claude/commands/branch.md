---
description: '브랜치 생성, 전환, 삭제 등 브랜치 관리 작업을 수행합니다'
allowed-tools:
  [
    'Bash(git branch:*)',
    'Bash(git checkout:*)',
    'Bash(git switch:*)',
    'Bash(git status:*)',
    'Bash(git stash:*)',
    'Bash(git log:*)',
    'Bash(git fetch:*)',
  ]
---

# Claude 명령어: Branch

이 저장소의 트렁크 기반 전략(`master` 하나만 보호 브랜치, `develop`/`release/*` 없음)에 맞춘
브랜치 관리 도구입니다. 규칙 원문은 [CONTRIBUTING.md](../../CONTRIBUTING.md) 1장.

## 사용법

```
/branch [type]/[이슈번호]-[짧은-설명]   # 새 브랜치 생성 및 전환
/branch                               # 대화형 브랜치 관리 메뉴
```

## 브랜치 네이밍 규칙

```
<type>/<이슈번호>-<짧은-설명>
```

| type | 용도 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 (급한 수정도 별도 hotfix 없이 이 흐름 사용) |
| `refactor` | 동작 변화 없는 구조 개선 |
| `docs` | 문서 |
| `chore` | 빌드/설정/의존성 |
| `test` | 테스트 추가·보강 |

- 소문자 + 하이픈. 한글·공백 금지.
- 이슈가 없으면 번호는 생략 가능 (`fix/leap-month-validation`).

```
✅ feat/12-couple-compatibility-score
✅ fix/18-leap-month-validation
❌ feature/user-auth      # feat/ 가 맞음, feature/ 아님
❌ hotfix/security-patch  # hotfix 타입 없음 → fix/ 사용
❌ FEATURE/USER-AUTH      # 대문자 금지
```

## 프로세스

### 생성

1. 현재 Git 상태 확인 (uncommitted 변경사항 있으면 stash 또는 커밋 권장)
2. 브랜치명이 위 규칙을 따르는지 검증, 어긋나면 수정 제안
3. **`master`**에서 분기 (이 저장소엔 `develop`/`main` 없음)
4. 새 브랜치 생성 및 전환

### 전환

1. 현재 작업 상태 확인, 필요시 stash
2. 대상 브랜치로 전환

### 삭제

1. `master`에 병합됐는지 확인 (PR이 squash merge되면 로컬 브랜치는 그 커밋을 포함하지 않으므로 `git branch -d`가 아니라 병합된 PR 확인 후 `-D` 필요할 수 있음)
2. 원격 브랜치 존재 여부 확인 후 삭제

## 크기 가이드

- **3일 이내**에 머지 가능한 크기로 브랜치를 자른다 (CONTRIBUTING.md 1장).
- 오래 끄는 브랜치는 `master`를 자주 받아 충돌을 줄인다.

## 안전 기능

- 브랜치 전환 전 자동 stash 제안 (스택 메시지에 이전 브랜치명 포함)
- `master`로의 직접 커밋/push는 이 명령어가 만들지 않음 — 항상 작업 브랜치 생성
