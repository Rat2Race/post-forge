# Auth 모듈 병목/장애 Q&A

이 문서는 auth 모듈에서 발생할 수 있는 동시성, 데이터 정합성, 외부 의존성 장애, 토큰 라이프사이클, 보안, 성능 병목, 사용자 회복 시나리오를 질문과 답변 형태로 정리한다.

표시 기준:
- `[답변 추가]`: 기존 질문 중 답변이 비어 있던 항목에 답변을 보강했다.
- `[질문 추가]`: 병목이나 장애 관점에서 추가로 고려해야 할 질문을 새로 넣었다.

## 동시성 / 데이터 정합성

### Q1. 비밀번호 변경과 닉네임 변경이 동시에 들어오거나 같은 변경 요청이 중복 전송되면?

A. `accounts.version`의 `@Version` 기반 낙관적 락으로 같은 계정 row의 동시 업데이트 충돌을 감지한다.

- 먼저 커밋된 트랜잭션이 `version`을 증가시킨다.
- 늦게 flush/commit 하는 요청은 `OptimisticLockingFailureException` 또는 `OptimisticLockException` 계열 예외로 실패한다.
- 전역 예외 핸들러는 이를 `CONCURRENT_MODIFICATION` 409로 매핑한다.
- 비밀번호 변경, 닉네임 변경, 계정 상태 변경처럼 같은 `Account` row를 수정하는 요청은 같은 방식으로 보호된다.

현재 트래픽 특성상 매번 발생하는 경합이 아니라면 비관적 락보다 낙관적 락이 적절하다. 충돌 빈도가 높아지면 계정 단위 쓰기 잠금, 요청 디바운스, idempotency key를 추가로 검토한다.

### Q2. 신규 사용자가 OAuth2로 접근할 때 중복 요청을 보내면?

A. 현재 `OAuth2AccountService`는 `get-or-create` 방식으로 처리한다.

- 먼저 `(provider, providerId)`로 계정을 조회한다.
- 없으면 OAuth 계정을 생성한다.
- 동시 생성 중 unique constraint가 발생하면 다시 `(provider, providerId)`로 조회해 이미 생성된 계정을 반환한다.

이 방식은 같은 OAuth 제공자 식별자에 대한 race condition을 막는다. 다만 예외가 이메일, 닉네임, username 충돌 때문에 발생한 경우에는 재조회로 계정을 찾지 못하고 generic 409로 전파될 수 있다. 이메일 충돌 정책과 닉네임 생성 충돌 정책은 별도로 정의해야 한다.

### Q3. 계정 비활성화와 로그인/토큰 재발급이 동시에 일어나면? `[질문 추가]`

A. 로그인과 refresh token 재발급은 계정 조회 후 `isActive()`를 확인한다. 하지만 이미 발급된 access token은 만료 전까지 DB를 다시 조회하지 않으므로 비활성화가 즉시 반영되지 않을 수 있다.

즉시 차단이 필요한 계정 상태 변경이라면 access token TTL 단축, token version, revocation list, 고위험 API의 DB 상태 재확인 중 하나가 필요하다.

### Q4. OAuth2 one-time code 교환 요청이 중복으로 들어오면? `[질문 추가]`

A. `OAuth2CodeService.exchangeCode`는 Redis `getAndDelete`를 사용하므로 code는 1회만 사용할 수 있다. 첫 요청은 성공하고, 같은 code를 다시 교환하면 `INVALID_TOKEN`으로 실패한다.

프론트엔드가 네트워크 타임아웃 후 같은 code로 재시도할 수 있다면 "이미 사용되었거나 만료된 code"에 대한 사용자 안내와 재로그인 흐름이 필요하다.

## DB 제약 / 트랜잭션

### Q1. 회원가입 요청을 동시에 받으면?

A. [답변 추가] 애플리케이션의 `existsByUsername`, `existsByNickname`, 이메일 인증 여부 검사는 사용자에게 빠른 피드백을 주기 위한 선행 검사이고, 최종 정합성은 DB unique constraint가 보장한다.

