package TEST.backend.security.config;

import TEST.backend.security.handler.FormAuthenticationFailureHandler;
import TEST.backend.security.handler.FormAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.WebAuthenticationDetails;


@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
	private final AuthenticationProvider authenticationProvider;
	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	public SecurityConfig(@Qualifier("jwtAuthenticationProvider") AuthenticationProvider authenticationProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
		this.authenticationProvider = authenticationProvider;
		this.authenticationManagerBuilder = authenticationManagerBuilder;
		this.authenticationManagerBuilder.authenticationProvider(authenticationProvider);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
						.authorizeHttpRequests(auth -> auth
										.requestMatchers("/css/**", "images/**", "/js/**", "/favicon.*", "/*/icon-*").permitAll()
										.requestMatchers("/", "/signup", "/login*").permitAll()
										.requestMatchers("/user").hasAuthority("ROLE_USER")
										.requestMatchers("/manager").hasAuthority("ROLE_MANAGER")
										.requestMatchers("/v1/oauth/**").permitAll()
								.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
										.anyRequest().authenticated())
						.formLogin(AbstractHttpConfigurer::disable)
						.authenticationProvider(authenticationProvider)
						.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
						.csrf(AbstractHttpConfigurer::disable)
						.exceptionHandling(exception -> exception
										.accessDeniedHandler(null))
						.with(new JwtSecurityConfig(authenticationManagerBuilder.getOrBuild()), customizer -> {});

		return http.build();

	}
}
