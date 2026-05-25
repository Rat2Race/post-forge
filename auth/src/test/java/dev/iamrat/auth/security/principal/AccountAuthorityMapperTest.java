package dev.iamrat.auth.security.principal;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AccountAuthorityMapperTest {

    @Test
    @DisplayName("계정 role을 Spring Security 권한으로 변환한다")
    void toAuthorities_mapsAccountRolesToGrantedAuthorities() {
        Account account = Account.builder()
            .username("admin")
            .password("encoded-password")
            .email("admin@example.com")
            .nickname("admin")
            .roles(Set.of(AccountRole.ADMIN, AccountRole.USER))
            .build();

        assertThat(AccountAuthorityMapper.toAuthorities(account))
            .extracting("authority")
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
        assertThat(AccountAuthorityMapper.toAuthorityNames(account))
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }
}
