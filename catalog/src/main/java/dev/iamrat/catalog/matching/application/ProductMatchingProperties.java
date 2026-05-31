package dev.iamrat.catalog.matching.application;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "catalog.product-matching")
public class ProductMatchingProperties {

    private boolean enabled = true;

    private int topK = 5;

    private BigDecimal autoMatchThreshold = new BigDecimal("0.92");

    private BigDecimal candidateThreshold = new BigDecimal("0.75");
}
