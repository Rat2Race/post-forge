package dev.iamrat.auth.login.presentation;

import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.auth.login.application.LoginService;
import dev.iamrat.auth.login.presentation.dto.LoginRequest;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.presentation.CookieProvider;
import dev.iamrat.auth.token.presentation.dto.AccessTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class LoginController {
	private final LoginService loginService;
	private final CookieProvider cookieProvider;

	@PostMapping("/login")
	public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest,
			HttpServletResponse response) {
		TokenIssueResult tokenIssueResult = loginService.login(
				request.username(),
				request.password(),
				servletRequest.getRemoteAddr());

		cookieProvider.addRefreshTokenCookie(response, tokenIssueResult.refreshToken());

		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.PRAGMA, "no-cache")
				.body(AccessTokenResponse.builder()
						.grantType(tokenIssueResult.grantType())
						.accessToken(tokenIssueResult.accessToken())
						.build());
	}

	@PostMapping("/logout")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserPrincipal userDetails,
			HttpServletResponse response) {
		loginService.logout(userDetails.getAccountId());
		cookieProvider.removeRefreshTokenCookie(response);
		return ResponseEntity.ok(MessageResponse.of("로그아웃되었습니다."));
	}
}
