package com.springweb.study.security.repository;

import com.springweb.study.common.RoleType;
import com.springweb.study.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
	Optional<User> findByAccount(String account);
	Optional<User> findById(UUID id);
	boolean existsById(UUID id);
	void deleteById(UUID id);
	List<User> findAllByType(RoleType type);
}
