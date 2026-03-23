package dev.iamrat.post.service;

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountService {
	private static final String COOKIE_PREFIX = "post_";
	private static final String COOKIE_SUFFIX = "_viewed";
	private static final int COOKIE_MAX_AGE = 24 * 60 * 60;

	public boolean shouldIncrementView(Long postId, Cookie[] cookies) {
		if (cookies == null) {
			return true;
		}

		String cookieName = getCookieName(postId);

		return Arrays.stream(cookies)
			.noneMatch(cookie -> cookie.getName().equals(cookieName));
	}

	public Cookie createViewCookie(Long postId) {
		String cookieName = getCookieName(postId);
		Cookie cookie = new Cookie(cookieName, "true");
		cookie.setMaxAge(COOKIE_MAX_AGE);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		return cookie;
	}

	private String getCookieName(Long postId) {
		return COOKIE_PREFIX + postId + COOKIE_SUFFIX;
	}
}
