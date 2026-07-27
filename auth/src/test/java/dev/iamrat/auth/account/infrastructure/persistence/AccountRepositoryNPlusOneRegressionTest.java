package dev.iamrat.auth.account.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(AccountRepositoryNPlusOneRegressionTest.JpaAuditingTestConfig.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AccountRepositoryNPlusOneRegressionTest {

    private final AccountRepository accountRepository;
    private final EntityManager entityManager;
    private final Statistics statistics;

    AccountRepositoryNPlusOneRegressionTest(
        AccountRepository accountRepository,
        EntityManager entityManager,
        EntityManagerFactory entityManagerFactory
    ) {
        this.accountRepository = accountRepository;
        this.entityManager = entityManager;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("username 조회는 roles 권한 매핑까지 단일 쿼리로 로드한다")
    void findByUsername_fetchesRolesWithAccount() {
        Account account = account("user01", "user01@test.com", "nick01");
        account.addRole(AccountRole.ADMIN);
        accountRepository.saveAndFlush(account);

        long queryCount = countQueries(() -> {
            Account found = accountRepository.findByUsername("user01").orElseThrow();
            assertThat(found.getRoles()).containsExactlyInAnyOrder(AccountRole.USER, AccountRole.ADMIN);
        });

        assertThat(queryCount).isEqualTo(1);
    }

    @Test
    @DisplayName("id 조회는 roles 권한 매핑까지 단일 쿼리로 로드한다")
    void findWithRolesById_fetchesRolesWithAccount() {
        Account account = accountRepository.saveAndFlush(account("user02", "user02@test.com", "nick02"));

        long queryCount = countQueries(() -> {
            Account found = accountRepository.findWithRolesById(account.getId()).orElseThrow();
            assertThat(found.getRoles()).containsExactly(AccountRole.USER);
        });

        assertThat(queryCount).isEqualTo(1);
    }

    @Test
    @DisplayName("OAuth provider 조회는 roles 권한 매핑까지 단일 쿼리로 로드한다")
    void findByProviderAndProviderId_fetchesRolesWithAccount() {
        Account account = Account.createOAuth("GOOGLE", "google-123", "oauth@test.com", "oauthUser");
        accountRepository.saveAndFlush(account);

        long queryCount = countQueries(() -> {
            Account found = accountRepository.findByProviderAndProviderId("GOOGLE", "google-123").orElseThrow();
            assertThat(found.getRoles()).containsExactly(AccountRole.USER);
        });

        assertThat(queryCount).isEqualTo(1);
    }

    private long countQueries(Runnable action) {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        action.run();

        return statistics.getPrepareStatementCount();
    }

    private static Account account(String username, String email, String nickname) {
        return Account.createLocal(username, "{noop}Password123!", email, nickname);
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("n-plus-one-test");
        }
    }
}
