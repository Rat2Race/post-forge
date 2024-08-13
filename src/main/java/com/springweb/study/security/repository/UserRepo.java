package com.springweb.study.security.repository;

import com.springweb.study.security.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<Users, Long> {
	Users findByEmail(String email);
}
