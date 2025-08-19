package com.postforge.auth.users.repository;

import com.postforge.auth.domain.entity.SocialMember;
import com.postforge.auth.domain.entity.SocialMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialMemberRepository extends JpaRepository<SocialMember, SocialMemberKey> {
}
