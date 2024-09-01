package com.springweb.study.repository;


import com.springweb.study.domain.Tokens;
import com.springweb.study.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.token.Token;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepo extends JpaRepository<Tokens, Long> {
	Tokens findByUser(User user);
}
