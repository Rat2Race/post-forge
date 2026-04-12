# Crawl Stock Domain Guide

## 목적

이 문서는 `crawl` 모듈을 읽을 때 나오는 주식/공시/가격 관련 변수와 데이터가 왜 필요한지 설명한다.
주식을 잘 모르는 상태에서도 아래 네 가지를 이해할 수 있게 만드는 것이 목표다.

- 각 필드가 무엇을 의미하는지
- 그 필드가 왜 수집되는지
- 그 값이 어떤 원리로 후보 선정에 반영되는지
- `crawl -> internal ingest -> AI -> post` 흐름에서 어디에 쓰이는지

관련 구조 문서는 [crawl-redesign-architecture.md](./crawl-redesign-architecture.md)를 참고한다.

## 가장 짧은 요약

이 프로젝트의 `crawl` 모듈은 아래 세 종류의 신호를 모은다.

- 회사가 직접 발표한 사실: DART 공시
- 시장이 실제로 반응한 흔적: 가격/거래량
- 시장 참여자들이 관심을 보였는지: 뉴스

그리고 이 세 신호를 합쳐서
"지금 글로 만들 가치가 있는 종목인가"를 판단한다.

## 먼저 알아야 할 주식 원리

주식 글감을 뽑는다고 생각하면, 사실 세 가지 질문만 반복한다.

1. 회사에 실제로 무슨 일이 있었나
2. 시장이 그 일을 중요하게 봤나
3. 그 반응이 글로 설명할 만큼 충분히 뚜렷한가

이걸 코드에 대응시키면:

- 1번은 `DART 공시`, `재무 수치`
- 2번은 `가격`, `거래량`
- 3번은 `CandidateSelector`의 후보 선정 로직

즉, `crawl`은 예측 시스템이 아니라
"설명할 만한 이벤트를 찾는 시스템"에 더 가깝다.

## 이 모듈이 보는 시장 신호 3종

### 1. 회사 발표

출처:
- DART 공시

의미:
- 회사가 공식적으로 시장에 알린 사실
- 신뢰도는 높지만, 시장이 반드시 반응하는 것은 아니다

예시:
- 사업보고서
- 반기/분기보고서
- 유상증자
- 대규모 계약
- 임원/지분 변동

코드 위치:
- [DartDisclosureItem.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/dart/dto/DartDisclosureItem.java)
- [DartFinancialItem.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/dart/dto/DartFinancialItem.java)
- [DartCrawlService.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/dart/service/DartCrawlService.java)

### 2. 시장 반응

출처:
- KRX 가격/거래량 데이터

의미:
- 공시나 뉴스가 실제 매매를 일으켰는지 보여주는 신호
- 공시만으로는 "사실"만 알 수 있고,
  가격/거래량을 같이 봐야 "시장 영향"을 볼 수 있다

예시:
- 주가가 크게 올랐다
- 거래량이 평소보다 2배 이상 늘었다

코드 위치:
- [StockPrice.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/price/entity/StockPrice.java)
- [KrxPriceCrawlService.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/price/service/KrxPriceCrawlService.java)

### 3. 외부 관심도

출처:
- 네이버 뉴스

의미:
- 시장 참여자들이 그 종목을 많이 보고 있는지 보여주는 보조 신호
- 뉴스 자체가 팩트라기보다 "지금 시장에 노출되고 있나"를 보는 데 가깝다

코드 위치:
- [NaverNewsItem.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/news/dto/NaverNewsItem.java)
- [NaverNewsCrawlService.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/news/service/NaverNewsCrawlService.java)

## 가장 먼저 알아야 할 식별자

### `ticker`

의미:
- 거래소 종목코드
- 한국 주식에서 종목을 가장 자주 식별할 때 쓰는 값
- 예: `005930` 삼성전자

왜 필요한가:
- 가격 데이터 연결
- 공시와 종목 연결
- 후보 선정과 게시글 생성 요청의 기준 키

### `dartCorpCode`

의미:
- DART가 내부적으로 회사를 식별하는 코드

왜 필요한가:
- DART 재무 API는 종목코드가 아니라 회사코드를 요구하는 경우가 많다
- 같은 회사의 공시/재무 데이터를 정확히 묶기 위해 필요하다

