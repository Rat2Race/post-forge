package dev.iamrat.app.config.security;

import dev.iamrat.app.config.monitoring.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class ActuatorSecurityConfig {

    private final MonitoringProperties monitoringProperties;

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(
        HttpSecurity http,
        PasswordEncoder passwordEncoder
    ) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .userDetailsService(actuatorUserDetailsService(passwordEncoder));
        return http.build();
    }

    public UserDetailsService actuatorUserDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails userDetails = User.withUsername(monitoringProperties.getUsername())
            .password(passwordEncoder.encode(monitoringProperties.getPassword()))
            .roles("ACTUATOR")
            .build();
        return new InMemoryUserDetailsManager(userDetails);
    }
}
