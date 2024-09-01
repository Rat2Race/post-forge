package com.springweb.study.security.jwt;

import com.springweb.study.domain.Tokens;
import com.springweb.study.domain.User;
import com.springweb.study.repository.TokenRepo;
import com.springweb.study.repository.UserRepo;
import com.springweb.study.security.impl.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
	private final JwtUtils jwtUtils;
	private final TokenRepo tokenRepo;
	private final UserRepo userRepo;
	private final UserDetailsServiceImpl userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String accessToken = parseJwt(request);
			User user = userRepo.findByUsername(jwtUtils.getUsername(accessToken))
					.orElseThrow(() -> new UsernameNotFoundException("not found username"));
			String refreshToken = tokenRepo.findByUser(user).getRefreshToken();

			if (jwtUtils.isAccessTokenExpired(accessToken)) {
				setAuthentication(request, accessToken);

			} else {
				accessToken = reissueAccessToken(user, refreshToken);

				if(accessToken == null) {
					throw new IllegalArgumentException("access token is null");
				}

				setAuthentication(request, accessToken);
			}
		} catch (Exception e) {
			request.setAttribute("exception", e);
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			return headerAuth.substring(7);
		}

		return null;
	}

	private String reissueAccessToken(User user, String refreshToken) {
		Tokens tokenRepoByUser = tokenRepo.findByUser(user);

		if(tokenRepoByUser.validateRefreshToken(refreshToken)) {
			String accessToken = jwtUtils.createAccessToken(user.getUsername());
			tokenRepoByUser.updateRefreshToken(jwtUtils.createRefreshToken());
			return accessToken;
		}

		return null;
	}

	private void setAuthentication(HttpServletRequest request, String accessToken) {

		UserDetails userDetails = userDetailsService.loadUserByUsername(jwtUtils.getUsername(accessToken));

		if (userDetails != null) {
			AbstractAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetails, accessToken, userDetails.getAuthorities());

			authentication.setDetails(new WebAuthenticationDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} else {
			throw new IllegalArgumentException("user details is null");
		}
	}

}