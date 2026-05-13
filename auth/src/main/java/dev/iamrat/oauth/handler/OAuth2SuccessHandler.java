package dev.iamrat.oauth.handler;

import dev.iamrat.global.security.UserPrincipal;
import dev.iamrat.oauth.service.OAuth2CodeService;
import dev.iamrat.security.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final OAuth2CodeService oAuth2CodeService;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            log.info("OAuth2 로그인 성공: userId={}", principal.getUserId());

            String code = oAuth2CodeService.createCode(principal.getUserId());

            String redirectUrl = String.format(
                    "%s?code=%s",
                    appProperties.getOauth2().getRedirectUrl(),
                    code
            );

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 실패", e);
            String errorRedirect = String.format("%s?error=%s",
                    appProperties.getOauth2().getRedirectUrl(),
                    URLEncoder.encode("로그인 처리에 실패했습니다.", StandardCharsets.UTF_8));
            response.sendRedirect(errorRedirect);
        }
    }
}
