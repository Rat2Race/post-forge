package dev.iamrat.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 도메인은 환경변수로만 받는다. yml에 기본값을 두면 배포 서버에서 환경변수를 빠뜨려도
 * 앱이 그대로 떠서 localhost로 리다이렉트하고, 로컬 테스트로는 절대 잡히지 않는다.
 * 뒤에 경로를 붙여 쓰므로 끝 슬래시도 막는다.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.url")
public class AppUrlProperties {

    private static final String URL_PATTERN = "^https?://.*[^/]$";
    private static final String URL_MESSAGE = "http(s):// 로 시작하고 끝에 /가 없어야 합니다";

    @NotBlank(message = "APP_FRONTEND_BASE_URL이 필요합니다")
    @Pattern(regexp = URL_PATTERN, message = URL_MESSAGE)
    private String frontend;

    @NotBlank(message = "APP_API_BASE_URL이 필요합니다")
    @Pattern(regexp = URL_PATTERN, message = URL_MESSAGE)
    private String api;
}
