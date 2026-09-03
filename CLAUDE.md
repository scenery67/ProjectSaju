# CLAUDE.md

Claude(Claude Code / Cowork)가 이 저장소에서 작업할 때 따르는 규칙.
사람이 읽는 기여 가이드는 [CONTRIBUTING.md](CONTRIBUTING.md), 제품 설명은 [README.md](README.md).

---

## 1. 프로젝트

모바일 우선 **사주 서비스**. 모노레포.

| 영역 | 스택 |
| --- | --- |
| frontend/ | React 19 + TypeScript + Vite + Tailwind v4 + React Router 7 (lint: oxlint) |
| backend/ | Spring Boot 4.1 (Java 21) + Spring Data JPA + validation, `cn.6tail:lunar` (만세력) |
| DB | PostgreSQL (로컬 Docker Compose / 운영 Neon) |
| 배포 | 프론트 GitHub Pages, 백엔드 Fly.io, GitHub Actions |
| 기본 브랜치 | `master` |

주요 API: `POST /api/saju/breakup`, `POST /api/saju/couple-compatibility`

## 2. 표준 명령

```bash
# backend (JDK21은 tools/jdk-21 사용 — 시스템 PATH 건드리지 말 것)
./scripts/run-backend.sh          # bootRun
cd backend && ./gradlew build     # 컴파일 + 테스트
cd backend && ./gradlew test

# frontend
cd frontend && npm ci
npm run dev
npm run lint                      # oxlint
npm run build                     # tsc -b + vite build (타입체크 겸용)

# DB
docker compose up -d
```

명령이 실패하면 우회하지 말고 실패 원인을 그대로 보고할 것.

---

## 3. 코드 규칙

### 3.1 주석 — 영어 + 한글 병기

공개 API·비즈니스 로직(특히 명리 계산)에는 두 언어를 함께 적는다. 명리 용어는 한자/한글을 같이 남긴다.

```java
/**
 * Builds the four pillars (year/month/day/hour) from a birth moment.
 * 출생 시각으로 사주팔자(년주/월주/일주/시주)를 구성한다.
 *
 * @param birthAt birth date-time in KST / 출생 일시(KST)
 * @param lunar   true if the input date is lunar / 입력이 음력이면 true
 */
```

```typescript
// Keep the form state in KST; the API expects a local date-time, not UTC.
// 폼 상태는 KST 기준으로 유지한다. API는 UTC가 아닌 로컬 일시를 받는다.
```

자명한 코드에는 주석을 달지 않는다. **왜(why)** 를 적는다. TODO는 `// TODO(kg.park): ...`.

### 3.2 개인정보 — 이 프로젝트의 최대 리스크

생년월일·출생시각·성별은 조합 시 **개인을 식별할 수 있는 민감 정보**다. `reading_record`에 저장되므로:

- 로그에 생년월일시·이름·IP를 그대로 남기지 않는다. 필요하면 마스킹(`1990-**-** **:**`) 또는 해시.
- 응답 본문·에러 메시지에 입력값을 그대로 되돌려주지 않는다.
- 저장은 서비스에 실제 필요한 필드만. 이메일·전화번호는 여전히 **수집하지 않는다**.
- OAuth 로그인(카카오/구글/네이버) 도입 후에도 `user_account`엔 provider+provider_user_id+닉네임만 저장한다 — 이메일 등 추가 프로필 정보는 요청/저장하지 않는다(2026-09-01). 로그인은 아직 기능 검증 단계이고 `reading_record`와 연결하지 않았다.
- 보관 기간 정책을 정하기 전까지 `reading_record`에 식별자(쿠키 ID, 세션 ID, `user_account.id` 등)를 추가하지 않는다.
- 해외(EU) 트래픽을 받게 되면 GDPR 관점(수집 최소화·보관 기간·삭제 요청·동의)을 먼저 검토한다.
- 테스트/샘플 데이터는 실제 사람의 생년월일을 쓰지 않는다.

### 3.3 시크릿 — 절대 커밋 금지

- API 키, 토큰, 비밀번호, DB 접속 정보, 내부 IP/도메인을 코드·주석·이슈·PR에 넣지 않는다.
- 설정은 환경변수로만: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `VITE_API_BASE_URL`.
- 운영 시크릿은 Fly secrets / GitHub Secrets에 둔다. `.env.local`은 커밋하지 않는다(`.gitignore` 확인).
- `docker-compose.yml`의 `saju/saju`는 **로컬 전용**. 운영 기본값으로 승격하지 않는다.

```java
// NOT OK
private static final String DB_PASSWORD = "saju";
// OK
@Value("${spring.datasource.password}") private String dbPassword; // env 주입
```

