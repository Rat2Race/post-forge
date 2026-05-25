package dev.iamrat.board.like.infrastructure.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikeRequestRedisKeysTest {

    @Test
    @DisplayName("cooldown key는 target/action/entity/account 순서로 생성한다")
    void cooldownKey_includesTargetActionEntityAndAccount() {
        assertThat(LikeRequestRedisKeys.cooldownKey("post", 1L, 2L, "like"))
            .isEqualTo("like:cooldown:post:like:1:2");
    }

    @Test
    @DisplayName("rate key는 account 단위로 생성한다")
    void rateKey_includesAccount() {
        assertThat(LikeRequestRedisKeys.rateKey(2L))
            .isEqualTo("like:rate:2");
    }
}
