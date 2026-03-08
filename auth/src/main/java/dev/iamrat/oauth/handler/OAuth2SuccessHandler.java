package dev.iamrat.oauth.handler;

import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import dev.iamrat.token.provider.JwtProvider;
import dev.iamrat.token.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final JwtService jwtService;
    private final CookieProvider cookieProvider;

    @Value("${spring.cors.allowed-origins:${cors.allowed-origins}}")
    private String allowedOrigins;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        JwtResponse jwtResponse = jwtProvider.createToken(authentication);

        log.info("OAuth2 로그인 성공, JWT 발급: userId={}", jwtService.parseClaims(jwtResponse.accessToken()).getId());

        cookieProvider.addRefreshTokenCookie(response, jwtResponse.refreshToken());

        String redirectUrl = List.of(allowedOrigins.split(",")).getFirst()
            + "/oauth2/callback"
            + "?accessToken=" + jwtResponse.accessToken();

        response.sendRedirect(redirectUrl);
    }
}