### `stockName` / `corpName`

의미:
- 사람이 읽는 회사명

왜 필요한가:
- 뉴스 검색
- 수집 문서 메타데이터
- 최종 게시글 제목/본문 문맥

## 엔티티별로 보면 쉬운 구조

### `StockMaster`: 이 회사가 누구인가

파일:
- [StockMaster.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/stock/entity/StockMaster.java)

핵심 필드:
- `ticker`: 종목코드
- `name`: 종목명
- `market`: KOSPI / KOSDAQ
- `marketCap`: 시가총액
- `dartCorpCode`: DART 회사코드
- `isEtf`, `isPreferred`, `isSpac`, `isSuspended`: 후보 제외 필터

왜 필요한가:
- 어떤 종목을 후보군에 넣을지 정하는 기준 테이블이다
- ETF, 우선주, 스팩, 거래정지 종목은 일반 기업 분석 글감으로는 노이즈가 많아서 제외한다

### `CrawledArticle`: 공시/뉴스 수집 이력

파일:
- [CrawledArticle.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/common/entity/CrawledArticle.java)

핵심 필드:
- `originalLink`: 중복 제거 기준
- `title`: 사람이 읽을 제목
- `source`: `dart`, `naver-news` 같은 출처
- `keyword`: 종목코드 또는 검색 키워드
- `crawledAt`: 언제 수집했는지
- `publishedAt`: 원문이 공개된 시점

왜 필요한가:
- 같은 기사를 두 번 넣지 않기 위해
- 최근 몇 시간 안에 공시/뉴스가 몇 건 있었는지 후보 선정에 쓰기 위해

### `StockPrice`: 오늘 시장이 어떻게 반응했나

파일:
- [StockPrice.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/price/entity/StockPrice.java)

핵심 필드:
- `openPrice`, `highPrice`, `lowPrice`, `closePrice`
- `volume`
- `changeRate`
- `avgVolume20d`
- `foreignNetBuy`, `instNetBuy`

왜 필요한가:
- 가격이 움직였는지
- 거래가 평소보다 몰렸는지
- 나중에 외국인/기관 수급까지 붙이면 더 강한 신호를 만들 수 있는지

### `CandidateSelection`: 글감 후보 요약

파일:
- [CandidateSelection.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/entity/CandidateSelection.java)

핵심 필드:
- `ticker`, `stockName`
- `reason`
- `priceChange`
- `volumeRatio`
- `disclosureCount`
- `newsCount`

왜 필요한가:
- 수집 결과를 바로 글로 만들지 않고, 먼저 "오늘 주목할 종목" 후보를 뽑아두기 위해
- 나중에 게시글 생성 요청을 걸 때 우선순위나 일일 제한을 적용하기 쉽게 하기 위해

## DART 쪽 변수는 왜 많은가

주식을 잘 모르면 DART 관련 값이 제일 낯설다.
하지만 역할은 생각보다 단순하다.

### `reportNm`

의미:
- 보고서 이름
- 예: 사업보고서, 반기보고서, 분기보고서, 주요사항보고서

왜 중요한가:
- 어떤 종류의 이벤트인지 판별하는 첫 신호다
- 정기공시인지, 이벤트성 공시인지 구분할 수 있다

### `rceptNo`

의미:
- DART 접수번호

왜 중요한가:
- 사실상 공시 문서의 고유 ID 역할
- 중복 제거 링크 생성에 쓰인다

### `rceptDt`

의미:
- 접수일

왜 중요한가:
- 최근 공시인지 판단할 때 필요
- 문서 메타데이터와 게시글 출처 표시에 사용 가능

### `corpCode`

의미:
- DART 회사코드

왜 중요한가:
- 재무 수치 API를 다시 호출할 때 필요

### `stockCode`

의미:
- 거래소 종목코드

왜 중요한가:
- 가격 데이터, 후보 선정, 게시글 생성 요청과 연결하는 기본 키

### `accountNm`, `thstrmAmount`, `frmtrmAmount`

의미:
- `accountNm`: 어떤 재무 항목인지. 예: 매출액, 영업이익
- `thstrmAmount`: 이번 기간 값
- `frmtrmAmount`: 이전 기간 값

