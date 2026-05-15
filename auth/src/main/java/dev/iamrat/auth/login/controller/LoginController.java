package dev.iamrat.auth.login.controller;

import dev.iamrat.core.global.security.UserPrincipal;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.auth.login.service.LoginService;
import dev.iamrat.auth.login.dto.LoginRequest;
import dev.iamrat.auth.token.dto.AccessTokenResponse;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.CookieProvider;
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
			HttpServletResponse response) {
		JwtResponse jwtResponse = loginService.login(request);

		cookieProvider.addRefreshTokenCookie(response, jwtResponse.refreshToken());

		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.PRAGMA, "no-cache")
				.body(AccessTokenResponse.builder()
						.grantType(jwtResponse.grantType())
						.accessToken(jwtResponse.accessToken())
						.build());
	}

	@PostMapping("/logout")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserPrincipal userDetails,
			HttpServletResponse response) {
		loginService.logout(userDetails.getUserId());
		cookieProvider.removeRefreshTokenCookie(response);
		return ResponseEntity.ok(MessageResponse.of("로그아웃되었습니다."));
	}
}
