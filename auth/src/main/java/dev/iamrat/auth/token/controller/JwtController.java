package dev.iamrat.auth.token.controller;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.token.dto.AccessTokenResponse;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.CookieProvider;
import dev.iamrat.auth.token.provider.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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
			throw new CustomException(AuthErrorCode.INVALID_TOKEN);
		}

		JwtResponse jwtResponse = jwtProvider.reissueToken(refreshToken);

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
