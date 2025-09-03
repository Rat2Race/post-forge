package com.postforge.global.security.service;

import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(member.getRoles()));
        ModelMapper mapper = new ModelMapper();
        AccountDto accountDto = mapper.map(member, AccountDto.class);

        return new AccountContext(accountDto, authorities);
    }
}
