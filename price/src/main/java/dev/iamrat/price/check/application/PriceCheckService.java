package dev.iamrat.price.check.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.SourceRequestExecutor;
import dev.iamrat.source.product.domain.SourceType;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceCheckService {

    private static final String PROVIDER = "NAVER";
    private static final int SAMPLE_SIZE = 10;
    private static final double LOW_THRESHOLD_RATIO = 0.95D;
    private static final double HIGH_THRESHOLD_RATIO = 1.05D;
    private static final long SHIPPING_UNCERTAINTY_BUFFER = 3_000L;
    private static final String SHIPPING_WARNING =
        "배송비 포함 여부를 확인하지 못해 낮은 확신도로 판정했어요.";

    private final SourceRequestExecutor sourceRequestExecutor;

    public PriceCheckResult check(PriceCheckCommand command) {
        String keyword = normalizeKeyword(command.keyword());
        long effectivePrice = effectivePrice(command);
        List<PriceCheckSampleItem> samples = sourceRequestExecutor
            .search(new ProductSourceQuery(SourceType.NAVER, keyword, SAMPLE_SIZE))
            .items()
            .stream()
            .filter(item -> item.price() != null && item.price() > 0)
            .sorted(Comparator.comparingLong(ProductSourceItem::price))
            .map(this::toSample)
            .toList();

        if (samples.isEmpty()) {
            return insufficient(effectivePrice, new PriceCheckBasis(PROVIDER, 0, 0L, 0L, 0L), samples);
        }

        long median = median(samples);
        long lowThreshold = Math.round(median * LOW_THRESHOLD_RATIO);
        long highThreshold = Math.round(median * HIGH_THRESHOLD_RATIO);
        PriceCheckBasis basis = new PriceCheckBasis(PROVIDER, samples.size(), median, lowThreshold, highThreshold);

        PriceJudgement judgement = judge(effectivePrice, lowThreshold, highThreshold);
        if (couldReverseWithUnverifiedShipping(effectivePrice, lowThreshold, highThreshold)) {
            return insufficient(effectivePrice, basis, samples);
        }
        return new PriceCheckResult(
            judgement,
            PriceCheckConfidence.LOW,
            effectivePrice,
            false,
            SHIPPING_WARNING,
            basis,
            samples
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(
                CommonErrorCode.INVALID_INPUT,
                PriceCheckExceptionMessages.KEYWORD_MUST_NOT_BE_BLANK
            );
        }
        return keyword.trim();
    }

    private long effectivePrice(PriceCheckCommand command) {
        requireNonNegative(command.basePrice(), "basePrice");
        requireNonNegative(command.shippingFee(), "shippingFee");
        requireNonNegative(command.discountAmount(), "discountAmount");
        requireNonNegative(command.finalPaidPrice(), "finalPaidPrice");

        if (command.finalPaidPrice() != null) {
            return command.finalPaidPrice();
        }
        if (command.basePrice() == null) {
            throw new CustomException(
                CommonErrorCode.INVALID_INPUT,
                PriceCheckExceptionMessages.BASE_PRICE_REQUIRED_UNLESS_FINAL_PAID_PRICE_PROVIDED
            );
        }
        long shipping = command.shippingFee() == null ? 0L : command.shippingFee();
        long discount = command.discountAmount() == null ? 0L : command.discountAmount();
        long effectivePrice = command.basePrice() + shipping - discount;
        if (effectivePrice < 0) {
            throw new CustomException(
                CommonErrorCode.INVALID_INPUT,
                PriceCheckExceptionMessages.CANDIDATE_EFFECTIVE_PRICE_MUST_NOT_BE_NEGATIVE
            );
        }
        return effectivePrice;
    }

    private void requireNonNegative(Long value, String field) {
        if (value != null && value < 0) {
            throw new CustomException(
                CommonErrorCode.INVALID_INPUT,
                PriceCheckExceptionMessages.mustNotBeNegative(field)
            );
        }
    }

    private PriceCheckSampleItem toSample(ProductSourceItem item) {
        return new PriceCheckSampleItem(
            item.title(),
            item.mallName(),
            item.price(),
            item.productUrl()
        );
    }

    private long median(List<PriceCheckSampleItem> samples) {
        int size = samples.size();
        if (size % 2 == 1) {
            return samples.get(size / 2).price();
        }
        return Math.round((samples.get(size / 2 - 1).price() + samples.get(size / 2).price()) / 2.0D);
    }

    private PriceJudgement judge(long effectivePrice, long lowThreshold, long highThreshold) {
        if (effectivePrice < lowThreshold) {
            return PriceJudgement.CHEAP;
        }
        if (effectivePrice > highThreshold) {
            return PriceJudgement.EXPENSIVE;
        }
        return PriceJudgement.NORMAL;
    }

    private boolean couldReverseWithUnverifiedShipping(long effectivePrice, long lowThreshold, long highThreshold) {
        return Math.abs(effectivePrice - lowThreshold) <= SHIPPING_UNCERTAINTY_BUFFER
            || Math.abs(effectivePrice - highThreshold) <= SHIPPING_UNCERTAINTY_BUFFER;
    }

    private PriceCheckResult insufficient(
        long effectivePrice,
        PriceCheckBasis basis,
        List<PriceCheckSampleItem> samples
    ) {
        return new PriceCheckResult(
            PriceJudgement.INSUFFICIENT_INFO,
            PriceCheckConfidence.LOW,
            effectivePrice,
            false,
            SHIPPING_WARNING,
            basis,
            samples
        );
    }
}