- `accounts`에는 `username`, `email`, `nickname`, `(provider, provider_id)` unique constraint가 있다.
- 동시에 같은 값으로 가입하면 한 요청만 `saveAndFlush`에 성공하고 나머지는 `DataIntegrityViolationException`으로 실패한다.
- 현재 전역 핸들러는 이를 `DATA_INTEGRITY_VIOLATION` 409로 매핑한다.
- 사용자 경험을 높이려면 constraint 이름을 분석해 `DUPLICATE_USERNAME`, `DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`처럼 더 구체적인 오류로 매핑한다.

`exists*` 검사만으로 중복 가입을 막으려고 하면 race condition이 남는다. DB constraint는 반드시 유지해야 한다.

### Q2. 일반 회원가입과 OAuth2 회원가입의 email이 같으면?

A. [답변 추가] 현재 `accounts.email`은 provider와 관계없이 unique이므로 같은 이메일로 두 계정을 만들 수 없다.

- 일반 회원가입은 메일 인증 발송 단계에서 이미 가입된 이메일이면 `DUPLICATE_EMAIL`로 막는다.
- OAuth2 가입 경로는 생성 전에 같은 이메일 계정을 명시적으로 조회하지 않으므로, local 계정과 같은 이메일이면 DB unique constraint 위반으로 generic 409가 날 수 있다.

정책 결정이 필요하다.

- 중복 이메일을 차단한다면 OAuth2 가입 전에 같은 이메일 계정을 조회해 명확한 오류를 반환한다.
- 계정 연결을 허용한다면 기존 계정 재인증 후 provider identity를 연결해야 한다.
- provider가 내려준 이메일만 보고 자동 병합하면 계정 탈취 위험이 있으므로 이메일 검증 여부와 재인증 절차를 확인해야 한다.

### Q3. 제약 조건 예외 `DataIntegrityViolationException` 발생 시 어떤 제약 조건 오류이고 어떤 입력값 때문에 생긴 오류인지 파악할 수 없나?

A. 현재 전역 핸들러는 모든 `DataIntegrityViolationException`을 `DATA_INTEGRITY_VIOLATION` 409로 반환한다. 그래서 클라이언트는 username, email, nickname 중 무엇이 중복인지 구분하기 어렵다.

개선 방향:
- DB constraint 이름을 명시적으로 유지한다. 예: `uk_accounts_username`, `uk_accounts_email`, `uk_accounts_nickname`.
- 알려진 constraint 이름을 business error code로 매핑한다.
- DB 원문 메시지나 사용자가 입력한 민감 값을 그대로 응답하지 않는다.
- 선행 `exists*` 검사는 유지하되, 동시성 race의 최종 방어는 constraint 예외 매핑으로 처리한다.

### Q4. 트랜잭션 commit 시점에만 드러나는 예외는 어떻게 사용자에게 매핑하나? `[질문 추가]`

A. 중복 가입, 동시 수정, 지연 flush는 service 내부 검사를 통과한 뒤 commit/flush 시점에 실패할 수 있다. 이 예외는 controller나 service의 일반 분기보다 전역 예외 핸들러에서 일관되게 매핑해야 한다.

현재는 optimistic lock과 data integrity violation을 각각 409로 매핑한다. 다음 단계는 constraint 이름, 충돌 대상, 재시도 가능 여부를 기준으로 사용자 행동이 가능한 메시지로 세분화하는 것이다.

## Redis 의존성 / TTL

### Q1. Redis 장애가 발생하면 어떤 기능이 영향을 받나? `[질문 추가]`

A. auth 모듈은 Redis를 이메일 인증, OAuth2 code, refresh token, 로그인 시도 제한에 사용한다.

- 이메일 인증 token 조회/저장 실패는 회원가입 흐름을 막을 수 있다.
- OAuth2 code 저장/교환 실패는 OAuth2 로그인 완료를 막을 수 있다.
- refresh token 조회/저장 실패는 재발급과 로그아웃을 불안정하게 만든다.
- 로그인 시도 제한은 Redis 예외를 잡고 fail-open으로 통과시킨다. 가용성은 지키지만 brute-force 방어가 일시적으로 약해진다.

Redis 장애는 인증 장애로 바로 이어질 수 있으므로 health check, latency/error metric, key TTL 모니터링이 필요하다.

### Q2. Redis 장애 시 fail-open과 fail-closed 기준은 무엇인가? `[질문 추가]`

A. 기능별로 기준을 다르게 잡아야 한다.

