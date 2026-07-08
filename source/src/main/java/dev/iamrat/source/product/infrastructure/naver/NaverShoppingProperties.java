package dev.iamrat.source.product.infrastructure.naver;

import dev.iamrat.source.infrastructure.naver.AbstractNaverSearchProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "source.naver-shopping")
public class NaverShoppingProperties extends AbstractNaverSearchProperties {

    @Getter
    private String exclude = "used:rental:cbshop";

    public NaverShoppingProperties() {
        super("sim");
    }

    public void setExclude(String exclude) {
        this.exclude = blankToDefault(exclude, "used:rental:cbshop");
    }

    boolean credentialsConfigured() {
        return hasCredentialsConfigured();
    }

    @AssertTrue(message = "NAVER_SHOPPING_ENABLED=true일 때 NAVER_SEARCH_CLIENT_ID/NAVER_SEARCH_CLIENT_SECRET 설정이 필요합니다")
    public boolean isCredentialsValidForEnabledSource() {
        return !isEnabled() || credentialsConfigured();
    }
}
