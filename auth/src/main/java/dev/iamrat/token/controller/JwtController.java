package dev.iamrat.token.controller;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.login.dto.CustomUserDetails;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.service.OAuth2CodeService;
import dev.iamrat.token.dto.AccessTokenResponse;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.CookieProvider;
import dev.iamrat.token.provider.JwtProvider;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
	private final OAuth2CodeService oAuth2CodeService;
	private final MemberService memberService;

	@PostMapping("/exchange")
	public ResponseEntity<AccessTokenResponse> exchange(@RequestBody String code,
			HttpServletResponse response) {
		String userId = oAuth2CodeService.exchangeCode(code);

		Member member = memberService.findByUserId(userId);

		CustomUserDetails principal = new CustomUserDetails(
				member.getUserId(), null, member.getNickname(), member.getAuthorities());

		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal, null, member.getAuthorities());

		JwtResponse jwtResponse = jwtProvider.createToken(authentication);
		cookieProvider.addRefreshTokenCookie(response, jwtResponse.refreshToken());

		log.info("OAuth2 code 교환 성공: userId={}", userId);

		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.header(HttpHeaders.PRAGMA, "no-cache")
				.body(AccessTokenResponse.builder()
						.grantType(jwtResponse.grantType())
						.accessToken(jwtResponse.accessToken())
						.build());
	}

	@PostMapping("/reissue")
	public ResponseEntity<AccessTokenResponse> reissue(HttpServletRequest request,
			HttpServletResponse response) {
		String refreshToken = cookieProvider.extractRefreshToken(request.getCookies());

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
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
