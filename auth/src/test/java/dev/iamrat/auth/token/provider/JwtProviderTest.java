package dev.iamrat.auth.token.provider;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.auth.login.dto.CustomUserDetails;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.entity.Role;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.auth.token.dto.JwtResponse;
import dev.iamrat.auth.token.service.JwtService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private JwtProvider jwtProvider;

    private static final String USER_ID = "testuser1";

    @Nested
    @DisplayName("토큰 생성")
    class CreateToken {

        @Test
        @DisplayName("사용자 식별자와 권한으로 토큰을 생성한다")
        void createToken_validUserContext_returnsJwtResponse() {
            given(jwtService.generateAccessToken(any(), any(), any())).willReturn("access-token");
            given(jwtService.generateRefreshToken(any())).willReturn("refresh-token");

            JwtResponse result = jwtProvider.createToken(
                USER_ID,
                "tester",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            verify(jwtService).generateAccessToken(
                eq(USER_ID),
                eq("tester"),
                eq(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            );
            verify(jwtService).generateRefreshToken(eq(USER_ID));
            verify(jwtService).saveRefreshToken(eq(USER_ID), eq("refresh-token"));
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

            doNothing().when(jwtService).validateRefreshToken(USER_ID, "old-refresh-token");

            Account account = Account.builder()
                .userId(USER_ID)
                .email("test@test.com")
                .nickname("tester")
                .roles(Set.of(Role.USER))
                .build();

            given(accountService.findByUserId(USER_ID)).willReturn(account);

            given(jwtService.generateAccessToken(eq(USER_ID), any(), any())).willReturn("new-access-token");
            given(jwtService.generateRefreshToken(USER_ID)).willReturn("new-refresh-token");

            JwtResponse result = jwtProvider.reissueToken("old-refresh-token");

            assertThat(result.grantType()).isEqualTo("Bearer");
            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
            verify(jwtService).saveRefreshToken(eq(USER_ID), eq("new-refresh-token"));
        }

        @Test
        @DisplayName("저장된 토큰과 다르면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_tokenMismatch_throwsInvalidToken() {
            Claims claims = mock(Claims.class);
            given(claims.getSubject()).willReturn(USER_ID);
            given(jwtService.parseClaims("wrong-refresh-token")).willReturn(claims);

            doThrow(new CustomException(AuthErrorCode.INVALID_TOKEN))
                .when(jwtService).validateRefreshToken(USER_ID, "wrong-refresh-token");

            assertThatThrownBy(() -> jwtProvider.reissueToken("wrong-refresh-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("파싱에 실패하면 INVALID_TOKEN 예외를 던진다")
        void reissueToken_parsingFails_throwsInvalidToken() {
            given(jwtService.parseClaims("malformed-token"))
                .willThrow(new CustomException(AuthErrorCode.INVALID_TOKEN));

            assertThatThrownBy(() -> jwtProvider.reissueToken("malformed-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(exception ->
                    assertThat(((CustomException) exception).getErrorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
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
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
        }
    }
}
