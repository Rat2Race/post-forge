package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.dto.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
                    provider.toLowerCase() + "_" + providerId,
                    null,
                    userInfo.getEmail(),
                    generateUniqueNickname(),
                    provider,
                    providerId
                )
            );
    }

    private String generateUniqueNickname() {
        for (int i = 0; i < 10; i++) {
            String nickname = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!memberRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
        // 10회 실패 시 타임스탬프 기반으로 유일성 보장
        return "user_" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100);
    }
}
