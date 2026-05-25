package dev.iamrat.auth.account.infrastructure.persistence;

import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountPersistenceAdapterTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountPersistenceAdapter accountPersistenceAdapter;

    @Test
    @DisplayName("saveAndFlush delegates to the Spring Data repository")
    void saveAndFlush_delegatesToRepository() {
        Account account = account(1L);
        given(accountRepository.saveAndFlush(account)).willReturn(account);

        Account result = accountPersistenceAdapter.saveAndFlush(account);

        assertThat(result).isSameAs(account);
    }

    @Test
    @DisplayName("findById delegates to the Spring Data repository")
    void findById_delegatesToRepository() {
        Account account = account(1L);
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));

        Optional<Account> result = accountPersistenceAdapter.findById(1L);

        assertThat(result).containsSame(account);
    }

    @Test
    @DisplayName("findWithRolesById delegates to the Spring Data repository")
    void findWithRolesById_delegatesToRepository() {
        Account account = account(1L);
        given(accountRepository.findWithRolesById(1L)).willReturn(Optional.of(account));

        Optional<Account> result = accountPersistenceAdapter.findWithRolesById(1L);

        assertThat(result).containsSame(account);
    }

    @Test
    @DisplayName("identity lookups delegate to the Spring Data repository")
    void identityLookups_delegateToRepository() {
        Account account = account(1L);
        given(accountRepository.findByUsername("user")).willReturn(Optional.of(account));
        given(accountRepository.findByProviderAndProviderId("GOOGLE", "google-123"))
            .willReturn(Optional.of(account));

        assertThat(accountPersistenceAdapter.findByUsername("user")).containsSame(account);
        assertThat(accountPersistenceAdapter.findByProviderAndProviderId("GOOGLE", "google-123"))
            .containsSame(account);
    }

    @Test
    @DisplayName("existence and flush operations delegate to the Spring Data repository")
    void existenceAndFlush_delegateToRepository() {
        given(accountRepository.existsByUsername("user")).willReturn(true);
        given(accountRepository.existsByNickname("nick")).willReturn(true);
        given(accountRepository.existsByEmail("user@test.com")).willReturn(true);

        assertThat(accountPersistenceAdapter.existsByUsername("user")).isTrue();
        assertThat(accountPersistenceAdapter.existsByNickname("nick")).isTrue();
        assertThat(accountPersistenceAdapter.existsByEmail("user@test.com")).isTrue();

        accountPersistenceAdapter.flush();
        verify(accountRepository).flush();
    }

    private Account account(Long accountId) {
        return Account.builder()
            .id(accountId)
            .username("user")
            .email("user@test.com")
            .nickname("nick")
            .provider("LOCAL")
            .build();
    }
}
