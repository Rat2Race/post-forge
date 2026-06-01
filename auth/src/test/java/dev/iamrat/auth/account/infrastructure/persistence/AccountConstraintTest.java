package dev.iamrat.auth.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestConstructor;
import jakarta.persistence.PersistenceException;

@Tag("persistence")
@DataJpaTest
@Import(AccountConstraintTest.JpaAuditingTestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AccountConstraintTest {

    private final AccountRepository accountRepository;

    AccountConstraintTest(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Test
    @DisplayName("username은 중복 저장할 수 없다")
    void usernameMustBeUnique() {
        accountRepository.saveAndFlush(account("user01", "user01@test.com", "nick01"));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user01", "user02@test.com", "nick02")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("email은 중복 저장할 수 없다")
    void emailMustBeUnique() {
        accountRepository.saveAndFlush(account("user01", "same@test.com", "nick01"));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user02", "same@test.com", "nick02")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("nickname은 중복 저장할 수 없다")
    void nicknameMustBeUnique() {
        accountRepository.saveAndFlush(account("user01", "user01@test.com", "sameNick"));

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user02", "user02@test.com", "sameNick")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("username은 필수이다")
    void usernameMustNotBeNull() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account(null, "user01@test.com", "nick01")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("email은 필수이다")
    void emailMustNotBeNull() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user01", null, "nick01")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("nickname은 필수이다")
    void nicknameMustNotBeNull() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user01", "user01@test.com", null)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("status는 필수이다")
    void statusMustNotBeNull() {
        Account account = Account.builder()
            .username("user01")
            .password("{noop}Password123!")
            .email("user01@test.com")
            .nickname("nick01")
            .status(null)
            .build();

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("username은 100자를 초과할 수 없다")
    void usernameMustNotExceed100Characters() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account(repeat("u", 101), "user01@test.com", "nick01")))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("password는 255자를 초과할 수 없다")
    void passwordMustNotExceed255Characters() {
        Account account = Account.builder()
            .username("user01")
            .password(repeat("p", 256))
            .email("user01@test.com")
            .nickname("nick01")
            .build();

        assertThatThrownBy(() -> accountRepository.saveAndFlush(account))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("email은 255자를 초과할 수 없다")
    void emailMustNotExceed255Characters() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user01", repeat("e", 256), "nick01")))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @DisplayName("nickname은 50자를 초과할 수 없다")
    void nicknameMustNotExceed50Characters() {
        assertThatThrownBy(() -> accountRepository.saveAndFlush(account("user01", "user01@test.com", repeat("n", 51))))
            .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    private static Account account(String username, String email, String nickname) {
        return Account.builder()
            .username(username)
            .password("{noop}Password123!")
            .email(email)
            .nickname(nickname)
            .build();
    }

    private static String repeat(String value, int count) {
        return value.repeat(count);
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("constraint-test");
        }
    }
}
