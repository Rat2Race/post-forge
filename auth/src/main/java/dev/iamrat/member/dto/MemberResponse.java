package dev.iamrat.member.dto;

import dev.iamrat.member.entity.Member;
import java.util.List;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;

@Builder
public record MemberResponse(
    Long id,
    String userId,
    String nickname,
    String provider,
    boolean isOAuthUser,
    List<String> roles
) {
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
            .id(member.getId())
            .userId(member.getUserId())
            .nickname(member.getNickname())
            .provider(member.getProvider())
            .isOAuthUser(member.getProvider() != null)
            .roles(member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();
    }
}
