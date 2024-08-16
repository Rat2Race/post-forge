package com.springweb.study.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springweb.study.security.filter.JwtAuthenticationProcessingFilter;
import com.springweb.study.security.handler.LoginFailureHandler;
import com.springweb.study.security.handler.LoginSuccessJWTProvideHandler;
import com.springweb.study.security.repository.UserRepo;
import com.springweb.study.security.service.JwtService;
import com.springweb.study.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final UserDetailsServiceImpl userDetailsService;
	private final ObjectMapper objectMapper;
	private final UserRepo userRepo;
	private final JwtService jwtService;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/login", "/resources/**", "/css/**", "/js/**",
								"**/favicon.ico", "/error", "/signup", "/swagger-ui/**").permitAll()
						.anyRequest().authenticated())
				.logout(logout -> logout
						.logoutSuccessUrl("/login")
						.invalidateHttpSession(true))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				);

		http
				.addFilterAfter(jsonUsernamePasswordAuthenticationFilter(), LogoutFilter.class)
				.addFilterBefore(jwtAuthenticationProcessingFilter(), JsonUsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider() throws Exception {
		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();

		daoAuthenticationProvider.setUserDetailsService(userDetailsService);
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

		return daoAuthenticationProvider;
	}

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager() throws Exception {
		DaoAuthenticationProvider provider = daoAuthenticationProvider();
		return new ProviderManager(provider);
	}

	@Bean
	public LoginSuccessJWTProvideHandler loginSuccessJWTProvideHandler() {
		return new LoginSuccessJWTProvideHandler(jwtService, userRepo);
	}

	@Bean
	public LoginFailureHandler loginFailureHandler() {
		return new LoginFailureHandler();
	}

	@Bean
	public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter() throws Exception {
		JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter = new JsonUsernamePasswordAuthenticationFilter(objectMapper);
		jsonUsernamePasswordAuthenticationFilter.setAuthenticationManager(authenticationManager());
		jsonUsernamePasswordAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessJWTProvideHandler());
		jsonUsernamePasswordAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler());
		return jsonUsernamePasswordAuthenticationFilter;
	}

	@Bean
	public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter() {
		JwtAuthenticationProcessingFilter jsonUsernamePasswordLoginFilter = new JwtAuthenticationProcessingFilter(jwtService, userRepo);

		return jsonUsernamePasswordLoginFilter;
	}
}
