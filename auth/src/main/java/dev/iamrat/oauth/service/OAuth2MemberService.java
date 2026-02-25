package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.dto.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OAuth2MemberService {
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    
    public Member getOrCreateMember(String provider, OAuth2UserInfo userInfo) {
        String providerId = userInfo.getId();
        
        return memberRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() ->
                memberService.createMember(
                    userInfo.getName(),
                    providerId,
                    null,
                    userInfo.getEmail(),
                    generateUniqueNickname(),
                    provider,
                    providerId
                )
            );
    }
    
    private String generateUniqueNickname() {
        int maxRetries = 100;
        
        for (int i = 0; i < maxRetries; i++) {
            String nickname = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!memberRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    }
}
