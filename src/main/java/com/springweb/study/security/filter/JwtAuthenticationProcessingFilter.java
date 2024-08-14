package com.springweb.study.security.filter;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.repository.UserRepo;
import com.springweb.study.security.service.JwtService;
import com.springweb.study.security.service.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepo userRepo;

	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

	private final String NO_CHECK_URL = "/login";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if(request.getRequestURI().equals(NO_CHECK_URL)) {
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