- 로그인 시도 제한은 부가 방어 계층이므로 현재처럼 fail-open을 선택하면 가용성은 유지된다.
- refresh token 검증, OAuth2 code 교환, 이메일 인증 token 검증은 인증 상태 자체를 증명하므로 fail-closed가 맞다.
- fail-open 구간은 보안 약화가 발생하므로 반드시 경고 로그, metric, 알림이 붙어야 한다.

### Q3. 메일 인증 요청이 중복으로 들어오면?

A. [답변 추가] 현재 구현은 요청마다 새 토큰을 만들고 `email_verify_token:{token}` 키에 30분 TTL로 저장한 뒤 메일을 발송한다. 같은 이메일로 여러 번 요청하면 여러 토큰이 동시에 유효할 수 있다.

위험 요소:
- 사용자는 어떤 메일의 링크가 최신인지 혼동할 수 있다.
- 메일 발송량이 늘어 SMTP 병목이나 발송 비용 증가로 이어질 수 있다.
- 공격자가 특정 이메일로 반복 요청하면 Redis 키와 메일 발송이 함께 증가한다.

개선 방향:
- 이메일/IP 단위 재요청 쿨다운을 둔다.
- `email_verify_email:{email}` 같은 역방향 키를 두어 이전 토큰을 무효화하거나 최신 토큰만 유효하게 만든다.
- 짧은 시간 안의 중복 요청은 새 메일을 보내지 않고 idempotent 성공 응답으로 처리한다.

### Q4. 이메일 인증 token TTL이나 인증 완료 TTL이 만료되면? `[질문 추가]`

A. 인증 token은 30분, 인증 완료 상태는 1시간 TTL을 가진다. 사용자가 메일 인증은 했지만 1시간 안에 회원가입을 끝내지 않으면 `EMAIL_NOT_VERIFIED`로 실패할 수 있다.

사용자 경험상 "인증이 만료되었으니 다시 인증해 주세요" 메시지와 재발송 경로가 필요하다. 운영상으로는 인증 요청 대비 완료율과 인증 완료 후 회원가입 실패율을 같이 봐야 한다.

### Q5. 로그인 제한 Redis key가 hot key가 될 수 있나? `[질문 추가]`

A. 가능하다. 공격자가 특정 username이나 특정 IP로 요청을 몰면 `auth:login:rate:user:*`, `auth:login:rate:ip:*`, `auth:login:fail:*`, `auth:login:lock:*` key가 집중적으로 증가한다.

TTL이 있어 영구 누적되지는 않지만 Redis latency와 key cardinality를 모니터링해야 한다. 대규모 공격은 애플리케이션 Redis만으로 막지 말고 gateway, WAF, CDN rate limit과 함께 방어한다.

## 토큰 / 세션 라이프사이클

### Q1. Refresh token 재발급 요청이 동시에 들어오면? `[질문 추가]`

A. 현재 재발급 흐름은 Redis에 저장된 refresh token과 요청 token을 비교한 뒤 새 refresh token을 저장한다. 같은 refresh token으로 두 요청이 거의 동시에 들어오면 둘 다 기존 token 검증을 통과한 뒤 각각 새 token을 발급할 수 있고, 마지막으로 저장된 refresh token만 살아남을 수 있다.

단일 사용 refresh token rotation을 강하게 보장하려면 Redis Lua script나 compare-and-delete/compare-and-set 방식으로 검증과 교체를 원자적으로 처리한다. 클라이언트는 중복 재발급 요청 중 하나가 `INVALID_TOKEN`으로 실패할 수 있다는 전제를 가져야 한다.

### Q2. 로그아웃과 refresh token 재발급이 동시에 들어오면? `[질문 추가]`

A. 로그아웃은 Redis refresh token을 삭제하고 쿠키를 제거한다. 재발급은 기존 refresh token 검증 후 새 refresh token을 저장한다.

두 요청이 겹치면 재발급이 먼저 검증을 통과한 뒤 로그아웃 삭제 이후 새 token을 다시 저장하는 race가 생길 수 있다. 강한 로그아웃 보장이 필요하면 계정별 session version, logout-after timestamp, refresh token family revocation, Redis 원자 연산을 검토한다.

### Q3. 로그아웃 후 이미 발급된 access token은 계속 유효한가? `[질문 추가]`

