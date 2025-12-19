package com.postforge.global.config;

import com.postforge.global.security.handler.JwtAccessDeniedHandler;
import com.postforge.global.security.handler.JwtAuthenticationEntryPoint;
import com.postforge.global.security.jwt.JwtAuthenticationFilter;
import com.postforge.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnWebApplication
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
//                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ===== 공개 API =====
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/reissue").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/email/send").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/email/verify").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts/*/comments").permitAll()
                .requestMatchers("/images/**").permitAll()

                .requestMatchers("/auth/security").permitAll()

                // ===== 인증 필요 API =====
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/auth/logout").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/posts").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/posts/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/posts/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/posts/*/like").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/posts/*/comments").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/posts/*/comments/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/posts/*/comments/*").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/posts/*/comments/*/like").hasRole("USER")

                // ===== 관리자 전용 API =====
                .requestMatchers("/admin/**").hasRole("ADMIN")

                .anyRequest().authenticated())
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
