package dev.iamrat.auth.login.service;

import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.repository.AccountRepository;

import dev.iamrat.auth.login.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUserId(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자 찾을 수 없음"));

        return new CustomUserDetails(
            account.getUserId(),
            account.getUserPw(),
            account.getNickname(),
            account.getAuthorities()
        );
    }
}