A. 현재 구조에서는 access token이 stateless라 로그아웃이 기존 access token을 직접 폐기하지 않는다. access token은 만료될 때까지 사용할 수 있다.

이 위험은 access token TTL을 짧게 유지해 줄인다. 즉시 폐기가 필요하면 access token blacklist, token version, 고위험 API의 DB/Redis 상태 확인이 필요하다.

### Q4. refresh token 쿠키 설정은 안전한가? `[질문 추가]`

A. 현재 refresh token 쿠키는 `HttpOnly`, `Secure`, `SameSite=Lax`, `path=/auth`로 설정된다. XSS로 JS가 쿠키를 직접 읽는 위험은 줄이고, HTTPS 전송을 강제하며, 쿠키 전송 범위도 `/auth`로 제한한다.

운영에서 확인할 부분:
- HTTPS가 아닌 로컬/테스트 환경에서 `Secure` 쿠키가 동작하지 않을 수 있다.
- cross-site OAuth redirect나 프론트 도메인 구조에 따라 `SameSite=Lax`가 적절한지 검증해야 한다.
- refresh token 쿠키 삭제 시 path/domain 속성이 발급 시점과 일치해야 한다.

### Q5. JWT secret 교체 시 기존 token은 어떻게 처리하나? `[질문 추가]`

A. JWT secret이 바뀌면 기존 token 서명 검증이 실패한다. 보안 사고 대응에는 유용하지만, 일반 배포 중 실수로 secret이 바뀌면 전체 사용자가 강제 로그아웃될 수 있다.

운영 정책으로 secret rotation 절차, 구버전 secret grace period 여부, 강제 로그아웃 공지 기준을 정해야 한다.

## OAuth2 로그인 장애

### Q1. OAuth 제공자 장애나 지연이 있으면? `[질문 추가]`

A. OAuth2 로그인은 외부 provider의 token/user-info 응답에 의존한다. provider 지연은 로그인 latency를 늘리고, provider 오류는 `OAUTH_USER_INFO_FAILED` 또는 인증 실패로 이어질 수 있다.

운영 관점에서는 provider별 오류율과 응답 시간을 분리해 봐야 한다. 장애 시에는 일반 로그인으로 우회 가능한지, OAuth 전용 계정 사용자에게 어떤 안내를 보여줄지 정책이 필요하다.

### Q2. OAuth provider가 이메일을 안 주거나 검증되지 않은 이메일을 주면? `[질문 추가]`

A. 현재 OAuth 계정 생성은 provider에서 받은 이메일을 계정 이메일로 사용한다. provider가 이메일을 내려주지 않거나 검증되지 않은 이메일을 주는 경우 계정 생성 실패, DB 제약 위반, 잘못된 계정 연결 위험이 생긴다.

provider별 이메일 제공 여부와 검증 여부를 명시하고, 이메일이 없으면 추가 이메일 인증 절차를 요구하는 정책이 필요하다.

### Q3. OAuth provider user id는 변하지 않는 값이라고 신뢰할 수 있나? `[질문 추가]`

A. 계정 식별의 최종 기준은 `(provider, provider_id)` unique constraint다. provider id가 stable identifier라는 전제가 깨지면 계정 매핑이 흔들린다.

provider 문서 기준으로 어떤 필드가 불변 식별자인지 확인하고, provider별 user-info 파싱 테스트를 유지해야 한다.

### Q4. OAuth success 이후 프론트로 전달하는 code가 유실되면? `[질문 추가]`

A. OAuth2 code는 Redis에 60초 TTL로 저장되고 1회 교환된다. 프론트 redirect, 네트워크 지연, 새로고침으로 code가 유실되거나 만료되면 사용자는 다시 OAuth 로그인부터 시작해야 한다.

UX 관점에서는 "로그인 시간이 만료되었습니다" 안내와 재로그인 버튼이 필요하다.

## 메일 인증 장애

### Q1. 메일 인증을 요청했는데 메일이 안 왔을 경우

A. [답변 추가] 현재 메일은 요청 처리 중 `JavaMailSender`로 동기 발송된다. 템플릿 로딩 실패나 SMTP 발송 예외는 `EMAIL_SEND_FAILED` 500으로 반환된다.

