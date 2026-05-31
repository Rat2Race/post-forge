package dev.iamrat.ai.autopost.application;

import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.catalog.product.application.ProductService;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.core.board.post.AutoPriceDropPostWriteCommand;
import dev.iamrat.core.board.post.AutoPriceDropPostWriteResult;
import dev.iamrat.core.board.post.AutoPriceDropPostWriter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoPriceDropPostService {

    private static final String SYSTEM_PROMPT = """
        You write concise Korean shopping community posts.
        Focus only on observed price data. Do not invent coupons, stock, or product quality claims.
        """;

    private final ProductService productService;
    private final TextGenerationClient textGenerationClient;
    private final AutoPriceDropPostWriter postWriter;
    private final MeterRegistry meterRegistry;

    public AutoPriceDropPostWriteResult createPriceDropPost(String eventId, PriceDropDetectedPayload payload) {
        counter("ai_post_requested_total").increment();
        try {
            Product product = productService.getById(payload.productId());
            String title = truncate("%s 최저가 %s%% 하락".formatted(product.getName(), formatRate(payload.dropRate())), 100);
            String summary = "%s원에서 %s원으로 하락했습니다.".formatted(
                formatPrice(payload.previousPrice()),
                formatPrice(payload.currentPrice())
            );
            String content = generateContent(product, payload);

            AutoPriceDropPostWriteResult result = postWriter.writeAutoPriceDropPost(new AutoPriceDropPostWriteCommand(
                product.getId(),
                eventId,
                postDate(payload),
                title,
                content,
                summary,
                tags(product),
                true
            ));
            counter("ai_post_created_total").increment();
            if (result.published()) {
                counter("auto_post_published_total").increment();
            }
            return result;
        } catch (RuntimeException e) {
            counter("ai_post_failed_total").increment();
            throw e;
        }
    }

    private String generateContent(Product product, PriceDropDetectedPayload payload) {
        String generated = textGenerationClient.generate(SYSTEM_PROMPT, userPrompt(product, payload));
        if (generated == null || generated.isBlank()) {
            return fallbackContent(product, payload);
        }
        return generated.trim();
    }

    private String userPrompt(Product product, PriceDropDetectedPayload payload) {
        return """
            상품명: %s
            브랜드: %s
            카테고리: %s > %s > %s
            직전 최저가: %s원
            현재 최저가: %s원
            하락률: %s%%
            감지 기준: %s
            감지 시각: %s

            가격 하락 사실, 확인할 점, 구매 전 주의사항을 3문단 이내로 작성해줘.
            """.formatted(
            product.getName(),
            blankToDash(product.getBrand()),
            blankToDash(product.getCategory1()),
            blankToDash(product.getCategory2()),
            blankToDash(product.getCategory3()),
            formatPrice(payload.previousPrice()),
            formatPrice(payload.currentPrice()),
            formatRate(payload.dropRate()),
            payload.detectionRule(),
            payload.detectedAt()
        );
    }

    private String fallbackContent(Product product, PriceDropDetectedPayload payload) {
        return """
            %s의 최근 수집 기준 최저가가 하락했습니다.

            직전 최저가는 %s원이었고, 현재 최저가는 %s원입니다. 하락률은 %s%%입니다.

            판매처, 배송비, 옵션 구성이 달라질 수 있으니 구매 전 상세 조건을 다시 확인하세요.
            """.formatted(
            product.getName(),
            formatPrice(payload.previousPrice()),
            formatPrice(payload.currentPrice()),
            formatRate(payload.dropRate())
        );
    }

    private List<String> tags(Product product) {
        return List.of("가격하락", blankToDash(product.getBrand()), blankToDash(product.getCategory3())).stream()
            .filter(tag -> !"-".equals(tag))
            .distinct()
            .limit(5)
            .toList();
    }

    private LocalDate postDate(PriceDropDetectedPayload payload) {
        return payload.detectedAt() == null ? LocalDate.now() : payload.detectedAt().toLocalDate();
    }

    private String formatPrice(Long value) {
        if (value == null) {
            return "-";
        }
        return NumberFormat.getNumberInstance(Locale.KOREA).format(value);
    }

    private String formatRate(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private Counter counter(String name) {
        return Counter.builder(name).register(meterRegistry);
    }
}
