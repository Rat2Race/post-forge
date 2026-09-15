package dev.iamrat.auth.token.presentation;

import dev.iamrat.auth.token.application.TokenLifetimeSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieProviderTest {

    private final CookieProvider cookieProvider = new CookieProvider(refreshTokenValidityDays(7));

    @Test
    @DisplayName("refresh token 쿠키는 /api/auth 경로에 추가한다")
    void addRefreshTokenCookie_usesApiAuthPath() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieProvider.addRefreshTokenCookie(response, "refresh-token");

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains("refresh_token=refresh-token")
            .contains("Path=/api/auth")
            .contains("Max-Age=604800")
            .contains("HttpOnly")
            .contains("Secure")
            .contains("SameSite=Lax");
    }

    @Test
    @DisplayName("refresh token 쿠키 삭제도 /api/auth 경로를 사용한다")
    void removeRefreshTokenCookie_usesApiAuthPath() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieProvider.removeRefreshTokenCookie(response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains("refresh_token=")
            .contains("Path=/api/auth")
            .contains("Max-Age=0")
            .contains("HttpOnly")
            .contains("Secure")
            .contains("SameSite=Lax");
    }

    private static TokenLifetimeSettings refreshTokenValidityDays(long days) {
        return () -> days;
    }
}
