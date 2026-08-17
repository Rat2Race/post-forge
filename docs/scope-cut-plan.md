# 스코프 컷 실행 계획 (ADR-004)

[ADR-004](decisions/adr-004-scope-reduction.md)가 **왜**, 이 문서가 **무엇을 어떤 순서로**를 담는다. 목표 모듈 구성은 [CLAUDE.md](../CLAUDE.md).

작성 시점 기준선: 커밋 `412f3d4c`, 브랜치 `release/postforge`. 컷 완료 후 이 파일은 삭제한다.

## 선행 블로커: `TrackedKeyword`

`ingest/news`(생존)의 `LaunchNewsPublishScheduler`가 `ingest/product`의 `TrackedKeywordService` / `TrackedKeyword`를 쓴다. 그리고 `TrackedKeyword`는 `source/product`의 `SourceType`을 참조한다.

```
ingest/news/LaunchNewsPublishScheduler  →  ingest/product/TrackedKeyword  →  source/product/SourceType
        (유지)                                    (삭제 대상)                      (삭제 대상)
```

`ingest/product`와 `source/product`를 먼저 지우면 생존 코드가 컴파일되지 않는다. **1단계를 반드시 먼저 끝낸다.**

- [ ] `TrackedKeyword`, `TrackedKeywordService`, `TrackedKeywordRepository`, `TrackedKeywordResponse`를 `ingest/product` → `ingest/news`로 이동
- [ ] `TrackedKeyword.source` 필드 결정: 뉴스 소스만 남으므로 컬럼째 제거하거나, `source/news`에 뉴스 전용 enum을 두고 갈아끼운다. 제거 쪽이 더 짧다
- [ ] `tracked_keywords` 테이블 소유자를 `ingest/news`로 바꾸고 [schema-ownership.md](database/schema-ownership.md) 39행 갱신
- [ ] `ProductCollectionAdminController`의 키워드 등록 API가 사라지므로, 뉴스 키워드 관리 엔드포인트가 필요한지 판단 (없어도 되면 DB 직접 입력)

## 0. 준비

- [ ] `git tag pre-scope-cut` — 유일한 복구 수단. 태그가 아직 없다
- [ ] 삭제 대상 테이블(`outbox_events`, `price_snapshots`, `products`, `offers`, `raw_products`, `product_embeddings`, `product_match_candidates`, `product_categories`, `post_product_links`, `collection_jobs`)에 보존할 실데이터 없음 확인

## 1. 모듈 통째 삭제

- [ ] `catalog/` (java 37) — 상품 + 임베딩 매칭
- [ ] `price/` (java 22) — check + tracking
- [ ] `messaging/` (java 23) — outbox + publisher
- [ ] `settings.gradle`에서 `:catalog`, `:price`, `:messaging` 제거

## 2. 패키지·클래스 삭제

- [ ] `ingest/src/{main,test}/java/dev/iamrat/ingest/product/` — 단, 1단계에서 옮긴 `TrackedKeyword*` 제외
- [ ] `source/src/{main,test}/java/dev/iamrat/source/product/`
- [ ] `ai/.../support/infrastructure/llm/LlmProductEmbeddingClient.java` + 테스트 — `catalog`의 `ProductEmbeddingClient` 구현체
- [ ] `ai/src/main/resources/prompts/news-analysis-{system,user}.md` — 호출자 없음
- [ ] `ai/.../support/error/AiErrorCode.java` — 고아
- [ ] `board`: `post/domain/PostProductLink.java`, `post/infrastructure/persistence/PostProductLinkRepository.java`, `post/application/ProductPostQueryService.java`(+테스트), `post/presentation/ProductPostController.java`

## 3. 빌드·설정 정리

- [ ] `ai/build.gradle:7` `project(':catalog')` 제거 → `ai`는 `core`만 참조
- [ ] `ingest/build.gradle:7-9` `:source` 유지, `:catalog` `:price` 제거
- [ ] `price/build.gradle` 파일째 삭제 (모듈과 함께)
- [ ] `app/build.gradle:16,17,20` `:catalog` `:price` `:messaging` 제거
- [ ] `app/src/main/resources/application.yml` `source.mock-product`(121행) 제거, `source.naver-news`는 유지
- [ ] `app/src/main/resources/application-prod.yml`에서 대응 키와 `MOCK_PRODUCT_SOURCE_ENABLED` 제거
- [ ] `.env.example`, `docker-compose.*.yml`에 남은 상품/가격 관련 환경변수 정리

## 4. 통과 계층 제거

1:1 `Store` 인터페이스 + `PersistenceAdapter` 사슬을 걷어내고 서비스가 JPA repository를 직접 쓰게 한다. 생존 모듈에 남는 쌍:

- [ ] `auth`: `AccountStore` / `AccountPersistenceAdapter`
- [ ] `board`: `CommentStore` `FileStore` `CommentLikeStore` `PostLikeStore` `PostStore` `PostReferenceLinkStore` `PurchaseVoteStore` + 각 `*PersistenceAdapter`

Redis 구현(`RedisEmailVerification*Store`, `RedisOAuth2CodeStore`, `RefreshTokenStore`, `ViewCountStore`)과 `ingest`의 `DocumentChunkStore`는 **유지**한다. 1:1 통과가 아니라 실제로 다른 저장소를 감싸는 경계다.

## 5. 스키마

- [ ] `V0000`~`V0004`를 컷 이후 스키마 하나로 스쿼시. 미커밋 상태인 지금만 가능하고, 한 번 push하면 창이 닫힌다
- [ ] 남는 테이블: `accounts` `account_roles` `posts` `comments` `post_like` `comment_like` `post_file` `post_tags` `post_purchase_vote` `post_reference_links` `vector_store`
- [ ] `vector_store`(Spring AI pgvector, RAG)는 유지. `product_embeddings`(catalog 매칭용)만 삭제 — 둘을 헷갈리지 않는다

## 6. 문서·테스트

- [ ] `app/src/test/java/dev/iamrat/app/architecture/ModuleBoundaryTest.java` 허용 그래프에서 삭제 모듈 제거
- [ ] [module-dependencies.md](architecture/module-dependencies.md) 허용 표 + 코드 배치 표
- [ ] [schema-ownership.md](database/schema-ownership.md) 37~48행, 94~99행
- [ ] [event-driven-outbox.md](architecture/event-driven-outbox.md) — 삭제 또는 "서비스 분리 시점에 재설계" 한 줄로 축소
- [ ] [adr-003-modular-monolith.md](decisions/adr-003-modular-monolith.md) 후속 변경의 messaging 경계
- [ ] `docs/database/postforge-mvp-erd.{dbml,md}`, `docs/api/`, `README.md`
- [ ] ADR-004 상태를 `Proposed` → `Accepted`로
- [ ] 이 파일 삭제

## 7. 검증

```bash
./gradlew clean build
```

- [ ] 빌드·전체 테스트 통과
- [ ] `ModuleBoundaryTest` 통과
- [ ] 빈 DB에 Flyway 새 baseline 적용 후 Hibernate `validate` 통과
- [ ] `grep -rn "catalog\|price\|messaging" --include="*.java" --include="*.gradle" --include="*.yml" . | grep -v build/` 결과가 비어 있음
