package dev.iamrat.auth.oauth.application;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2CodeService {
    private final OAuth2CodeStore oAuth2CodeStore;

    public String createCode(Long accountId) {
        if (accountId == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        String code = UUID.randomUUID().toString();
        oAuth2CodeStore.save(code, accountId);
        return code;
    }

    public Long exchangeCode(String code) {
        String accountId = oAuth2CodeStore.getAndDelete(code);
        if (accountId == null) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.valueOf(accountId);
        } catch (NumberFormatException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
