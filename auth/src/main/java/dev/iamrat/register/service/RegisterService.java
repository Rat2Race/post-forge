package dev.iamrat.register.service;

import dev.iamrat.auth.exception.AuthErrorCode;
import dev.iamrat.email.service.EmailVerificationService;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.register.dto.RegisterRequest;
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