왜 중요한가:
- 숫자 자체보다 "전기 대비 좋아졌는가/나빠졌는가"를 보기 위해 필요
- 게시글 생성 시 실적 해설의 핵심 재료가 된다

### `reprtCode`

의미:
- 사업보고서 / 반기 / 분기 같은 보고서 종류를 나타내는 코드

왜 중요한가:
- 같은 회사라도 어떤 기간의 숫자를 읽는지 정확히 맞추기 위해 필요

### `fsDiv`

의미:
- 연결재무제표(`CFS`)인지 별도재무제표(`OFS`)인지

왜 중요한가:
- 보통 그룹 전체 기준 분석이 더 자연스러워서 연결재무제표를 우선 사용한다

## 가격 데이터는 왜 필요한가

공시만 보면 "회사가 발표한 사실"만 알 수 있다.
하지만 글감 후보로는 그 사실이 시장에 반응을 일으켰는지도 중요하다.

### `changeRate`

의미:
- 오늘 주가가 몇 퍼센트 움직였는지

원리:
- 큰 공시가 나와도 주가 반응이 없으면 시장 영향이 작을 수 있다
- 반대로 큰 등락이 있으면 글감 가치가 높아진다

### `volume`

의미:
- 오늘 거래된 주식 수량

원리:
- 거래량은 관심도와 매매 강도를 본다
- 평소보다 거래량이 갑자기 많아지면 시장이 그 종목에 몰렸다는 뜻일 수 있다

### `avgVolume20d`

의미:
- 최근 20거래일 평균 거래량

원리:
- 절대 거래량보다 "평소 대비 얼마나 튀었나"가 더 중요하다
- 10만 주 거래는 어떤 종목엔 크고, 어떤 종목엔 작다

### `volumeRatio`

의미:
- 오늘 거래량이 평균 대비 몇 퍼센트/몇 배인지

현재 코드 원리:
- [CandidateSelector.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/service/CandidateSelector.java)에서
  `volume / avgVolume20d * 100` 으로 계산한다

해석:
- 100% = 평소 수준
- 200% = 평소의 2배
- 300% = 평소의 3배

## 왜 뉴스까지 보는가

뉴스는 DART처럼 공식 공시는 아니지만, 현재 시장 주목도를 확인하는 데 유용하다.

원리:
- 공시는 중요하지만 시장이 무시할 수 있다
- 뉴스는 공식 공시보다 신뢰도는 낮지만, 관심도 측정에는 도움이 된다
- 공시 + 뉴스 + 거래량 급증이 함께 나오면 글감 가치가 커진다

즉:
- DART는 사실 확인
- 뉴스는 관심도 확인
- 가격/거래량은 반응 확인

## 후보 선정은 어떤 원리로 돌아가나

파일:
- [CandidateSelector.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/service/CandidateSelector.java)

핵심 아이디어:
- "회사 발표가 있었는가"
- "뉴스가 붙었는가"
- "가격이 크게 움직였는가"
- "거래량이 평소보다 많이 늘었는가"

이 네 가지 중 의미 있는 게 있으면 후보로 본다.

### 실제 메서드 기준

- `countArticles("dart", ticker, window)`
  - 최근 공시 개수
- `countNewsArticles(stockName, window)`
  - 최근 뉴스 개수
- `priceChange`
  - 최신 가격 등락률
- `calculateVolumeRatio(latestPrice)`
  - 거래량 급증 여부
- `buildReasons(...)`
  - 위 조건을 사람이 읽는 이유 문장으로 변환

### 이 방식의 장점

- 너무 많은 종목을 한꺼번에 글감으로 뽑지 않는다
- "실제 이벤트가 있었고 시장도 반응한 종목" 위주로 추릴 수 있다
- AI가 의미 없는 종목까지 분석하지 않게 막을 수 있다

### 이 방식의 한계

- 등락률이 작아도 중요한 공시는 놓칠 수 있다
- 뉴스가 적어도 중요한 기업 이벤트는 존재할 수 있다
- 거래량만 튄다고 항상 좋은 글감은 아니다

즉, 후보 선정은 정답 시스템이 아니라
"노이즈를 줄이는 1차 필터"라고 보는 편이 맞다.

## 설정값은 왜 필요한가

파일:
- [CandidateConfig.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/config/CandidateConfig.java)

