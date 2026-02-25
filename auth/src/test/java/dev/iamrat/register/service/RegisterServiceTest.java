package dev.iamrat.register.service;

import dev.iamrat.email.entity.EmailVerification;
import dev.iamrat.email.repository.EmailVerificationRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.register.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private MemberService memberService;
    
    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @InjectMocks
    private RegisterService registerService;

    private RegisterRequest createValidRequest() {
        return new RegisterRequest("홍길동", "testuser1", "Test1234!", "test@example.com", "길동이");
    }

    private EmailVerification createVerifiedEmailVerification() {
        return EmailVerification.builder()
            .email("test@example.com")
            .token("valid-token")
            .expiryDate(java.time.LocalDateTime.now().plusMinutes(30))
            .verified(true)
            .build();
    }

    private EmailVerification createUnverifiedEmailVerification() {
        return EmailVerification.builder()
            .email("test@example.com")
            .token("valid-token")
            .expiryDate(java.time.LocalDateTime.now().plusMinutes(30))
            .verified(false)
            .build();
    }

    @Nested
    @DisplayName("회원가입 성공")
    class RegisterSuccess {

        @Test
        @DisplayName("모든 조건을 만족하면 회원을 생성하고 ID를 반환한다")
        void register_allConditionsMet_returnsMemberId() {
            RegisterRequest request = createValidRequest();
            EmailVerification verification = createVerifiedEmailVerification();

            given(emailVerificationRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(verification));
            given(memberService.existsByUserName("홍길동")).willReturn(false);
            given(memberService.existsByUserId("testuser1")).willReturn(false);

            Member member = Member.builder()
                .userId("testuser1")
                .userName("홍길동")
                .email("test@example.com")
                .nickname("길동이")
                .build();
            
            given(memberService.createMember("홍길동", "testuser1", "Test1234!",
                "test@example.com", "길동이", "LOCAL", null))
                .willReturn(member);

            Long result = registerService.register(request);

            assertThat(result).isEqualTo(member.getId());
            verify(memberService).createMember("홍길동", "testuser1", "Test1234!",
                "test@example.com", "길동이", "LOCAL", null);
            verify(emailVerificationRepository).delete(verification);
        }
    }

    @Nested
    @DisplayName("회원가입 실패")
    class RegisterFail {

        @Test
        @DisplayName("이메일 인증 기록이 없으면 EMAIL_CODE_NOT_FOUND 예외를 던진다")
        void register_emailVerificationMissing_throwsEmailCodeNotFound() {
            RegisterRequest request = createValidRequest();
            given(emailVerificationRepository.findByEmail("test@example.com"))
                .willReturn(Optional.empty());

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_CODE_NOT_FOUND));

            verify(memberService, never()).createMember(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이메일 인증이 완료되지 않았으면 EMAIL_NOT_VERIFIED 예외를 던진다")
        void register_emailNotVerified_throwsEmailNotVerified() {
            RegisterRequest request = createValidRequest();
            EmailVerification verification = createUnverifiedEmailVerification();

            given(emailVerificationRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(verification));

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));

            verify(memberService, never()).createMember(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 존재하는 사용자명이면 DUPLICATE_USERNAME 예외를 던진다")
        void register_duplicateUsername_throwsDuplicateUsername() {
            RegisterRequest request = createValidRequest();
            EmailVerification verification = createVerifiedEmailVerification();

            given(emailVerificationRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(verification));
            given(memberService.existsByUserName("홍길동")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_USERNAME));

            verify(memberService, never()).createMember(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("이미 존재하는 아이디이면 DUPLICATE_ID 예외를 던진다")
        void register_duplicateUserId_throwsDuplicateId() {
            RegisterRequest request = createValidRequest();
            EmailVerification verification = createVerifiedEmailVerification();

            given(emailVerificationRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(verification));
            given(memberService.existsByUserName("홍길동")).willReturn(false);
            given(memberService.existsByUserId("testuser1")).willReturn(true);

            assertThatThrownBy(() -> registerService.register(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_ID));

            verify(memberService, never()).createMember(any(), any(), any(), any(), any(), any(), any());
        }
    }
}