다만 SMTP 서버가 메일을 수락한 뒤 실제 수신함에서 지연, 스팸 분류, bounce가 발생하는 경우는 애플리케이션이 즉시 알기 어렵다.

필요한 운영 대응:
- 재발송 버튼과 재요청 쿨다운을 제공한다.
- 인증 링크 TTL과 만료 시 재요청 방법을 안내한다.
- SMTP 발송 실패율, 발송 지연, template 로딩 실패를 모니터링한다.
- 발송량이 늘어나면 요청 thread에서 직접 보내지 말고 비동기 큐나 outbox 기반 발송으로 분리한다.

### Q2. 메일 발송이 요청 latency 병목이 될 수 있나? `[질문 추가]`

A. 가능하다. 현재 인증 메일 발송은 요청 thread에서 동기 처리된다. SMTP 연결 지연이나 메일 서버 장애가 있으면 API 응답 시간이 함께 늘어난다.

트래픽이 늘면 발송 요청을 저장한 뒤 비동기 worker/outbox가 메일을 보내도록 분리한다. API는 "발송 요청 접수"를 빠르게 반환하고, 실제 발송 실패는 재시도와 운영 알림으로 처리한다.

### Q3. 인증 URL이나 token이 로그에 남을 수 있나? `[질문 추가]`

A. `JavaMailEmailSender`는 인증 URL을 debug log로 남긴다. 운영에서 debug log가 켜지면 이메일 인증 token이 로그에 남을 수 있다.

운영 환경에서는 debug log 비활성화를 전제로 하고, token/authorization code/refresh token은 로그 마스킹 대상에 포함한다.

### Q4. 메일 재발송 쿨다운은 필요한가? `[질문 추가]`

A. 필요하다. 재발송 제한이 없으면 SMTP 비용, 발송 평판, Redis key 증가, 사용자 혼란이 생긴다.

이메일 단위와 IP 단위 쿨다운을 함께 두고, 마지막 발송 시각을 기준으로 너무 빠른 재요청은 새 메일을 발송하지 않거나 429로 안내한다.

## 로그인 보호 / Rate Limit

### Q1. 로그인 brute-force 방어는 충분한가? `[질문 추가]`

A. `LoginAttemptGuard`는 username/IP 기준 요청량 제한과 실패 횟수 기반 lock을 제공한다.

- 기본값은 60초 동안 사용자 10회, IP 30회 요청 제한이다.
- 실패 5회 이상이면 300초 동안 username을 잠근다.
- Redis 장애 시에는 fail-open으로 우회한다.

프록시나 로드밸런서 뒤에서 실제 client IP가 올바르게 전달되는지 확인해야 한다. 공격자가 username을 분산하거나 IP를 분산하는 경우에는 gateway/WAF 단위 제한도 함께 필요하다.

### Q2. 프록시 뒤에서 client IP를 정확히 얻고 있나? `[질문 추가]`

A. 로그인 컨트롤러는 `HttpServletRequest.getRemoteAddr()` 값을 로그인 가드에 넘긴다. 프록시나 로드밸런서 뒤에서는 이 값이 실제 사용자 IP가 아니라 프록시 IP가 될 수 있다.

운영 환경에서는 trusted proxy 설정, `Forwarded`/`X-Forwarded-For` 처리, spoofing 방지 정책이 필요하다. 잘못 설정하면 IP rate limit이 무력화되거나 모든 사용자가 같은 IP로 묶일 수 있다.

### Q3. username lock이 정상 사용자 UX를 해치지 않나? `[질문 추가]`

A. 공격자가 타인의 username으로 실패 로그인을 반복하면 해당 계정 사용자가 일시적으로 로그인하지 못할 수 있다. 이는 보안과 UX의 tradeoff다.

계정 lock 메시지는 민감 정보를 노출하지 않으면서도 재시도 가능 시간을 알려주는 방향이 좋다. 필요하면 CAPTCHA, 추가 인증, IP 평판 기반 완화도 검토한다.

### Q4. 공격자가 username/IP를 분산하면 현재 제한으로 충분한가? `[질문 추가]`

A. 애플리케이션 단의 username/IP 제한만으로는 분산 공격을 완전히 막기 어렵다. gateway, WAF, CDN, ASN/국가/디바이스 기반 이상 탐지와 함께 운영해야 한다.

