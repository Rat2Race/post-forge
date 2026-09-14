package dev.iamrat.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * URL은 app.url.frontend / app.url.api 두 개에서만 파생돼야 한다.
 * 도메인과 경로를 env 값 하나에 합쳐두면 endpoint를 옮길 때 gitignore된 .env가
 * diff에 안 잡혀 링크가 조용히 깨진다.
 */
@SpringBootTest(properties = "spring.config.import=optional:classpath:application-monitoring.yml")
@ActiveProfiles("test")
class UrlPropertyCompositionTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("프론트로 보내는 URL은 전부 app.url.frontend에서 파생된다")
    void frontendFacingUrlsDeriveFromFrontendBase() {
        String frontend = environment.getProperty("app.url.frontend");
        assertThat(frontend).isNotBlank();

        assertThat(environment.getProperty("app.oauth2.redirect-url"))
            .as("OAuth2 리다이렉트가 프론트 도메인 밖으로 나가면 콜백을 받을 수 없다")
            .startsWith(frontend);
        assertThat(environment.getProperty("app.email.verification-base-url"))
            .as("이메일 인증 링크가 프론트 도메인 밖으로 나가면 메일 링크가 깨진다")
            .startsWith(frontend);
    }

    @Test
    @DisplayName("OAuth2 provider 콜백 URI는 전부 app.url.api에서 파생된다")
    void providerCallbackUrisDeriveFromApiBase() {
        String api = environment.getProperty("app.url.api");
        assertThat(api).isNotBlank();

        for (String registration : List.of("google", "naver", "kakao")) {
            assertThat(environment.getProperty(
                "spring.security.oauth2.client.registration." + registration + ".redirect-uri"))
                .as("%s 콜백 URI가 API 도메인과 어긋나면 provider 콘솔 등록값과 불일치한다", registration)
                .isEqualTo(api + "/login/oauth2/code/" + registration);
        }
    }
}
