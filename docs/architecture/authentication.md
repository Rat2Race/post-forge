## 로그인 보호 정책

`POST /auth/login`은 BCrypt 검증 때문에 CPU 비용이 큰 endpoint다.
BCrypt cost는 보안 강도와 직접 연결되므로 기본 정책에서는 낮추지 않고, Redis 기반 보호 정책으로 과도한 반복 시도를 먼저 제한한다.

현재 기본값은 다음과 같다.

| 항목 | 기본값 |
| --- | ---: |
| 사용자별 로그인 시도 제한 | 60초당 10회 |
| IP별 로그인 시도 제한 | 60초당 30회 |
| 실패 누적 관찰 구간 | 300초 |
| 사용자별 실패 허용 횟수 | 5회 |
| 실패 한도 도달 시 잠금 | 300초 |

처리 순서:

1. `LoginService`가 `AuthenticationManager` 호출 전에 `LoginAttemptGuard`로 사용자/IP별 rate limit과 잠금 상태를 확인한다.
2. 인증 실패 시 사용자별 실패 카운터를 증가시킨다.
3. 실패 횟수가 한도에 도달하면 사용자 잠금 키를 만들고 `429 TOO_MANY_REQUESTS`를 반환한다.
4. 인증 성공 시 해당 사용자의 실패 카운터와 잠금 키를 삭제한다.

Redis 장애 시에는 정상 로그인을 막지 않기 위해 fail-open으로 동작하고 warn 로그를 남긴다.
운영값은 `auth.login.protection.*` 설정으로 조정한다.
