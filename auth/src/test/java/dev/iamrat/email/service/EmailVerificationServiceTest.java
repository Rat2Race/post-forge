package dev.iamrat.email.service;

import dev.iamrat.email.entity.EmailVerification;
import dev.iamrat.email.repository.EmailVerificationRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {
    
    @Mock
    EmailVerificationRepository emailVerificationRepository;
    
    @Mock
    MemberRepository memberRepository;
    
    @Mock
    EmailService emailService;
    
    @InjectMocks
    EmailVerificationService emailVerificationService;
    
    @Test
    @DisplayName("이미 가입된 이메일로 인증 요청하면 DUPLICATE_EMAIL 예외를 발생한다")
    void sendEmail_duplicate_throwsDuplicateEmail() {
        String mockEmail = "tester@test.com";
        
        given(memberRepository.existsByEmail(mockEmail)).willReturn(true);
        
        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(mockEmail))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }
    
    @Test
    @DisplayName("올바른 토큰이 아니면 EMAIL_CODE_NOT_FOUND 예외를 발생한다")
    void verify_invalidToken_throwsCodeNotFound() {
        String token = "쓰레기-토큰-입니다";
        
        given(emailVerificationRepository.findByToken(token))
            .willReturn(Optional.empty());
        
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND));
    }
    
    @Test
    @DisplayName("만료된 토큰이면 EMAIL_CODE_EXPIRED 예외를 발생한다")
    void verify_expiredToken_throwsCodeExpired() {
        String token = "만료된-쓰레기-토큰-입니다><";
        
        EmailVerification verification = EmailVerification.builder()
            .email("tester@test.com")
            .token(token)
            .expiryDate(LocalDateTime.now().minusHours(1))
            .verified(false)
            .build();
        
        given(emailVerificationRepository.findByToken(token))
            .willReturn(Optional.of(verification));
        
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED));
    }
    
    @Test
    @DisplayName("인증된 토큰이면 EMAIL_ALREADY_VERIFIED 예외를 발생한다")
    void verify_alreadyVerified_throwsAlreadyVerified() {
        String token = "인증된-도태-토큰-입니다><";
        
        EmailVerification verification = EmailVerification.builder()
            .email("tester@test.com")
            .token(token)
            .expiryDate(LocalDateTime.now().plusHours(1))
            .verified(true)
            .build();
        
        given(emailVerificationRepository.findByToken(token))
            .willReturn(Optional.of(verification));
        
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED));
    }
    
}