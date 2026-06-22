# N+1 분석

## 요약

`GET /posts`는 로그인 BCrypt CPU 병목을 제거한 뒤에도 다른 읽기 API보다 느렸다.
디버그 SQL 로그에서 요청 1건당 추가 쿼리 11개가 확인되어 N+1 병목으로 분리했다.

## 기준 신호

출처: 과거 raw 리포트에서 추린 historical summary. 원문 산출물은 `docs/performance/k6/`, `docs/performance/grafana/`, `docs/performance/manual-runs/`에 보관한다.

| 시나리오 | `GET /posts` med | `GET /posts` p95 | 시스템 신호 |
| --- | ---: | ---: | --- |
| 로그인 혼합 30 VU | 498ms | 1.89s | CPU 100%, Load 12.0 |
| 읽기 25 VU + 로그인 5 VU | 90ms | 508ms | CPU max 59%, Load 0.7 |

시나리오를 분리하자 CPU 포화는 해소되었지만, `GET /posts`는 `GET /posts/{id}`나 `GET /posts/{id}/comments`보다 계속 느렸다.
CPU가 여유 있는 상태에서 특정 조회 API만 느리면 DB 쿼리 수와 fetch 전략을 우선 확인한다.

## 근거

- `org.hibernate.SQL: DEBUG` 로그에서 `GET /posts` 요청 1건당 추가 쿼리 11개가 관측되었다.
- 동일 시나리오에서 단건 조회와 댓글 조회는 p95 189-205ms까지 내려갔지만 목록 조회는 p95 508ms였다.
- N+1 개선 후 `GET /posts` p95는 195.83ms로 내려갔다.
- batch size 설정 후 `GET /posts` p95는 104.47ms로 추가 개선되었다.

## 재검증 결과

| 단계 | checks | 전체 p95 | `GET /posts` p95 | `GET /posts/{id}` p95 | 로그인 p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| N+1 해결 후 | 6042/6042 | 285.04ms | 195.83ms | 191.04ms | 1.40s |
| batch size 설정 후 | 7416/7416 | 180.75ms | 104.47ms | 97.98ms | 864.34ms |

## 점검 기준

목록 조회를 수정할 때는 다음을 같이 확인한다.

- 목록 응답 DTO가 참조하는 연관 엔티티 수
- `@EntityGraph`, fetch join, batch size 적용 범위
- pagination과 fetch join 조합의 부작용
- 로그인/토큰 재발급 같은 CPU-heavy endpoint가 섞인 시나리오인지 여부
- k6 p95뿐 아니라 SQL count, RPS, CPU/Load를 함께 비교

## 원본 아티팩트 보존 정책

과거 k6/Bruno/Grafana raw 산출물은 정량 근거이므로 삭제하지 않는다.
이 문서는 N+1 판단에 필요한 historical 숫자와 점검 기준을 원문 산출물 위에 얹은 요약 문서다.
