package com.postforge.auth.security.provider;

import com.postforge.auth.security.details.JwtAuthenticationToken;
import com.postforge.auth.users.service.JWTTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Log4j2
@Component("jwtAuthenticationProvider")
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {
    private final JWTTokenService jwtTokenService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Long userId = jwtTokenService.extractUserId(((JwtAuthenticationToken) authentication).getJsonWebToken());
        return new JwtAuthenticationToken(userId, "", Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
