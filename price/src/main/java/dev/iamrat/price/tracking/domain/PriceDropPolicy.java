package dev.iamrat.price.tracking.domain;

import java.math.BigDecimal;

public record PriceDropPolicy(BigDecimal minDropRate) {
    public static PriceDropPolicy defaultPolicy() {
        return new PriceDropPolicy(BigDecimal.valueOf(10));
    }
}
