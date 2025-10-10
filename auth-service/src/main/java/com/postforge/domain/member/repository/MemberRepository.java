package com.postforge.domain.member.repository;

import com.postforge.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByUsername(String username);
	Optional<Member> findByUserId(String userId);
	boolean existsByUsername(String username);
	boolean existsByUserId(String userId);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.roles WHERE m.username = :username")
    Optional<Member> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.roles WHERE m.userId = :userId")
    Optional<Member> findByUserIdWithRoles(@Param("userId") String userId);
}
