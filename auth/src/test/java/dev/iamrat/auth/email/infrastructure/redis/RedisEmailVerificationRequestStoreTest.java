package dev.iamrat.auth.email.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import dev.iamrat.auth.email.application.EmailVerificationRequestDecision;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RedisEmailVerificationRequestStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private RedisEmailVerificationRequestStore store;

    @Test
    @DisplayName("이메일 인증 발송 가드를 Lua script 1회 실행으로 평가한다")
    void evaluateEmailRequest_executesLuaGuardScript() {
        List<String> keys = List.of(
            "email_verify_send:lock:email:tester@test.com",
            "email_verify_send:rate:email:tester@test.com",
            "email_verify_send:cooldown:email:tester@test.com"
        );
        given(redisTemplate.execute(
            ArgumentMatchers.<RedisScript<String>>any(),
            eq(keys),
            eq("180"),
            eq("5"),
            eq("180"),
            eq("10")
        ))
            .willReturn("ALLOWED");

        EmailVerificationRequestDecision decision = store.evaluateEmailRequest(
            "tester@test.com",
            10L,
            180L,
            5L,
            180L
        );

        assertThat(decision).isEqualTo(EmailVerificationRequestDecision.ALLOWED);
        verify(redisTemplate).execute(
            ArgumentMatchers.<RedisScript<String>>any(),
            eq(keys),
            eq("180"),
            eq("5"),
            eq("180"),
            eq("10")
        );
    }

    @Test
    @DisplayName("이메일 인증 발송 가드 Lua script는 classpath resource로 관리한다")
    void luaGuardScriptResourceExists() throws IOException {
        ClassPathResource resource = new ClassPathResource("redis/email-verification-request-guard.lua");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getContentAsString(StandardCharsets.UTF_8))
            .contains("return \"ALLOWED\"")
            .contains("return \"COOLDOWN\"")
            .contains("return \"LOCKED\"")
            .contains("return \"RATE_LIMITED\"");
    }
}
