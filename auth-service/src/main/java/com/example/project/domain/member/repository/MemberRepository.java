package com.example.project.domain.member.repository;

import com.example.project.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.roles WHERE m.username = :username")
    Optional<Member> findByUsernameWithRoles(String username);
}