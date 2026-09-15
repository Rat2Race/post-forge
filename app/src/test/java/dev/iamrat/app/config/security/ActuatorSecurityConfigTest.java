package dev.iamrat.app.config.security;

import dev.iamrat.app.config.monitoring.MonitoringProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorSecurityConfigTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    @DisplayName("액추에이터 계정 비밀번호는 인코딩되어 저장된다")
    void actuatorUserDetailsService_encodesPassword() {
        MonitoringProperties monitoringProperties = new MonitoringProperties();
        monitoringProperties.setUsername("monitor");
        monitoringProperties.setPassword("plain-secret");
        ActuatorSecurityConfig config = new ActuatorSecurityConfig(monitoringProperties);

        UserDetailsService userDetailsService = config.actuatorUserDetailsService(passwordEncoder);
        UserDetails userDetails = userDetailsService.loadUserByUsername("monitor");

        assertThat(passwordEncoder.matches("plain-secret", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getPassword()).doesNotContain("{noop}");
    }
}
