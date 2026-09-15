package dev.iamrat.auth.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iamrat.auth.account.domain.Account;
import jakarta.persistence.OptimisticLockException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest
@Import(AccountOptimisticLockTest.JpaAuditingTestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AccountOptimisticLockTest {

    private final AccountRepository accountRepository;
    private final TestEntityManager entityManager;

    AccountOptimisticLockTest(AccountRepository accountRepository, TestEntityManager entityManager) {
        this.accountRepository = accountRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("계정 row는 version으로 낙관적 락 충돌을 감지한다")
    void accountRowDetectsOptimisticLockConflictByVersion() {
        Account saved = accountRepository.saveAndFlush(account("user01", "user01@test.com", "nick01"));
        assertThat(saved.getVersion()).isZero();

        Long accountId = saved.getId();
        entityManager.clear();

        Account firstCopy = accountRepository.findById(accountId).orElseThrow();
        entityManager.detach(firstCopy);

        Account staleCopy = accountRepository.findById(accountId).orElseThrow();
        entityManager.detach(staleCopy);

        firstCopy.updateNickname("nick02");
        Account updated = accountRepository.saveAndFlush(firstCopy);
        assertThat(updated.getVersion()).isEqualTo(1L);

        entityManager.clear();

        staleCopy.updateNickname("nick03");
        assertThatThrownBy(() -> accountRepository.saveAndFlush(staleCopy))
            .isInstanceOfAny(OptimisticLockingFailureException.class, OptimisticLockException.class);
    }

    private static Account account(String username, String email, String nickname) {
        return Account.builder()
            .username(username)
            .password("{noop}Password123!")
            .email(email)
            .nickname(nickname)
            .provider("LOCAL")
            .build();
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("optimistic-lock-test");
        }
    }
}
