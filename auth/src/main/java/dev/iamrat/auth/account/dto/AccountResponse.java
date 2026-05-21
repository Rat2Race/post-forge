package dev.iamrat.auth.account.dto;

import dev.iamrat.auth.account.entity.Account;
import java.util.List;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;

@Builder
public record AccountResponse(
    String username,
    String nickname,
    String provider,
    boolean isOAuthUser,
    List<String> roles
) {
    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
            .username(account.getUsername())
            .nickname(account.getNickname())
            .provider(account.getProvider())
            .isOAuthUser(!"LOCAL".equals(account.getProvider()))
            .roles(account.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();
    }
}