- `disclosureLookbackHours`
  - 최근 몇 시간의 이벤트를 볼지
- `priceChangeThreshold`
  - 몇 퍼센트 이상 움직여야 의미 있다고 볼지
- `volumeRatioThreshold`
  - 평소보다 거래량이 몇 배 이상이어야 의미 있다고 볼지

이 값들은 주식 지식 그 자체라기보다,
"후보를 너무 많이 뽑지 않기 위한 운영 기준"이다.

## 왜 ETF, 우선주, 스팩은 제외하나

현재 `StockMaster`와 조회 조건에서 제외하는 이유는 아래와 같다.

- ETF
  - 개별 기업 이벤트보다 지수/섹터 묶음 영향이 커서 공시 기반 글감과 잘 안 맞음
- 우선주
  - 보통주와 가격 흐름이 다를 수 있고 거래량이 얇을 수 있음
- 스팩
  - 일반 기업 실적/공시 해석과 결이 다름
- 거래정지 종목
  - 시장 반응 신호를 보기 어려움

즉 "개별 기업 분석 글감"에 맞는 대상을 남기기 위한 필터다.

## 현재 데이터가 메인 앱까지 가는 원리

지금 구조에서 `crawl`은 수집한 데이터를 직접 분석하지 않는다.
흐름은 아래와 같다.

1. 수집
2. 중복 제거
3. 로컬 저장
4. 메인 앱 internal API 전송
5. 메인 앱이 문서를 저장
6. 후보 종목을 골라 게시글 생성 요청

코드 위치:
- [AiDocumentSender.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/common/AiDocumentSender.java)
- [CandidatePostPublisher.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/pipeline/service/CandidatePostPublisher.java)
- [InternalCrawlController.java](/home/rat/spring-workspace/post-forge/ai/src/main/java/dev/iamrat/ai/internal/controller/InternalCrawlController.java)

왜 이렇게 하냐면:
- 수집과 분석 책임을 분리하기 위해
- 저장 후 전송으로 장애 복구를 쉽게 하기 위해
- 같은 데이터로 여러 종류의 분석을 나중에 다시 할 수 있게 하기 위해

## 처음 읽을 때 추천 순서

주식 도메인을 모르겠다면 아래 순서로 보면 제일 덜 헷갈린다.

1. [StockMaster.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/stock/entity/StockMaster.java)
   - 어떤 종목을 다루는지
2. [CrawledArticle.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/common/entity/CrawledArticle.java)
   - 공시/뉴스 이력이 어떻게 남는지
3. [StockPrice.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/price/entity/StockPrice.java)
   - 시장 반응 데이터가 뭔지
4. [CandidateSelection.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/entity/CandidateSelection.java)
   - 최종 후보가 어떤 형태인지
5. [CandidateSelector.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/candidate/service/CandidateSelector.java)
   - 왜 후보가 되는지
6. [DartCrawlService.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/dart/service/DartCrawlService.java)
   - 공시 쪽 세부 수집
7. [KrxPriceCrawlService.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/price/service/KrxPriceCrawlService.java)
   - 가격 쪽 세부 수집
8. [CandidatePostPublisher.java](/home/rat/spring-workspace/post-forge/crawl/src/main/java/dev/iamrat/crawl/pipeline/service/CandidatePostPublisher.java)
   - 실제 게시 생성 요청이 언제 나가는지

## 지금 당장 헷갈리지 않아도 되는 것

처음엔 아래까지 완벽히 알 필요는 없다.

- `reprtCode` 세부 숫자 의미
- `corpCls` 세부 분류
- `fsNm`, `sjDiv`, `sjNm` 같은 DART 응답 상세 필드
- `foreignNetBuy`, `instNetBuy`의 실전 활용 방식

이 값들은 확장할 때 중요하고, 현재 후보 선정 핵심은 아니다.

## 앞으로 변수 정리 시 추천 기준

변수가 많아 보여도 아래 4카테고리로 나누면 된다.

- 종목 식별 변수
- 공시/재무 변수
- 가격/거래량 변수
- 후보 선정 변수

이 기준으로 네이밍과 문서를 맞추면 주식 도메인을 몰라도 코드가 훨씬 읽기 쉬워진다.
