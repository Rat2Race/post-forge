package com.springweb.study.security.repository;

import com.springweb.study.domain.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);
	Optional<User> findByAccount(String account);
	Optional<User> findById(UUID id);
}
