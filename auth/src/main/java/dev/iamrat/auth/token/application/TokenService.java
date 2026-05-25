package dev.iamrat.auth.token.application;

import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.security.principal.AccountAuthorityMapper;
import dev.iamrat.auth.security.principal.AuthenticatedAccount;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenStore refreshTokenStore;
    private final AccountQueryService accountQueryService;

    public TokenIssueResult createToken(Long accountId, Collection<? extends GrantedAuthority> authorities) {
        String accessToken = tokenIssuer.generateAccessToken(accountId, toAuthorityNames(authorities));
        String refreshToken = tokenIssuer.generateRefreshToken(accountId);

        refreshTokenStore.save(accountId, refreshToken);

        return TokenIssueResult.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    public TokenIssueResult reissueToken(String refreshToken) {
        Long accountId = parseAccountId(tokenIssuer.parse(refreshToken).subject());

        refreshTokenStore.validate(accountId, refreshToken);

        Account account = accountQueryService.findWithRolesById(accountId);
        if (!account.isActive()) {
            throw new CustomException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        String newAccessToken = tokenIssuer.generateAccessToken(
            accountId,
            AccountAuthorityMapper.toAuthorityNames(account)
        );
        String newRefreshToken = tokenIssuer.generateRefreshToken(accountId);

        refreshTokenStore.save(accountId, newRefreshToken);
        
        return TokenIssueResult.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }
    
    public void deleteToken(Long accountId) {
        refreshTokenStore.delete(accountId);
    }
    
    public Authentication resolveAuthentication(String token) {
        TokenClaims claims = tokenIssuer.parse(token);
        Long accountId = parseAccountId(claims.subject());
        Collection<? extends GrantedAuthority> authorities = parseAuthorities(claims);
 
        log.debug("[JWT] Authentication 생성 성공 - accountId={}, authorities={}", accountId, authorities);
        
        return UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedAccount(accountId), null, authorities
        );
    }
    
    private Collection<? extends GrantedAuthority> parseAuthorities(TokenClaims claims) {
        return claims.roles().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
    }

    private List<String> toAuthorityNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private Long parseAccountId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new CustomException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
