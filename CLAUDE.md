# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew clean build

# Run (requires env vars below)
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "team23.q_check.club.service.ClubServiceTest"
```

**Required environment variables for local run:**
```bash
LOCAL_MYSQL_URL=jdbc:mysql://localhost:3306/qcheck
LOCAL_MYSQL_USER=<user>
LOCAL_MYSQL_PASSWORD=<password>
CORS_ALLOWED_ORIGINS=http://localhost:3000
DISCORD_CLIENT_ID=<id>
DISCORD_CLIENT_SECRET=<secret>
DISCORD_REDIRECT_URI=http://localhost:8080/api/auth/code
FRONTEND_AUTH_CALLBACK_URL=http://localhost:5173/auth/callback
JWT_SECRET=<at-least-32-chars>
```

Swagger UI is served at `/api/swagger-ui.html` (OpenAPI JSON: `/api/v3/api-docs`).

## Architecture

**Stack:** Spring Boot 3.4.2 · Spring Data JPA · MySQL · Flyway · Lombok · SpringDoc OpenAPI 2.8.5 · JJWT 0.12 · JUnit 5 + Mockito

**Domain modules** under `src/main/java/team23/q_check/`:
- `identity` — Discord OAuth2 인증, 사용자 정보·검색 (`/api/auth/**`, `/api/users/**`)
- `club` — 클럽 생성·멤버 관리·권한 위임·탈퇴/제거 (`/api/clubs/**`)
- `event` — 행사 CRUD·신청 폼·참가 신청/취소·QR/수동 출석·캘린더·사진 (`/api/events/**`, `/api/attendance/**`, `/api/calendar/**`)
- `settlement` — 정산 그룹 분배·상태 전이(UNPAID/PENDING/COMPLETED)·리마인드 (`/api/settlements/**`)
- `common` — auth resolver·JWT·전역 예외 처리·`ApiResponse<T>`·OpenAPI 설정

Each module follows the pattern: `controller/`, `dto/`, `domain/{model,repository,service}/`.

**Database:** Flyway SQL migrations in `src/main/resources/db/migration/`. `ddl-auto: validate` — schema changes require a new migration file. Key enums in DB: `ClubRole` (OWNER/ADMIN/MEMBER), `RegistrationStatus` (REGISTERED/CANCELED/CHECKED_IN), `FieldType` (TEXT/SELECT/BOOLEAN/NUMBER), `AttendanceMethod` (QR/AR/MANUAL), `SettlementItemStatus` (UNPAID/PENDING/COMPLETED).

## Auth

**Discord OAuth2 + JWT** 가 기본 인증 흐름. 헤더 기반 dev fallback 도 지원.

흐름:
1. `GET /api/auth/login` → Discord OAuth 인가 페이지로 redirect, state 를 세션 저장
2. Discord 콜백 → `GET /api/auth/code?code=...&state=...` → 백엔드가 처리 후 **프론트 콜백 URL** (`FRONTEND_AUTH_CALLBACK_URL`) 로 302 redirect
   - 기존 회원: `?token=<accessJwt>&isNewUser=false` + `refresh_token` httpOnly 쿠키
   - 신규 회원: `?token=<signupJwt>&isNewUser=true` (쿠키 없음, 10분 유효)
   - 에러: `?error=<코드>&message=<설명>`
3. `POST /api/auth/signup` (Authorization: Bearer signup-jwt) → 회원 생성 후 access JWT + refresh 쿠키
4. `POST /api/auth/refresh` (refresh_token 쿠키) → 새 access·refresh 쌍 (refresh 회전)

보호된 엔드포인트는 `JwtAuthInterceptor` 가 게이트하고, `@CurrentUserId` 가 컨트롤러 파라미터로 userId 를 주입한다.

- **Production**: `Authorization: Bearer <access-jwt>` 만 허용
- **Dev fallback**: `dev-auth.enabled=true` 일 때 `X-USER-ID: <Long>` 헤더 허용. 프로덕션에선 반드시 false.

JWT 타입 분리: access / refresh / signup 토큰은 클레임 `type` 으로 구분되며 `JwtService` 에서 잘못된 타입 추출 시 401.

## API Conventions

- 응답은 `ApiResponse<T>` (`success`, `code`, `message`, `data`, `timestamp`) 로 래핑.
- 에러는 `AppException(ErrorCode, message)` 으로 던지고 `GlobalExceptionHandler` 가 `ErrorCode` 의 HTTP status (400/401/403/404/409/500) 로 매핑.
- 입력 검증 예외 자동 매핑:
  - `MethodArgumentNotValidException` / `MethodArgumentTypeMismatchException` / `MissingServletRequestParameterException` / `HttpMessageNotReadableException` / `IllegalArgumentException` → 400
  - `MissingRequestHeaderException` → `Authorization` 헤더는 401, 그 외 헤더는 400
- 권한 검증은 서비스 레이어에서 `ClubAuthorizationService.requireMembership` / `requireAdminOrOwner` 로.
- 이메일·refresh token 등 민감 데이터는 응답 DTO 에 노출하지 않는다 (예: `UserSearchResultDto` 는 email 미포함).

## Logging

`logback-spring.xml` 에서 프로파일로 출력 포맷이 갈린다. `prod` = stdout JSON (Loki/Grafana 수집 전제), 그 외 = 사람이 읽기 쉬운 텍스트.

**MDC 키** — 모든 로그 라인에 자동으로 박힘:
- `requestId` — `RequestLoggingFilter` 가 요청 진입 시 발급. 클라이언트가 `X-Request-Id` 헤더로 보내면 그 값 사용, 아니면 UUID 생성. 응답 헤더에도 동일 값 내려감
- `userId` — `JwtAuthInterceptor` 가 인증 성공 시 주입 (JWT 또는 dev-auth fallback 모두)
- `clientIp` — `X-Forwarded-For` 우선, 없으면 `request.getRemoteAddr()`

**메시지 컨벤션** — `도메인.이벤트 key=value key=value` 형태. 한국어 자유서술 X, grep 효율 우선.

**현재 찍히는 로그** (도메인 이벤트 로깅은 미도입 — 인시던트 발생 시 해당 메서드에 추가):

| 위치 | 메시지 | 레벨 | 트리거 |
|---|---|---|---|
| `RequestLoggingFilter` | `http.access method= path= status= latencyMs=` | INFO | 모든 요청, 응답 직후 |
| `HttpExchangeLoggingFilter` (logger=`http.exchange`) | `http.exchange method= path= reqHeaders= reqBody= status= resHeaders= resBody=` | DEBUG | 모든 요청 (DEBUG 활성 시에만). 평소엔 꺼두고 인시던트 시 `POST /actuator/loggers/http.exchange {"configuredLevel":"DEBUG"}` 로 토글. 민감 헤더 마스킹·본문 4KB 캡·바이너리 사이즈만 표시 |
| `JwtAuthInterceptor` | `auth.dev_fallback used userId= path=` | WARN | dev-auth 헤더로 인증 성공 (프로덕션 오설정 감지용) |
| `JwtAuthInterceptor` | `auth.dev_fallback invalid value=` | WARN | `X-USER-ID` 가 숫자가 아님 |
| `JwtAuthInterceptor` | `auth.missing path=` | INFO | 인증 헤더 없음 |
| `JwtAuthInterceptor` | `auth.rejected reason= path=` | INFO | JWT 검증 실패 (만료/타입 불일치/서명 실패) |
| `GlobalExceptionHandler` | `app.exception code= msg=` | ERROR/WARN | `AppException` — 5xx ERROR + 스택, 4xx WARN |
| `GlobalExceptionHandler` | `request.invalid type= msg=` | INFO | 입력 검증 실패류 (`MethodArgumentNotValidException` 등) |
| `GlobalExceptionHandler` | `request.missing_header header= code=` | INFO | 필수 헤더 누락 |
| `GlobalExceptionHandler` | `access.denied msg=` | WARN | `AccessDeniedException` |
| `GlobalExceptionHandler` | `unhandled.exception type= msg=` | ERROR | catch-all (5xx) — 스택 포함 |
| `DiscordOAuthService` | `discord.token_exchange status= latencyMs= [body=]` | INFO/WARN | Discord 토큰 교환 (200=INFO, 4xx/network=WARN) |
| `DiscordOAuthService` | `discord.user_info status= latencyMs= [discordId=\|body=]` | INFO/WARN | Discord 사용자 정보 조회 |
| Hibernate | 슬로우 쿼리 | WARN | 1초 초과 (`spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=1000`) |
| Hikari | 커넥션 누수 | WARN | 5초 동안 미반환 (`spring.datasource.hikari.leak-detection-threshold=5000`) |

**민감 데이터 금지**: JWT 원문, refresh token, Discord client-secret, 비밀번호, 이메일은 절대 로그에 남기지 않음. 사용자 식별은 `userId`(Long) 만으로.

## Testing

- 컨트롤러 테스트: `MockMvcBuilders.standaloneSetup(controller)` — Spring context 미로드.
- 서비스/리포지토리는 Mockito 로 모킹.
- 엔티티 ID 가 필요한 fixture 는 reflection 으로 `id` 필드 주입 (각 테스트 파일 내 `setId(...)` private 메서드).
- 테스트 파일 패키지 구조는 main 미러링.

## Backlog

도메인 기능은 핵심 흐름이 모두 들어있고, 아래는 의도적으로 후속으로 미룬 항목들이다.

### Settlement
- 영수증 이미지 multipart 업로드 (현재 URL 문자열만)
- OCR 처리 (`Settlement.ocrData` 필드만 있고 미사용)
- 리마인드 실제 발송 (현재는 `last_reminded_at`/`remind_count` 갱신만)
- 정산 수정/삭제 API
- 사용자용 본인 분담금 조회 (현재 ADMIN+ 만 조회 가능)

### Event / Attendance
- EventPhoto multipart 업로드 (현재 URL 만)
- 사진 메타데이터 (caption, taken_at)
- 참가 취소 후 재신청 — `existsByEvent_IdAndUser_Id` 가 status 무관해서 막힘. status 필터 추가 필요
- AR 체크인 흐름 (`AttendanceMethod.AR` 사용 안 됨)
- 체크인 취소 (실수 처리)
- `attendance_logs.note` 컬럼 추가로 수동 체크인 사유 기록

### 인프라 / 외부 연동
- 디스코드 봇 연동 — `events.discord_channel_id`, `clubs.discord_guild_id` 가 스키마에 있고 알림/리마인드를 실제 채널에 발송하려면 봇 필요
- S3/MinIO 같은 이미지 스토리지 — 영수증·사진 multipart 의 전제
- presigned URL 발급 엔드포인트

### 에러 처리
- `HttpRequestMethodNotSupportedException` → 405 (현재 catch-all 500)
- `NoResourceFoundException` → 404 (정의되지 않은 경로)
- `ErrorCode` 에 `METHOD_NOT_ALLOWED` 추가

### 테스트 보강
- `DiscordOAuthService` (RestClient 모킹 또는 WireMock)
- `CalendarController` MockMvc

### 새 기능 후보 (원래 계획엔 없던 것)
- 클럽 공개 정보 / 가입 신청 흐름 (현재 "내가 속한 클럽" 만 조회 가능)
- 출석 통계 (행사별 참석률, 사용자별 누적 출석)
- 행사 댓글/QnA
- 푸시·이메일 알림
