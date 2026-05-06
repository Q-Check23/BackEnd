# Q-Check

**대학교 동아리를 위한 행사 운영·출석·정산 통합 백엔드**

Discord 로 로그인하고, 동아리·행사·참가 신청·출석·회비 정산까지 한 번에 관리한다

## 주요 기능

- **Discord OAuth2 로그인** — 디스코드 계정으로 가입·로그인. JWT access·refresh 토큰 발급, refresh 회전 지원
- **동아리 관리** — 동아리 생성, 멤버 초대, 역할(OWNER / ADMIN / MEMBER) 위임, 탈퇴/제거
- **행사 관리** — 행사 생성/수정/삭제, 신청 폼 커스터마이징(텍스트·선택·체크박스·숫자), 참가 신청·취소, 행사 사진 업로드
- **출석 체크** — QR 코드 기반 자동 체크인, 관리자 수동 체크인
- **캘린더** — 내가 속한 동아리의 월별 일정, 행사명·동아리명·장소로 검색·필터링
- **회비 정산** — 그룹 단위 분담 금액 입력(보증금 납부자/미납자처럼 묶음 별 다른 금액 가능), 송금 신고·확인 흐름(UNPAID → PENDING → COMPLETED), 미납자 리마인드 기록
- **사용자 검색** — 닉네임·이메일로 사용자 찾아 클럽에 추가

## 스택

Spring Boot 3.4.2 · Spring Data JPA · MySQL · Flyway · JJWT · SpringDoc OpenAPI · JUnit 5 + Mockito

## 시작하기

### 필요한 환경 변수

```bash
LOCAL_MYSQL_URL=jdbc:mysql://localhost:3306/qcheck
LOCAL_MYSQL_USER=<user>
LOCAL_MYSQL_PASSWORD=<password>
CORS_ALLOWED_ORIGINS=http://localhost:3000
DISCORD_CLIENT_ID=<id>
DISCORD_CLIENT_SECRET=<secret>
DISCORD_REDIRECT_URI=http://localhost:8080/auth/code
JWT_SECRET=<at-least-32-chars>
```

### 실행

```bash
./gradlew bootRun
```

API 문서는 `http://localhost:8080/swagger-ui.html` 에서 확인.

### 테스트

```bash
./gradlew test
```

## API 한눈에 보기

| 경로 | 설명 |
|---|---|
| `/auth/**` | Discord 로그인, 회원가입, 토큰 갱신 |
| `/api/users/**` | 내 정보, 사용자 검색 |
| `/api/clubs/**` | 동아리·멤버 관리 |
| `/api/events/**` | 행사 CRUD, 신청, 사진 |
| `/api/attendance/**` | QR · 수동 출석 |
| `/api/calendar/**` | 월별 일정, 검색·필터 |
| `/api/settlements/**` | 정산 생성·조회·상태 변경·리마인드 |

자세한 개발 가이드는 [`CLAUDE.md`](./CLAUDE.md) 참고.
