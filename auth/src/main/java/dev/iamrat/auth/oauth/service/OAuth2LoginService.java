package dev.iamrat.auth.oauth.service;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.member.entity.Member;
import dev.iamrat.auth.member.service.MemberService;
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
    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    public JwtResponse exchange(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        String userId = oAuth2CodeService.exchangeCode(code.trim());
        Member member = memberService.findByUserId(userId);

        log.info("OAuth2 code 교환 성공: userId={}", userId);

        return jwtProvider.createToken(
            member.getUserId(),
            member.getNickname(),
            member.getAuthorities()
        );
    }
}
