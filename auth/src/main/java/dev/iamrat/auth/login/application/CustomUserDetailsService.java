package dev.iamrat.auth.login.application;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.security.principal.AccountAuthorityMapper;
import dev.iamrat.auth.security.principal.CustomUserDetails;
import dev.iamrat.auth.support.error.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AccountQueryService accountQueryService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountQueryService.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(AuthErrorCode.INVALID_CREDENTIALS.getMessage()));

        if (!account.isActive()) {
            throw new DisabledException(AuthErrorCode.ACCOUNT_NOT_ACTIVE.getMessage());
        }

        return new CustomUserDetails(
            account.getId(),
            account.getUsername(),
            account.getPassword(),
            AccountAuthorityMapper.toAuthorities(account)
        );
    }
}
