package com.springweb.study.security.repository;

import com.springweb.study.security.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
	User findByEmail(String email);
	boolean existsByEmail(String email);
	User findByNumber(String number);
	boolean existsByNumber(String number);
	User findByRefreshToken(String refreshToken);
}
