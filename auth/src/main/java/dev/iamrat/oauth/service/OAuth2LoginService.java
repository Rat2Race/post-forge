package dev.iamrat.oauth.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.provider.JwtProvider;
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
            throw new CustomException(ErrorCode.INVALID_INPUT);
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
