package TEST.backend.users.repository;

import TEST.backend.domain.entity.SocialMember;
import TEST.backend.domain.entity.SocialMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialMemberRepository extends JpaRepository<SocialMember, SocialMemberKey> {
}
