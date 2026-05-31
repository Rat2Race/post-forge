package dev.iamrat.auth.oauth.application;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.security.infrastructure.principal.AccountAuthorityMapper;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.auth.token.application.TokenIssueResult;
import dev.iamrat.auth.token.application.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginService {
    private final OAuth2CodeService oAuth2CodeService;
    private final AccountQueryService accountQueryService;
    private final TokenService tokenService;

    public TokenIssueResult exchange(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        Long accountId = oAuth2CodeService.exchangeCode(code.trim());
        Account account = accountQueryService.findWithRolesById(accountId);
        if (!account.isActive()) {
            throw new CustomException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        log.info("OAuth2 code 교환 성공: accountId={}", accountId);

        return tokenService.createToken(
            account.getId(),
            AccountAuthorityMapper.toAuthorities(account)
        );
    }
}
