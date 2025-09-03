//package com.postforge.auth.security.config;
//
//import com.postforge.auth.security.handler.FormAuthenticationFailureHandler;
//import com.postforge.auth.security.handler.FormAuthenticationSuccessHandler;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationDetailsSource;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.WebAuthenticationDetails;
//
//
//@Slf4j
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
////    private final AuthenticationProvider authenticationProvider;
////    private final AuthenticationManagerBuilder authenticationManagerBuilder;
//
////    public SecurityConfig(
////        @Qualifier("jwtAuthenticationProvider") AuthenticationProvider authenticationProvider,
////        AuthenticationManagerBuilder authenticationManagerBuilder) {
////        this.authenticationProvider = authenticationProvider;
////        this.authenticationManagerBuilder = authenticationManagerBuilder;
////        this.authenticationManagerBuilder.authenticationProvider(authenticationProvider);
////    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//            .httpBasic(AbstractHttpConfigurer::disable)
//            .cors(Customizer.withDefaults())
//            .csrf(AbstractHttpConfigurer::disable)
//            .formLogin(AbstractHttpConfigurer::disable)
//            .authorizeHttpRequests(
//                requests -> requests
//                    .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
//                    .requestMatchers(HttpMethod.GET, "").permitAll()
//                    .anyRequest().authenticated())
//                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                    .exceptionHandling(exception -> exception.accessDeniedHandler(null));
//
////        http
////            .authorizeHttpRequests(auth -> auth
////                .requestMatchers("/css/**", "images/**", "/js/**", "/favicon.*", "/*/icon-*")
////                .permitAll()
////                .requestMatchers("/", "/signup", "/login*").permitAll()
////                .requestMatchers("/user").hasAuthority("ROLE_USER")
////                .requestMatchers("/manager").hasAuthority("ROLE_MANAGER")
////                .requestMatchers("/v1/oauth/**").permitAll()
////                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
////                    "/swagger-resources/**", "/webjars/**").permitAll()
////                .anyRequest().authenticated())
////            .authenticationProvider(authenticationProvider)
////            .sessionManagement(
////                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
////            .exceptionHandling(exception -> exception
////                .accessDeniedHandler(null))
////            .with(new JwtSecurityConfig(authenticationManagerBuilder.getOrBuild()), customizer -> {
////            });
//
//        return http.build();
//
//    }
//}
