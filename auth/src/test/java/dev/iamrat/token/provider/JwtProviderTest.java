package dev.iamrat.token.provider;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.login.dto.CustomUserDetails;
import dev.iamrat.login.service.CustomUserDetailsService;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.entity.RefreshToken;
import dev.iamrat.token.service.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private MemberService memberService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JwtProvider jwtProvider;

    private static final String USER_ID = "testuser1";

    @Nested
    @DisplayName("토큰 생성")
    class CreateToken {

        @Test
        @DisplayName("인증 정보로 토큰을 생성한다")
        void createToken_validAuthentication_returnsJwtResponse() {
            given(jwtService.generateAccessToken(any(), any(), any())).willReturn("access-token");
            given(jwtService.generateRefreshToken(any())).willReturn("refresh-token");
            given(authentication.getName()).willReturn(USER_ID);
            CustomUserDetails userDetails = new CustomUserDetails(USER_ID, "pw", "tester", Collections.emptyList());
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(authentication.getAuthorities()).willReturn(Collections.emptyList());

            JwtResponse result = jwtProvider.createToken(authentication);

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(jwtService).generateAccessToken(eq(USER_ID), eq("tester"), any());
            verify(jwtService).generateRefreshToken(eq(USER_ID));
            verify(jwtService).saveOrUpdateRefreshToken(eq(USER_ID), eq("refresh-token"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueToken {

        @Test
        @DisplayName("유효한 리프레시 토큰이면 새 토큰을 발급한다")
        void reissueToken_validRefreshToken_returnsJwtResponse() {
            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn(USER_ID);
            given(jwtService.parseClaims("old-refresh-token")).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                .userId(USER_ID)
                .token("old-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
            given(jwtService.getRefreshToken(USER_ID)).willReturn(storedToken);

            Member member = Member.builder()
                .userId(USER_ID)
                .userName("tester")
                .email("test@test.com")
                .nickname("tester")
                .roles(Set.of(Role.USER))
                .build();
            
            given(memberService.findByUserId(USER_ID)).willReturn(member);
            
            given(jwtService.generateAccessToken(eq(USER_ID), any(), any())).willReturn("new-access-token");
            given(jwtService.generateRefreshToken(USER_ID)).willReturn("new-refresh-token");

            JwtResponse result = jwtProvider.reissueToken("old-refresh-token");

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
            verify(jwtService).saveOrUpdateRefreshToken(eq(USER_ID), eq("new-refresh-token"));
        }

        @Test
        @DisplayName("저장된 토큰과 다르면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_tokenMismatch_throwsInvalidToken() {
            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn(USER_ID);
            given(jwtService.parseClaims("wrong-refresh-token")).willReturn(claims);

            RefreshToken storedToken = RefreshToken.builder()
                .userId(USER_ID)
                .token("correct-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
            
            given(jwtService.getRefreshToken(USER_ID)).willReturn(storedToken);

            assertThatThrownBy(() -> jwtProvider.reissueToken("wrong-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("파싱에 실패하면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_parsingFails_throwsInvalidToken() {
            given(jwtService.parseClaims("malformed-token"))
                .willThrow(new CustomException(ErrorCode.INVALID_TOKEN));

            assertThatThrownBy(() -> jwtProvider.reissueToken("malformed-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }

    @Nested
    @DisplayName("토큰 삭제")
    class DeleteToken {

        @Test
        @DisplayName("사용자 ID로 리프레시 토큰을 삭제한다")
        void deleteToken_userIdProvided_deletesRefreshToken() {
            jwtProvider.deleteToken(USER_ID);

            verify(jwtService).deleteRefreshToken(eq(USER_ID));
        }
    }

    @Nested
    @DisplayName("Authentication 파서")
    class ResolveAuthentication {

        @Test
        @DisplayName("유효한 토큰이면 Authentication을 반환한다")
        void resolveAuthentication_validToken_returnsAuthentication() {
            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn(USER_ID);
            given(claims.get("nickname", String.class)).willReturn("tester");
            given(claims.get("roles", List.class)).willReturn(List.of("ROLE_USER"));
            given(jwtService.parseClaims("valid-token")).willReturn(claims);
            
            Authentication result = jwtProvider.resolveAuthentication("valid-token");

            assertThat(result.getName()).isEqualTo(USER_ID);
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            
            CustomUserDetails principal = (CustomUserDetails) result.getPrincipal();
            assertThat(principal.getNickname()).isEqualTo("tester");
        }

        @Test
        @DisplayName("subject가 빈 값이면 예외가 발생한다")
        void resolveAuthentication_blankSubject_throwsInvalidToken() {
            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn("");
            given(jwtService.parseClaims("token-without-subject")).willReturn(claims);

            assertThatThrownBy(() -> jwtProvider.resolveAuthentication("token-without-subject"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }
}
