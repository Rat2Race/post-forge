package dev.iamrat.auth.account.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.account.presentation.dto.AccountUpdateRequest;
import dev.iamrat.auth.account.presentation.dto.PasswordUpdateRequest;
import dev.iamrat.auth.security.infrastructure.principal.AuthenticatedAccount;
import dev.iamrat.auth.support.web.TestExceptionResponseHandler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestExceptionResponseHandler.class)
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AccountCommandService accountCommandService;

    @MockitoBean
    AccountQueryService accountQueryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 계정 조회는 인증 계정 ID로 계정을 조회해 응답 DTO로 반환한다")
    void getMyAccount_authenticatedUser_returnsAccountResponse() throws Exception {
        authenticateAccount(1L);
        Account account = Account.createLocal("testuser1", "encoded-password", "test@example.com", "길동이");
        given(accountQueryService.findWithRolesById(1L)).willReturn(account);

        mockMvc.perform(get("/user/account"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser1"))
            .andExpect(jsonPath("$.nickname").value("길동이"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("닉네임 변경은 인증 계정 ID와 요청 닉네임을 command service에 위임한다")
    void updateNickname_authenticatedUser_delegatesToCommandService() throws Exception {
        authenticateAccount(1L);
        AccountUpdateRequest request = new AccountUpdateRequest("새닉네임");

        mockMvc.perform(patch("/user/account/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("닉네임 변경 완료"));

        verify(accountCommandService).updateNickname(1L, "새닉네임");
    }

    @Test
    @DisplayName("비밀번호 변경은 인증 계정 ID와 현재/새 비밀번호를 command service에 위임한다")
    void updatePassword_authenticatedUser_delegatesToCommandService() throws Exception {
        authenticateAccount(1L);
        PasswordUpdateRequest request = new PasswordUpdateRequest("Old1234!", "New1234!");

        mockMvc.perform(patch("/user/account/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("비밀번호 변경 완료"));

        verify(accountCommandService).updatePassword(1L, "Old1234!", "New1234!");
    }

    private void authenticateAccount(Long accountId) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(accountId),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            )
        );
    }
}
