package dev.iamrat.member.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    
    public Member createMember(String name, String id, String pw,
                               String email, String nickname, String provider, String providerId) {
        Member member = Member.builder()
            .userName(name)
            .userId(id)
            .userPw(pw != null ? passwordEncoder.encode(pw) : null)
            .email(email)
            .nickname(nickname)
            .provider(provider)
            .providerId(providerId)
            .build();
        
        member.addRole(Role.USER);
        
        return memberRepository.save(member);
    }
    
    public Member findByUserId(String userId) {
        return memberRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
    
    public boolean existsByUserId(String userId) {
        return memberRepository.existsByUserId(userId);
    }
    
    public boolean existsByUserName(String userName) {
        return memberRepository.existsByUserName(userName);
    }
}