package com.postforge.api.auth.service;

import com.postforge.api.member.dto.MemberPermission;
import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.entity.Role;
import com.postforge.domain.member.repository.MemberRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationServiceImpl implements com.postforge.api.auth.AuthorizationService {

    private final MemberRepository memberRepository;

    @Override
    public boolean hasPermission(String username, String resource, String action) {
        // TODO: 리소스별 권한 체크 로직 구현
        return false;
    }

    @Override
    public boolean hasRole(String username, String role) {
        Member member = memberRepository.findByUsername(username)
            .orElse(null);
        if (member == null) return false;

        return member.getRoles().stream()
            .anyMatch(r -> r.getValue().equals("ROLE_" + role));
    }

    @Override
    public MemberPermission getPermissions(String username) {
        Member member = memberRepository.findByUsernameWithRoles(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new MemberPermission(
            member.getUsername(),
            member.getRoles().stream()
                .map(Role::getValue)
                .toList()
        );
    }
}
