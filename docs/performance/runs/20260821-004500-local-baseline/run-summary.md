# 2026-08-21 로컬 기준선 — 분야 카테고리·데일리 다이제스트 반영 후

증분 1(분야 카테고리 축, V0006)·증분 2(데일리 요약, V0007) 구현 직후의 로컬 기준선이다.
과거 기록(Oracle ARM / Intel N100 prod)과 하드웨어가 달라 **직접 비교 불가** — 이 파일이 로컬(M-series) 축의 첫 기준선이다.

## 환경

| 항목 | 값 |
| --- | --- |
| 앱 | `app.jar` (bootJar), Java 21, `--server.port=58080` |
| DB | pgvector/pgvector:0.8.2-pg18 (docker, 일회용), Flyway V0000~V0007 적용 |
| Redis | redis:7-alpine (docker, 일회용) |
| LLM | mock 서버 (OpenAI 호환 고정 응답) — 파이프라인 검증용, 추론 성능은 측정 대상 아님 |
| 데이터 | posts 2,005건 (분야 7종 순환, 30일 분산), 계정 1 |
| 부하 | k6 v2.0.0, constant-vus, localhost |

## 결과

| 시나리오 | VUs | 시간 | 요청 수 | RPS | p95 | max | 실패율 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `GET /api/posts` (목록, 랜덤 페이지) | 20 | 25s | 60,526 | 2,420 | 11.13ms | 33.28ms | 0% |
| `GET /api/posts/{id}` (상세) | 20 | 25s | 87,242 | 3,489 | 7.68ms | 25.42ms | 0% |
| `GET /api/posts?boardCategory=` (신규 분야 필터) | 20 | 25s | 61,952 | 2,477 | 10.91ms | 32.77ms | 0% |
| `POST /api/auth/login` (연타) | 5 | 15s | 141,736 | 9,449 | 0.70ms | 101.51ms | 99.99% (아래) |

## 판정

- **분야 필터(신규)가 무필터 목록과 동일 비용** — p95 10.91ms vs 11.13ms. `idx_posts_board_category`가 동작하며 V0006이 조회 성능을 해치지 않았다.
- **login 99.99% "실패"는 장애가 아니라 로그인 보호가 동작한 실측 증거다.** 분당 사용자 10회/IP 30회 제한(`auth.login.protection`)에 걸려 첫 10건 성공 후 전부 429. bcrypt 연산이 rate limit 뒤에 있어 무제한 연타로 CPU를 태울 수 없음이 확인됐다. p95 0.70ms는 429 응답의 비용이다.
- 조회 경로는 로컬에서 2,400~3,500 RPS — 현 규모에서 병목 없음. 다음 측정 포인트는 실 하드웨어(prod)에서의 동일 시나리오와, 뉴스 수집·다이제스트 생성이 겹치는 시간대의 커넥션 풀 사용률이다.

## 같은 날 수행한 E2E (수동)

회원 가입(SQL 시드)→로그인→글 작성→댓글→좋아요→상세 확인, 어제 날짜 뉴스 3건 시드→`POST /api/admin/news/digest`→DIGITAL 다이제스트 1건 생성(mock LLM 경유 전체 파이프라인)→재호출 시 `ALREADY_PUBLISHED` 멱등 확인→`GET /api/posts?category=DAILY_DIGEST` 노출 확인. 전 구간 통과.

주의: DB 컨테이너 `now()`는 UTC라 SQL 시드 시 KST 타임스탬프를 명시해야 앱(JVM KST)이 쓰는 날짜 범위와 일치한다.
