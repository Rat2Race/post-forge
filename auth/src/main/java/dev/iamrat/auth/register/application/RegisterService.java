package dev.iamrat.auth.register.application;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.support.normalizer.EmailNormalizer;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final AccountCommandService accountCommandService;
    private final AccountQueryService accountQueryService;

    @Transactional
    public Long register(RegisterCommand command) {
        String normalizedEmail = EmailNormalizer.normalize(command.email());

        if (accountQueryService.existsByUsername(command.username())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_USERNAME);
        }

        if (accountQueryService.existsByEmail(normalizedEmail)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        if (accountQueryService.existsByNickname(command.nickname())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        Account account = accountCommandService.createGeneralAccount(
            command.username(),
            command.password(),
            normalizedEmail,
            command.nickname()
        );

        return account.getId();
    }
}
