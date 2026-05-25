package dev.iamrat.auth.oauth.infrastructure.external;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class NaverUserInfoTest {

    @Test
    @DisplayName("네이버 OAuth 응답의 response 객체에서 사용자 정보를 추출한다")
    void naverUserInfo_nestedResponse_extractsProfileFields() {
        NaverUserInfo userInfo = new NaverUserInfo(Map.of(
            "resultcode", "00",
            "message", "success",
            "response", Map.of(
                "id", "naver-user-123",
                "name", "홍길동",
                "email", "naver@example.com"
            )
        ));

        assertThat(userInfo.getId()).isEqualTo("naver-user-123");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
        assertThat(userInfo.getEmail()).isEqualTo("naver@example.com");
    }

    @Test
    @DisplayName("네이버 OAuth 응답에 response 객체가 없으면 null 값을 반환한다")
    void naverUserInfo_missingResponse_returnsNullFields() {
        NaverUserInfo userInfo = new NaverUserInfo(Map.of(
            "id", "top-level-id",
            "name", "top-level-name",
            "email", "top-level@example.com"
        ));

        assertThat(userInfo.getId()).isNull();
        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    @DisplayName("네이버 OAuth 응답의 response 값이 객체가 아니면 null 값을 반환한다")
    void naverUserInfo_nonObjectResponse_returnsNullFields() {
        NaverUserInfo userInfo = new NaverUserInfo(Map.of(
            "response", "invalid-response"
        ));

        assertThat(userInfo.getId()).isNull();
        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getEmail()).isNull();
    }

    @Test
    @DisplayName("네이버 OAuth 응답이 null이면 null 값을 반환한다")
    void naverUserInfo_nullAttributes_returnsNullFields() {
        NaverUserInfo userInfo = new NaverUserInfo(null);

        assertThat(userInfo.getId()).isNull();
        assertThat(userInfo.getName()).isNull();
        assertThat(userInfo.getEmail()).isNull();
    }
}
