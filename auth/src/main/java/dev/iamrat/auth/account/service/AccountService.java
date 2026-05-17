package dev.iamrat.auth.account.service;

import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.entity.Role;
import dev.iamrat.auth.account.repository.AccountRepository;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public Account createAccount(String userId, String rawPassword,
                                 String email, String nickname, String provider, String providerId) {
        Account account = Account.builder()
            .userId(userId)
            .userPw(rawPassword != null ? passwordEncoder.encode(rawPassword) : null)
            .email(email)
            .nickname(nickname)
            .provider(provider)
            .providerId(providerId)
            .build();

        account.addRole(Role.USER);

        return accountRepository.save(account);
    }
    
    public Account findByUserId(String userId) {
        return accountRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
    
    public boolean existsByUserId(String userId) {
        return accountRepository.existsByUserId(userId);
    }

    @Transactional
    public void updateNickname(String userId, String nickname) {
        if (accountRepository.existsByNickname(nickname)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        Account account = findByUserId(userId);
        account.updateNickname(nickname);
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        Account account = findByUserId(userId);

        if (account.getUserPw() == null) {
            throw new CustomException(AuthErrorCode.OAUTH_PASSWORD_CHANGE_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(currentPassword, account.getUserPw())) {
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }

        account.changePassword(newPassword, passwordEncoder);
    }
}
