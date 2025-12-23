package com.postforge.api.auth.service;

import com.postforge.domain.member.entity.EmailVerification;
import com.postforge.domain.member.repository.EmailVerificationRepository;
import com.postforge.domain.member.repository.MemberRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    public void sendVerificationEmail(String email) {
        if(memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        emailVerificationRepository.deleteByEmail(email);

        String token = UUID.randomUUID().toString();

        EmailVerification verification = EmailVerification.builder()
            .email(email)
            .token(token)
            .build();

        emailVerificationRepository.save(verification);

        emailService.sendVerificationEmail(email, token);
    }

    public String verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
            .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_CODE_NOT_FOUND));

        verification.verifyToken();

        return verification.getEmail();
    }


}
