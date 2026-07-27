package dev.iamrat.auth.security.infrastructure.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final OAuth2RedirectProperties oAuth2RedirectProperties;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        log.error("OAuth2 로그인 실패: {}", exception.getMessage());

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);

        String redirectUrl = String.format(
            "%s/oauth2/callback?error=%s",
            oAuth2RedirectProperties.getRedirectUrl(),
            errorMessage
        );

        response.sendRedirect(redirectUrl);
    }
}
