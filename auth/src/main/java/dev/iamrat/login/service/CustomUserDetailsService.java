package dev.iamrat.login.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import java.util.Collection;
import java.util.stream.Collectors;

import dev.iamrat.login.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUserId(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자 찾을 수 없음"));

        return new CustomUserDetails(
            member.getUserId(),
            member.getUserPw(),
            member.getNickname(),
            member.getAuthorities()
        );
    }
}
