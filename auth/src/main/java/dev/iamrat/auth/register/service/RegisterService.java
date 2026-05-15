package dev.iamrat.auth.register.service;

import dev.iamrat.auth.error.AuthErrorCode;
import dev.iamrat.auth.email.service.EmailVerificationService;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.member.entity.Member;
import dev.iamrat.auth.member.service.MemberService;
import dev.iamrat.auth.register.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterService {
    private final MemberService memberService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public Long register(RegisterRequest request) {

        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (memberService.existsByUserId(request.userId())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_ID);
        }

        Member member = memberService.createMember(
            request.userId(),
            request.password(),
            request.email(),
            request.nickname(),
            "LOCAL",
            null
        );

        emailVerificationService.removeVerifiedEmail(request.email());

        return member.getId();
    }
}
