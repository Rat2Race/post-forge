package dev.iamrat.auth.account.infrastructure.persistence;

import dev.iamrat.auth.account.domain.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	@EntityGraph(attributePaths = "roles")
	Optional<Account> findByUsername(String username);

	@EntityGraph(attributePaths = "roles")
	Optional<Account> findWithRolesById(Long id);

	boolean existsByUsername(String username);
	boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    
    Optional<Account> findByProviderAndProviderId(String provider, String providerId);
}
