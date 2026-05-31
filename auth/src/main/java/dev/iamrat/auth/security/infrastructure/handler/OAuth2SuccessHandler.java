package dev.iamrat.auth.security.infrastructure.handler;

import dev.iamrat.auth.oauth.application.OAuth2CodeService;
import dev.iamrat.core.account.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final OAuth2CodeService oAuth2CodeService;
    private final OAuth2RedirectProperties oAuth2RedirectProperties;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            log.info("OAuth2 로그인 성공: accountId={}", principal.getAccountId());

            String code = oAuth2CodeService.createCode(principal.getAccountId());

            String redirectUrl = String.format(
                "%s?code=%s",
                oAuth2RedirectProperties.getRedirectUrl(),
                code
            );

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 실패", e);
            String errorRedirect = String.format(
                "%s?error=%s",
                oAuth2RedirectProperties.getRedirectUrl(),
                URLEncoder.encode("로그인 처리에 실패했습니다.", StandardCharsets.UTF_8)
            );
            response.sendRedirect(errorRedirect);
        }
    }
}
