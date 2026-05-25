package dev.iamrat.auth.oauth.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisOAuth2CodeKeysTest {

    @Test
    @DisplayName("OAuth2 교환 코드 key는 code 단위로 생성한다")
    void exchangeCodeKey_includesCode() {
        assertThat(RedisOAuth2CodeKeys.exchangeCodeKey("exchange-code"))
            .isEqualTo("oauth2_code:exchange-code");
    }
}
