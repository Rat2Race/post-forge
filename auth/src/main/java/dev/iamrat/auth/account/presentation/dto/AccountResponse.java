package dev.iamrat.auth.account.presentation.dto;

import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.domain.AccountRole;
import java.util.List;
import lombok.Builder;

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
            .isOAuthUser(!Account.LOCAL_PROVIDER.equals(account.getProvider()))
            .roles(account.getRoles().stream()
                .map(AccountRole::getValue)
                .sorted()
                .toList())
            .build();
    }
}
