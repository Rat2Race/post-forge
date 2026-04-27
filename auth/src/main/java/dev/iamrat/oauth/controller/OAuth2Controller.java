package dev.iamrat.oauth.controller;

import dev.iamrat.oauth.service.OAuth2LoginService;
import dev.iamrat.token.dto.AccessTokenResponse;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public ResponseEntity<AccessTokenResponse> exchange(@RequestBody String code,
                                                        HttpServletResponse response) {
        JwtResponse jwtResponse = oAuth2LoginService.exchange(code);
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
