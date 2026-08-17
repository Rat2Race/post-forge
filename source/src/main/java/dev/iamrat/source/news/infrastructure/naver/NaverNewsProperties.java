package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.infrastructure.naver.AbstractNaverSearchProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "source.naver-news")
public class NaverNewsProperties extends AbstractNaverSearchProperties {

    @Getter
    private int display = 10;

    public NaverNewsProperties() {
        super("date");
    }

    public void setDisplay(int display) {
        this.display = display <= 0 ? 10 : Math.min(display, 100);
    }

    boolean credentialsConfigured() {
        return hasCredentialsConfigured();
    }

    @AssertTrue(message = "NAVER_NEWS_ENABLED=true일 때 NAVER_API_HUB_API_KEY_ID/NAVER_API_HUB_API_KEY 설정이 필요합니다")
    public boolean isCredentialsValidForEnabledSource() {
        return !isEnabled() || credentialsConfigured();
    }
}
