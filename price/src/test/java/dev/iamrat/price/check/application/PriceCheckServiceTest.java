package dev.iamrat.price.check.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.application.SourceRequestExecutor;
import dev.iamrat.source.product.domain.SourceType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriceCheckServiceTest {

    @Test
    @DisplayName("최종 결제 가격이 있으면 해당 가격을 사용한다")
    void usesFinalPaidPriceWhenProvided() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L, 110_000L, 120_000L));

        PriceCheckResult result = service.check(new PriceCheckCommand(
            "무선 이어폰",
            null,
            999_999L,
            999_999L,
            0L,
            90_000L
        ));

        assertThat(result.candidateEffectivePrice()).isEqualTo(90_000L);
        assertThat(result.judgement()).isEqualTo(PriceJudgement.CHEAP);
    }

    @Test
    @DisplayName("기본가, 배송비, 할인으로 실효 가격을 계산한다")
    void computesEffectivePriceFromBaseShippingAndDiscount() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L, 110_000L, 120_000L));

        PriceCheckResult result = service.check(new PriceCheckCommand(
            "무선 이어폰",
            null,
            100_000L,
            3_000L,
            10_000L,
            null
        ));

        assertThat(result.candidateEffectivePrice()).isEqualTo(93_000L);
    }

    @Test
    @DisplayName("음수 가격 입력은 거절한다")
    void rejectsNegativePriceInputs() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L));

        assertThatThrownBy(() -> service.check(new PriceCheckCommand(
            "무선 이어폰",
            null,
            -1L,
            null,
            null,
            null
        ))).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("유효하지 않은 네이버 가격 샘플은 제외한다")
    void filtersInvalidNaverPrices() {
        PriceCheckService service = new PriceCheckService(query -> new ProductSourceResult(List.of(
            item("bad", 0L),
            item("valid", 100_000L)
        )));

        PriceCheckResult result = service.check(command(80_000L));

        assertThat(result.basis().sampleSize()).isEqualTo(1);
        assertThat(result.items()).extracting(PriceCheckSampleItem::price).containsExactly(100_000L);
    }

    @Test
    @DisplayName("비교 샘플이 비어 있으면 정보 부족 판단을 반환한다")
    void returnsInsufficientInfoForEmptyComparisonSample() {
        PriceCheckService service = serviceWithPrices(List.of());

        PriceCheckResult result = service.check(command(100_000L));

        assertThat(result.judgement()).isEqualTo(PriceJudgement.INSUFFICIENT_INFO);
        assertThat(result.confidence()).isEqualTo(PriceCheckConfidence.LOW);
        assertThat(result.basis().sampleSize()).isZero();
    }

    @Test
    @DisplayName("중앙값 기준으로 저렴, 보통, 비쌈을 판단한다")
    void judgesCheapNormalAndExpensiveAgainstMedianThresholds() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L, 110_000L, 120_000L));

        assertThat(service.check(command(90_000L)).judgement()).isEqualTo(PriceJudgement.CHEAP);
        assertThat(service.check(command(110_000L)).judgement()).isEqualTo(PriceJudgement.NORMAL);
        assertThat(service.check(command(130_000L)).judgement()).isEqualTo(PriceJudgement.EXPENSIVE);
    }

    @Test
    @DisplayName("배송비가 검증되지 않으면 신뢰도를 낮추고 경고한다")
    void degradesConfidenceAndWarnsWhenShippingIsUnverified() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L, 110_000L, 120_000L));

        PriceCheckResult result = service.check(command(90_000L));

        assertThat(result.confidence()).isEqualTo(PriceCheckConfidence.LOW);
        assertThat(result.shippingIncludedVerified()).isFalse();
        assertThat(result.message()).contains("배송비 포함 여부를 확인하지 못해");
    }

    @Test
    @DisplayName("미검증 배송비가 판단을 뒤집을 수 있으면 정보 부족을 반환한다")
    void returnsInsufficientInfoWhenUnverifiedShippingCanReverseDecision() {
        PriceCheckService service = serviceWithPrices(List.of(100_000L, 110_000L, 120_000L));

        PriceCheckResult result = service.check(command(103_000L));

        assertThat(result.judgement()).isEqualTo(PriceJudgement.INSUFFICIENT_INFO);
        assertThat(result.confidence()).isEqualTo(PriceCheckConfidence.LOW);
    }

    @Test
    @DisplayName("가격 비교는 네이버 소스를 먼저 검색한다")
    void searchesNaverSourceFirst() {
        List<ProductSourceQuery> queries = new ArrayList<>();
        SourceRequestExecutor executor = query -> {
            queries.add(query);
            return new ProductSourceResult(List.of(item("sample", 100_000L)));
        };

        new PriceCheckService(executor).check(command(80_000L));

        assertThat(queries).hasSize(1);
        assertThat(queries.getFirst().source()).isEqualTo(SourceType.NAVER);
    }

    @Test
    @DisplayName("종료된 네이버 쇼핑 소스는 외부 서비스 장애로 변환한다")
    void reportsRetiredNaverShoppingAsUnavailable() {
        PriceCheckService service = new PriceCheckService(query -> {
            throw new UnsupportedOperationException("retired");
        });

        assertThatThrownBy(() -> service.check(command(80_000L)))
            .isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE)
            );
    }

    private PriceCheckCommand command(Long basePrice) {
        return new PriceCheckCommand("무선 이어폰", null, basePrice, null, null, null);
    }

    private PriceCheckService serviceWithPrices(List<Long> prices) {
        return new PriceCheckService(query -> new ProductSourceResult(prices.stream()
            .map(price -> item("item-" + price, price))
            .toList()));
    }

    private ProductSourceItem item(String id, Long price) {
        return new ProductSourceItem(
            id,
            "무선 이어폰 " + id,
            "brand",
            "maker",
            "digital",
            "audio",
            "earbuds",
            price,
            "https://image.example/" + id,
            "https://shop.example/" + id,
            "예시몰"
        );
    }
}
