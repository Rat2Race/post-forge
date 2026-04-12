package dev.iamrat.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorSecurityConfigTest {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    @DisplayName("액추에이터 계정 비밀번호는 인코딩되어 저장된다")
    void actuatorUserDetailsService_encodesPassword() {
        ActuatorSecurityConfig config = new ActuatorSecurityConfig();
        ReflectionTestUtils.setField(config, "username", "monitor");
        ReflectionTestUtils.setField(config, "password", "plain-secret");

        UserDetailsService userDetailsService = config.actuatorUserDetailsService(passwordEncoder);
        UserDetails userDetails = userDetailsService.loadUserByUsername("monitor");

        assertThat(passwordEncoder.matches("plain-secret", userDetails.getPassword())).isTrue();
        assertThat(userDetails.getPassword()).doesNotContain("{noop}");
    }
}
