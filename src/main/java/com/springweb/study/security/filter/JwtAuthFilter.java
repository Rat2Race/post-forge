package com.springweb.study.security.filter;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.service.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		List<String> list = Arrays.asList(
				"/user/login",
				"/login",
				"/css/**",
				"/js/**",
				"/images/**"
		);

		if (list.contains(request.getRequestURI())) {
			filterChain.doFilter(request, response);
			return;
		}

		String refreshToken = jwtService
				.extractRefreshToken(request)
				.filter(jwtService::isTokenValid)
				.orElse(null);

		if(refreshToken != null) {
			checkRefreshTokenAndReIssueAccessToken(response, refreshToken);
			return;
		}

		checkAccessTokenAndAuthentication(request, response, filterChain);
	}

	private void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		jwtService.extractAccessToken(request).filter(jwtService::isTokenValid).ifPresent(
				accessToken -> jwtService.extractEmail(accessToken).ifPresent(
						email -> userRepo.findByEmail(email).ifPresent(
								user -> saveAuthentication(user)
						)
				)
		);

		filterChain.doFilter(request,response);
	}

	private void saveAuthentication(User user) {
		UserDetailsImpl userDetails = new UserDetailsImpl(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,authoritiesMapper.mapAuthorities(userDetails.getAuthorities()));

		SecurityContext context = SecurityContextHolder.createEmptyContext();//5
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	private void checkRefreshTokenAndReIssueAccessToken(HttpServletResponse response, String refreshToken) {
		userRepo.findByRefreshToken(refreshToken).ifPresent(
				users -> jwtService.sendAccessToken(response, jwtService.createAccessToken(users.getEmail()))
		);
	}
}
