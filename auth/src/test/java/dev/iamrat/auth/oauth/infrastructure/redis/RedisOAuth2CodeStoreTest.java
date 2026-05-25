package dev.iamrat.auth.oauth.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RedisOAuth2CodeStoreTest {

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    RedisOAuth2CodeStore redisOAuth2CodeStore;

    @Test
    @DisplayName("OAuth2 교환 코드는 oauth2_code prefix와 60초 TTL로 저장한다")
    void save_storesExchangeCodeWithTtl() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        redisOAuth2CodeStore.save("exchange-code", 1L);

        verify(valueOperations).set(
            "oauth2_code:exchange-code",
            "1",
            60L,
            TimeUnit.SECONDS
        );
    }

    @Test
    @DisplayName("교환 코드는 조회와 동시에 삭제한다")
    void getAndDelete_returnsStoredAccountId() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete("oauth2_code:exchange-code")).willReturn("1");

        assertThat(redisOAuth2CodeStore.getAndDelete("exchange-code")).isEqualTo("1");
    }
}