로그인 실패율, 계정별 lock 발생, IP별 실패 cardinality를 지표로 봐야 한다.

## SecurityContext / 인증 주체

### Q1. 전역에서 사용 가능한 SecurityContext에 password 같은 credential 값을 넣는 게 맞을까?

A. 로그인 인증에 필요한 객체와 인증 이후 요청 처리 객체를 분리하는 현재 방향이 적절하다.

- `CustomUserDetails`는 ID/PW 인증을 위해 `AuthenticationManager`와 `DaoAuthenticationProvider`에 전달하는 재료다.
- JWT 인증 이후 `SecurityContext`에는 `AuthenticatedAccount(accountId)`와 권한만 들어간다.
- `AuthenticatedAccount`는 변동 가능한 username이 아니라 PK인 `accountId`만 들고 있어 내부 요청 처리 기준으로 적합하다.
- JWT 인증 객체의 credentials는 `null`이므로 password가 요청 처리용 principal에 남지 않는다.

추가로 `CustomUserDetails`가 로그에 찍히지 않게 하고, 인증 객체가 장시간 보관되는 구조가 생긴다면 `CredentialsContainer` 구현이나 credential erase 동작을 확인한다.

### Q2. principal에 계정 정보를 많이 넣으면 stale data가 생기지 않나? `[질문 추가]`

A. 생길 수 있다. 현재 `AuthenticatedAccount`는 `accountId`만 들고 있으므로 stale data 위험이 낮다. 닉네임, 이메일, 계정 상태, 역할 같은 값을 principal에 넣으면 변경 이후에도 오래된 값으로 요청이 처리될 수 있다.

요청 처리에 필요한 최신 정보는 필요한 시점에 DB/캐시에서 조회하고, principal은 안정적인 식별자 중심으로 유지하는 편이 안전하다.

## 권한 / 계정 상태 반영

### Q1. JWT에 담긴 role이나 계정 상태가 변경되면 즉시 반영되나? `[질문 추가]`

A. access token은 stateless이고 role claim을 포함한다. `JwtProvider.resolveAuthentication`은 token의 subject와 role만 파싱하며 매 요청마다 DB에서 계정 상태를 다시 확인하지 않는다.

따라서 role 변경이나 계정 비활성화는 기존 access token이 만료될 때까지 지연 반영될 수 있다. 현재 설정상 access token 유효기간은 15분이다. 즉시 차단이 필요하면 token version, revocation list, 짧은 TTL, 고위험 API의 DB 상태 재확인 중 하나를 도입한다.

### Q2. 관리자 권한 회수 같은 고위험 변경은 즉시 반영되어야 하나? `[질문 추가]`

A. 일반 사용자 role 변경은 짧은 access token TTL로 충분할 수 있지만, 관리자 권한 회수는 즉시 반영 요구가 더 강하다.

관리자 API는 token claim만 믿지 않고 DB에서 최신 role/status를 재확인하거나, 관리자 권한 변경 시 token version을 증가시켜 기존 token을 무효화하는 방식이 필요하다.

### Q3. 계정 삭제/탈퇴 후 refresh token과 인증 상태는 정리되나? `[질문 추가]`

A. 계정 삭제나 탈퇴 정책이 들어오면 refresh token 삭제, 이메일 인증 상태 삭제, OAuth code 무효화, access token 잔여 유효시간 처리가 함께 정의되어야 한다.

현재 계정 상태 변경은 `AccountStatus` 중심이므로, 탈퇴를 soft delete로 볼지 hard delete로 볼지에 따라 token 정리 전략도 달라진다.

## 성능 병목

### Q1. 사용자 기능을 쓸 때마다 Account DB 조회를 해야 하나?

A. JWT 인증 경로 자체는 매 요청마다 Account DB를 조회하지 않는다. access token에서 `accountId`와 role을 파싱해 `AuthenticatedAccount`를 만들기 때문이다.

DB 조회가 필요한 경우:
- 최신 계정 상태, 닉네임, 이메일, 역할을 화면이나 비즈니스 로직에서 써야 할 때
- refresh token 재발급처럼 계정 활성 상태를 다시 확인해야 할 때
- 다른 모듈이 작성자 프로필을 조회해야 할 때

