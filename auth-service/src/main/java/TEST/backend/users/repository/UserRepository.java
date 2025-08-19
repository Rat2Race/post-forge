package TEST.backend.users.repository;

import TEST.backend.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Account, Long> {
	Account findByUsername(String username);
}
