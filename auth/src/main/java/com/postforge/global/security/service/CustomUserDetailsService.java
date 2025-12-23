package com.postforge.global.security.service;

import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.entity.Role;
import com.postforge.domain.member.repository.MemberRepository;
import java.util.Collection;
import java.util.stream.Collectors;
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

        /** UsernameNotFoundException(401) 이외의 예외는
         * InternalAuthenticationServiceException(500)으로 래핑됨
         * 즉, GlobalExceptionHandler가 예외를 잡지 못함 **/
        Member member = memberRepository.findByUserId(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자 찾을 수 없음"));

        Collection<GrantedAuthority> authorities = member.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .collect(Collectors.toList());

        return new CustomUserDetails(
            member.getId(),
            member.getUserId(),
            member.getUserPw(),
            authorities
        );
    }
}
