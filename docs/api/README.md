# PostForge API 명세

작성 기준: 2026-06-22 현재 `@RestController`, DTO, `PostForgeAuthorizationRules` 소스.

## 경로 원칙

- 애플리케이션 API는 `/api/...` 경로만 사용한다.
- Spring Security/OAuth2 프레임워크가 소유하는 `/oauth2/**`, `/login/oauth2/**`와 Swagger/static 경로는 API controller 명세에서 제외한다.
- Refresh token cookie는 `refresh_token` 이름을 쓰며 `Path=/api/auth`, `HttpOnly`, `Secure`, `SameSite=Lax`로 발급된다.

## 인증 표기

| 표기 | 의미 |
| --- | --- |
| `Public` | 인증 없이 호출 가능 |
| `Optional JWT` | 인증 없이 호출 가능하지만, JWT가 있으면 사용자별 상태를 응답에 반영 |
| `USER` | `ROLE_USER` 필요 |
| `USER/ADMIN` | `ROLE_USER` 또는 `ROLE_ADMIN` 필요 |
| `Owner/ADMIN` | 리소스 소유자 `USER` 또는 `ROLE_ADMIN` 필요 |
| `ADMIN` | `ROLE_ADMIN` 필요 |

## 공통 응답

| 타입 | 필드 |
| --- | --- |
| `PageResponse<T>` | `content`, `page`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last`, `empty` |
| `MessageResponse` | `message` |
| `UrlResponse` | `url` |
| `ErrorResponse` | `status`, `error`, `message`, `validation?`, `timestamp` |

## Pagination

Spring `Pageable` 파라미터를 쓰는 API는 기본적으로 `page`, `size`, `sort` query를 받는다.
컨트롤러에서 `@PageableDefault`를 지정한 경우 각 문서에 기본값을 적었다.

## 모듈 문서

| 모듈 | 문서 | 주요 경로 |
| --- | --- | --- |
| auth | [auth.md](auth.md) | `/api/auth/**`, `/api/user/account/**` |
| board | [board.md](board.md) | `/api/posts/**`, `/api/user/profile/**`, `/api/files/**` |
| catalog | [catalog.md](catalog.md) | `/api/products/**`, `/api/admin/products/**`, `/api/admin/product-match-candidates/**` |
| price | [price.md](price.md) | `/api/products/{productId}/prices`, `/api/price-checks` |
| ai | [ai.md](ai.md) | `/api/ai/**` |
| ingest | [ingest.md](ingest.md) | `/api/ingest/**`, `/api/admin/tracked-keywords/**`, `/api/admin/collection-jobs/**`, `/api/admin/news-documents/**`, `/api/admin/launch-news/**` |
