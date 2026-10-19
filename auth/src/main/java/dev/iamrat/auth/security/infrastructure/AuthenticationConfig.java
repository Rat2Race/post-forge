package dev.iamrat.auth.security.infrastructure;

import dev.iamrat.auth.security.infrastructure.filter.JwtAuthenticationFilter;
import dev.iamrat.auth.token.application.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 로그인·가입·토큰 검증에 auth가 직접 쓰는 빈. 필터 체인 조립과 URL 규칙은 app의 SecurityConfig가 맡는다.
 */
@Configuration
public class AuthenticationConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService) {
        return new JwtAuthenticationFilter(tokenService);
    }
}
