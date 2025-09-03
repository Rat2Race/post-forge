package com.postforge.domain.member.repository;

import com.postforge.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByUsername(String username);
	Optional<Member> findByUserId(String userId);
}
