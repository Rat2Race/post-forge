package dev.iamrat.token.controller;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.token.dto.AccessTokenResponse;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import dev.iamrat.token.provider.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/token")
@RequiredArgsConstructor
@Slf4j
public class JwtController {
    private final JwtProvider jwtProvider;
    private final CookieProvider cookieProvider;

    @PostMapping("/reissue")
    public ResponseEntity<AccessTokenResponse> reissue(HttpServletRequest request,
                                                       HttpServletResponse response) {
        String refreshToken = cookieProvider.extractRefreshToken(request.getCookies());

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        JwtResponse jwtResponse = jwtProvider.reissueToken(refreshToken);

        cookieProvider.addRefreshTokenCookie(response, jwtResponse.refreshToken());

        return ResponseEntity.ok(AccessTokenResponse.builder()
            .grantType(jwtResponse.grantType())
            .accessToken(jwtResponse.accessToken())
            .build());
    }
}