접속 중인 사용자 정보를 캐시할 수는 있지만 status/role/nickname 변경 시 invalidation 전략이 필요하다. 보안에 영향을 주는 status와 role은 긴 TTL 캐시를 피하고, 필요한 API에서만 명시적으로 조회하는 편이 안전하다.

### Q2. 비밀번호 해시가 CPU 병목이 될 수 있나? `[질문 추가]`

A. 가능하다. `PasswordEncoderFactories.createDelegatingPasswordEncoder()`는 기본적으로 bcrypt 계열 인코더를 사용하므로 로그인 검증, 회원가입, 비밀번호 변경은 CPU 비용이 있는 작업이다.

이 비용은 보안을 위해 필요한 지연이다. 대신 로그인 rate limit, 요청량 모니터링, p95/p99 latency 측정, 적절한 bcrypt cost 조정으로 관리한다.

### Q3. Redis latency가 auth 전체 latency에 영향을 줄 수 있나? `[질문 추가]`

A. 그렇다. 로그인 제한, refresh token, 이메일 인증, OAuth2 code가 Redis에 의존하므로 Redis latency가 올라가면 auth API latency도 같이 올라간다.

Redis command latency, timeout, connection pool saturation을 별도 지표로 보고, 장애 시 어떤 기능이 fail-open/fail-closed인지 명확히 해야 한다.

### Q4. OAuth provider 지연이 thread pool을 고갈시킬 수 있나? `[질문 추가]`

A. 가능하다. OAuth user-info 호출이 느려지면 요청 thread가 외부 응답을 기다리며 묶일 수 있다. 장애가 길어지면 로그인 요청이 누적되어 전체 auth 응답성에 영향을 줄 수 있다.

provider별 timeout, circuit breaker, fallback UX, provider 장애 알림이 필요하다.

## 사용자 회복 시나리오

### Q1. 동시성 충돌이나 rate limit 실패를 사용자가 어떻게 회복해야 하나? `[질문 추가]`

A. 409 `CONCURRENT_MODIFICATION`은 "최신 정보로 다시 조회 후 재시도"가 맞다. 429 `TOO_MANY_REQUESTS`는 lock/cooldown 시간이 지나면 재시도하도록 안내해야 한다.

클라이언트는 비밀번호 변경, 닉네임 변경, 토큰 재발급 버튼에 중복 클릭 방지를 넣고, 실패 시 무한 재시도하지 않도록 한다.

### Q2. 인증 메일 만료/분실 시 재발송 흐름이 명확한가? `[질문 추가]`

A. 인증 token 만료, 인증 완료 TTL 만료, 메일 미수신은 모두 사용자가 직접 회복할 수 있어야 한다. 재발송 버튼, 쿨다운 안내, 기존 링크 만료 안내가 필요하다.

서버는 만료/분실을 모두 generic 실패로만 반환하지 말고, 사용자가 다음 행동을 알 수 있는 메시지를 제공해야 한다.

### Q3. OAuth code 만료 시 다시 로그인으로 자연스럽게 이어지나? `[질문 추가]`

A. OAuth2 code는 60초 TTL이고 1회성이다. 만료되거나 이미 사용된 code는 `INVALID_TOKEN`으로 실패한다.

프론트는 이 실패를 일반 인증 실패처럼 보여주기보다 "로그인 시간이 만료되었습니다. 다시 로그인해 주세요"로 안내해야 한다.

## 관측 / 운영 지표

### Q1. auth 모듈에서 어떤 지표를 봐야 하나? `[질문 추가]`

A. 병목과 장애를 조기에 발견하려면 기능별 지표를 나눠서 봐야 한다.

추적하면 좋은 지표:
- `OptimisticLockingFailureException` 발생 횟수
- `DataIntegrityViolationException` constraint별 발생 횟수
- 인증 메일 발송 latency, 실패율, 재요청 횟수
- 이메일 인증 요청 대비 완료율
- Redis command latency/error, auth 관련 key cardinality
- 로그인 실패/lock/rate limit 발생 횟수
- refresh token 재발급 성공률과 실패율
- OAuth provider별 user-info latency와 실패율
- 계정 비활성화 후 token 사용 시도 수
- access token/refresh token 관련 `INVALID_TOKEN`, `EXPIRED_TOKEN` 비율
