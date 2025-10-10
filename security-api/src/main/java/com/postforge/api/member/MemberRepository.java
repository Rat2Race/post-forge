package com.postforge.api.member;

import com.postforge.api.member.dto.MemberInfo;
import java.util.Optional;

public interface MemberRepository {
    MemberInfo save(MemberInfo member);
    Optional<MemberInfo> findById(Long id);
    Optional<MemberInfo> findByUsername(String username);
    Optional<MemberInfo> findByUserId(String userId);
    boolean existsByUsername(String username);
    boolean existsByUserId(String userId);
    void deleteById(Long id);
}
