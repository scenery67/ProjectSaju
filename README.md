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
  — 입력값 검증 후 DB(`reading_record` 테이블)에 기록, **실제 사주 계산 로직은 아직 미구현(TODO)**
- 반응형: `#root` 최대폭 480px로 모바일 뷰를 기준으로 하고, 큰 화면에서는 중앙 정렬됨

## TODO / 다음 단계

- [ ] 실제 사주(사주팔자) 계산 로직 구현 (`SajuReadingService`)
- [ ] 캐릭터 디자인/네이밍 확정 후 `frontend/src/data/personas.ts` 교체
- [ ] 배포 대상 클라우드 결정 (백엔드+DB 통합 호스팅) 및 CI/CD 구성
- [ ] 인증/결제(프리미엄 상품) 여부 결정
- [ ] Flyway/Liquibase 도입 (현재 `ddl-auto: update`는 로컬 편의용)
