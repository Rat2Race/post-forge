package dev.iamrat.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1)
public class ActuatorSecurityConfig {
    
    @Value("${monitoring.username}")
    private String username;
    
    @Value("${monitoring.password}")
    private String password;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .userDetailsService(actuatorUserDetailsService());
        return http.build();
    }
    
    @Bean
    public UserDetailsService actuatorUserDetailsService() {
        UserDetails userDetails = User.withUsername(username)
            .password("{noop}" + password)
            .roles("ACTUATOR")
            .build();
        return new InMemoryUserDetailsManager(userDetails);
    }
}
