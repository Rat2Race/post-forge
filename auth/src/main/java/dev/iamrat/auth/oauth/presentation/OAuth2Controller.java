package dev.iamrat.auth.oauth.presentation;

import dev.iamrat.auth.oauth.application.OAuth2LoginService;
import dev.iamrat.auth.oauth.presentation.dto.OAuth2ExchangeRequest;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.presentation.CookieProvider;
import dev.iamrat.auth.token.presentation.dto.AccessTokenResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OAuth2Controller {
    private final OAuth2LoginService oAuth2LoginService;
    private final CookieProvider cookieProvider;

    @PostMapping("/auth/oauth2/exchange")
    public ResponseEntity<AccessTokenResponse> exchange(@RequestBody @Valid OAuth2ExchangeRequest request,
                                                        HttpServletResponse response) {
        TokenIssueResult tokenIssueResult = oAuth2LoginService.exchange(request.code());
        cookieProvider.addRefreshTokenCookie(response, tokenIssueResult.refreshToken());

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.PRAGMA, "no-cache")
            .body(AccessTokenResponse.builder()
                .grantType(tokenIssueResult.grantType())
                .accessToken(tokenIssueResult.accessToken())
                .build());
    }
}
