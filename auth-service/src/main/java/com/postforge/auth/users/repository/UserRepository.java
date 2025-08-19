package com.postforge.auth.users.repository;

import com.postforge.auth.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Account, Long> {
	Account findByUsername(String username);
}
