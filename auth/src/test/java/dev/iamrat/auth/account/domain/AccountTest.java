package dev.iamrat.auth.account.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AccountTest {

    @Test
    @DisplayName("Local 계정 factory는 기본 provider와 회원 role을 설정한다")
    void createLocal_setsDefaultProviderAndUserRole() {
        Account account = Account.createLocal(
            "testuser1",
            "encoded-password",
            "test@example.com",
            "tester"
        );

        assertThat(account.getUsername()).isEqualTo("testuser1");
        assertThat(account.getPassword()).isEqualTo("encoded-password");
        assertThat(account.getEmail()).isEqualTo("test@example.com");
        assertThat(account.getNickname()).isEqualTo("tester");
        assertThat(account.getProvider()).isEqualTo(Account.LOCAL_PROVIDER);
        assertThat(account.getProviderId()).isNull();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getRoles()).containsExactly(AccountRole.USER);
    }

    @Test
    @DisplayName("OAuth 계정 factory는 provider identity와 회원 role을 설정한다")
    void createOAuth_setsProviderIdentityAndUserRole() {
        Account account = Account.createOAuth(
            "GOOGLE",
            "google-user-123",
            "test@gmail.com",
            "oauthUser"
        );

        assertThat(account.getUsername()).isEqualTo("google_google-user-123");
        assertThat(account.getPassword()).isNull();
        assertThat(account.getEmail()).isEqualTo("test@gmail.com");
        assertThat(account.getNickname()).isEqualTo("oauthUser");
        assertThat(account.getProvider()).isEqualTo("GOOGLE");
        assertThat(account.getProviderId()).isEqualTo("google-user-123");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getRoles()).containsExactly(AccountRole.USER);
    }

    @Test
    @DisplayName("비밀번호 변경은 이미 인코딩된 비밀번호만 반영한다")
    void updatePassword_replacesEncodedPassword() {
        Account account = Account.createLocal(
            "testuser1",
            "old-encoded-password",
            "test@example.com",
            "tester"
        );

        account.updatePassword("new-encoded-password");

        assertThat(account.getPassword()).isEqualTo("new-encoded-password");
    }
}
