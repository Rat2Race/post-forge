package dev.iamrat.auth.email.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EmailVerificationRequestDecisionTest {

    @Test
    @DisplayName("알 수 없는 decision code는 내부 상태 예외로 감싼다")
    void fromCode_unknownCode_throwsIllegalStateException() {
        assertThatThrownBy(() -> EmailVerificationRequestDecision.fromCode("UNKNOWN"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("알 수 없는 이메일 인증 요청 결정입니다: UNKNOWN")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
