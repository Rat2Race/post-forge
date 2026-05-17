package dev.iamrat.auth.oauth.service;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginService {
    private final OAuth2CodeService oAuth2CodeService;
    private final AccountService accountService;
    private final JwtProvider jwtProvider;

    public JwtResponse exchange(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        String userId = oAuth2CodeService.exchangeCode(code.trim());
        Account account = accountService.findByUserId(userId);

        log.info("OAuth2 code 교환 성공: userId={}", userId);

        return jwtProvider.createToken(
            account.getUserId(),
            account.getNickname(),
            account.getAuthorities()
        );
    }
}
