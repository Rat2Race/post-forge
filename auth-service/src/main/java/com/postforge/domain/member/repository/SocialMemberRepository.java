package com.postforge.domain.member.repository;

import com.postforge.domainOld.entity.SocialMember;
import com.postforge.domainOld.entity.SocialMemberKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialMemberRepository extends JpaRepository<SocialMember, SocialMemberKey> {
}
