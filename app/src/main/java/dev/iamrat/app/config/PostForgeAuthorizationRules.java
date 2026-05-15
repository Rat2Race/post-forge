package dev.iamrat.app.config;

import dev.iamrat.auth.security.config.HttpAuthorizationRules;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class PostForgeAuthorizationRules implements HttpAuthorizationRules {

    @Override
    public void customize(
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry requests
    ) {
        requests
            .requestMatchers(
                "/",
                "/index.html",
                "/favicon.ico",
                "/images/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/webjars/**",
                "/oauth2/**",
                "/login/oauth2/**"
            ).permitAll()
            .requestMatchers(HttpMethod.POST,
                "/auth/register",
                "/auth/login",
                "/auth/token/reissue",
                "/auth/oauth2/exchange",
                "/auth/token/exchange",
                "/auth/email/send"
            ).permitAll()
            .requestMatchers(HttpMethod.GET,
                "/auth/email/verify",
                "/posts",
                "/posts/*",
                "/posts/*/comments"
            ).permitAll()
            .requestMatchers("/ai/**", "/ingest/**").hasAnyRole("USER", "ADMIN")
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated();
    }
}
