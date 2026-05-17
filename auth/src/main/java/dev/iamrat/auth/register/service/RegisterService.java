package dev.iamrat.auth.register.service;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.email.service.EmailVerificationService;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.register.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final AccountService accountService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public Long register(RegisterRequest request) {

        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (accountService.existsByUserId(request.userId())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_ID);
        }

        Account account = accountService.createAccount(
            request.userId(),
            request.password(),
            request.email(),
            request.nickname(),
            "LOCAL",
            null
        );

        emailVerificationService.removeVerifiedEmail(request.email());

        return account.getId();
    }
}
