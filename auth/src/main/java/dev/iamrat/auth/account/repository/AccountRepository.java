package dev.iamrat.auth.account.repository;

import dev.iamrat.auth.account.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
	@EntityGraph(attributePaths = "roles")
	Optional<Account> findByUserId(String userId);

	boolean existsByUserId(String userId);
	boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    
    Optional<Account> findByProviderAndProviderId(String provider, String providerId);
}
