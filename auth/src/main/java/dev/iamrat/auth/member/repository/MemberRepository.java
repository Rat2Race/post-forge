package dev.iamrat.auth.member.repository;

import dev.iamrat.auth.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
	@EntityGraph(attributePaths = "roles")
	Optional<Member> findByUserId(String userId);

	boolean existsByUserId(String userId);
	boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);
}
