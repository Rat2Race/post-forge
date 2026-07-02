# Auth 모듈 질문 메모

이 문서는 auth 모듈의 테스트 의도를 정리하기 전에 답해볼 질문을 모아둔다.
Redis key와 TTL 상세는 [auth-redis.md](./auth-redis.md)를 참고한다.

## 책임과 상태

#### Q1. auth 모듈이 직접 소유하는 상태는 무엇인가요?

답변:

#### Q2. auth 모듈이 다른 모듈에 넘겨주는 인증 정보는 무엇인가요?

답변:

#### Q3. 계정 상태와 Redis 인증 상태가 어긋나면 어떤 문제가 생길까요?

답변:

## 중복 요청과 동시성

#### Q4. 같은 회원가입 요청이 동시에 두 번 들어오면 어떻게 되어야 할까요?

답변:

#### Q5. OAuth2 신규 계정 생성 요청이 중복으로 들어오면 어떤 상태가 생길 수 있을까요?

답변:

#### Q6. 이미 사용한 OAuth2 code나 email token을 다시 보내면 어떻게 처리되어야 할까요?

답변:

#### Q7. refresh token 재발급 요청이 동시에 여러 번 들어오면 어떤 문제가 생길 수 있을까요?

답변:

#### Q8. 비밀번호 변경과 닉네임 변경처럼 같은 계정 row를 동시에 수정하면 어떻게 처리되어야 할까요?

답변:

## 장애와 보안

#### Q9. Redis가 실패하면 로그인 제한, refresh token, OAuth2 code, email token은 각각 어떻게 처리되어야 할까요?

답변:

#### Q10. 메일 발송이 실패하면 email verification 상태는 어떻게 남아야 할까요?

답변:

#### Q11. password, token, OAuth code, email verification URL은 로그나 응답에 노출될 수 있을까요?

답변:

#### Q12. access token은 stateless인데 계정 비활성화나 role 변경을 즉시 반영해야 한다면 무엇이 필요할까요?

답변:

## 테스트와 부하

#### Q13. auth에서 가장 먼저 남겨야 할 단위 테스트는 무엇인가요?

답변:

#### Q14. auth에서 persistence나 Redis adapter 테스트로 확인해야 할 것은 무엇인가요?

답변:

#### Q15. auth controller 테스트는 어떤 응답 계약을 고정해야 할까요?

답변:

#### Q16. k6로 login, refresh, protected API를 부하 테스트한다면 어떤 지표를 봐야 할까요?

답변:
