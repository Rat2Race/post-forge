package dev.iamrat.app.config.security;

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
            .requestMatchers(PostForgeSecurityRoutes.PUBLIC).permitAll()
            .requestMatchers(HttpMethod.POST, PostForgeSecurityRoutes.PUBLIC_POST).permitAll()
            .requestMatchers(HttpMethod.GET, PostForgeSecurityRoutes.PUBLIC_GET).permitAll()
            .requestMatchers(PostForgeSecurityRoutes.USER_OR_ADMIN).hasAnyRole("USER", "ADMIN")
            .requestMatchers(PostForgeSecurityRoutes.ADMIN).hasRole("ADMIN")
            .anyRequest().denyAll();
    }
}
