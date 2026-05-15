package dev.iamrat.auth.oauth.controller;

import dev.iamrat.auth.oauth.dto.OAuth2ExchangeRequest;
import dev.iamrat.auth.oauth.service.OAuth2LoginService;
import dev.iamrat.auth.token.dto.AccessTokenResponse;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.CookieProvider;
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
        JwtResponse jwtResponse = oAuth2LoginService.exchange(request.code());
        cookieProvider.addRefreshTokenCookie(response, jwtResponse.refreshToken());

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.PRAGMA, "no-cache")
            .body(AccessTokenResponse.builder()
                .grantType(jwtResponse.grantType())
                .accessToken(jwtResponse.accessToken())
                .build());
    }
}
