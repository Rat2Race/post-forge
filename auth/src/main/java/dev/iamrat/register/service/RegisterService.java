package dev.iamrat.register.service;

import dev.iamrat.email.entity.EmailVerification;
import dev.iamrat.email.repository.EmailVerificationRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.register.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class RegisterService {
    
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    
    @Transactional
    public Long register(RegisterRequest request) {
        
        EmailVerification verification = emailVerificationRepository.findByEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_CODE_NOT_FOUND));
        
        if(!verification.getVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        
        if (memberService.existsByUserName(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        
        if (memberService.existsByUserId(request.id())) {
            throw new CustomException(ErrorCode.DUPLICATE_ID);
        }
        
        Member member = memberService.createMember(
            request.name(),
            request.id(),
            request.pw(),
            request.email(),
            request.nickname(),
            "LOCAL",
            null
        );
        
        emailVerificationRepository.delete(verification);
        
        return member.getId();
    }
}
