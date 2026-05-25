package dev.iamrat.auth.token.presentation;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import dev.iamrat.auth.token.presentation.dto.AccessTokenResponse;
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
	private final TokenService tokenService;
	private final CookieProvider cookieProvider;

	@PostMapping("/reissue")
	public ResponseEntity<AccessTokenResponse> reissue(HttpServletRequest request,
			HttpServletResponse response) {
		String refreshToken = cookieProvider.extractRefreshToken(request.getCookies());

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(AuthErrorCode.INVALID_TOKEN);
		}

		TokenIssueResult tokenIssueResult = tokenService.reissueToken(refreshToken);

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