### 3.4 취약점 체크

| 항목 | 금지 | 대안 |
| --- | --- | --- |
| SQL Injection | JPQL/네이티브 쿼리 문자열 연결 | 파라미터 바인딩(`:param`), Spring Data 메서드 |
| XSS | `dangerouslySetInnerHTML` | React 기본 이스케이프, 불가피하면 DOMPurify |
| CORS | `allowedOrigins("*")` + credentials | Pages 도메인만 화이트리스트 |
| 하드코딩 시크릿 | 상수 문자열 | 환경변수 |
| 과다 노출 | 엔티티를 그대로 응답 | 응답 DTO/record 분리 |
| 스택트레이스 노출 | 예외 그대로 반환 | `@RestControllerAdvice`로 일반화된 에러 응답 |

### 3.5 입력 검증 (필수)

모든 외부 입력은 컨트롤러에서 Bean Validation으로 검증하고, 도메인 경계에서 한 번 더 확인한다.

```java
public record BreakupSajuRequest(
    @NotNull @Past LocalDate birthDate,          // 생년월일
    @NotNull LocalTime birthTime,                // 출생 시각 (모름이면 별도 플래그)
    @NotNull CalendarType calendarType,          // SOLAR / LUNAR (양력/음력)
    boolean leapMonth,                           // 윤달 여부
    @NotNull Gender gender
) {}
```

- 프론트 검증은 UX용일 뿐, **백엔드 검증이 진짜 방어선**이다.
- 미래 날짜, 1900년 이전, 존재하지 않는 음력 날짜(윤달 포함)를 명시적으로 거른다.

### 3.6 도메인(명리) 규칙

- 입력 일시는 **KST 기준 로컬 일시**로 다루고, 저장 시각(created_at 등)은 UTC(`Instant`)로 저장한다. 둘을 섞지 않는다.
- 절기 경계·자시(23:00~01:00) 처리 방식은 한 곳(도메인 서비스)에 모으고, 규칙을 바꾸면 주석에 근거를 남긴다.
- `cn.6tail:lunar`의 절기 시각은 중국 기준이며 한국 경도로 보정되지 않음 — 경계 부근 케이스는 테스트로 고정해 둔다(README의 "알려진 한계" 참조).
- 해석 텍스트는 코드에 하드코딩하지 말고 데이터/템플릿으로 분리한다(`frontend/src/data/`, 백엔드 템플릿).
- 결과는 **오락/참고용**이다. 의료·법률·재무 판단으로 읽힐 문구(질병 예측, 투자 권유 등)를 생성하지 않는다.

### 3.7 테스트

- 버그 수정은 재현 테스트 먼저.
- 명리 계산(사주 구성, 오행 집계, 궁합 점수)은 **입력→기대 팔자**를 고정한 단위 테스트 필수. 경계값: 자시, 절기 경계일, 윤달, 윤년.
- 백엔드: JUnit 5 + Spring Boot test starters. 컨트롤러는 slice 테스트로 검증 로직까지 확인.
- 프론트: 현재 테스트 러너 미도입. 도입 시 Vitest + React Testing Library로 하고, 그 전까지는 `npm run build`(tsc)로 타입 안전성만 보장.

---

## 4. Claude 작업 방식

- 수정 전에 관련 파일을 먼저 읽고 기존 패턴을 따른다. 새 의존성 추가는 먼저 제안하고 승인받는다.
- 요청받지 않은 리팩터링·파일 생성(README 등)을 하지 않는다.
- `git push --force`, 히스토리 재작성, 브랜치 삭제 금지.
- 불확실하면 추측하지 말고 불확실하다고 밝힌다. 답변이 길면 핵심 요약을 먼저 쓴다.
- 생성한 코드를 그대로 배포하지 않는다. 보안 관련 결정은 사람 검토를 거친다.

## 5. 커밋 / PR

Conventional Commits. 제목·본문 모두 한국어. 상세는 [CONTRIBUTING.md](CONTRIBUTING.md).

```
feat(saju): 윤달 음력 생년월일 검증 추가

윤달 입력 시 존재하지 않는 날짜가 통과되던 문제를 막기 위해
LunarDateValidator를 추가.

Refs: #12
```

## 6. 건드리지 말 것

- `tools/`, `backend/build/`, `backend/.gradle/`, `frontend/node_modules/`, `frontend/dist/`
- `frontend/.env.local`, 그 밖의 `.env*`, 키 파일(`*.jks`, `*.p12`)
- `gradlew`, `gradle/wrapper/` (버전 변경 요청이 있을 때만)
