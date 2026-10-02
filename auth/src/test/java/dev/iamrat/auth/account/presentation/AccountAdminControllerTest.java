package dev.iamrat.auth.account.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.security.infrastructure.principal.AuthenticatedAccount;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountAdminControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AccountCommandService accountCommandService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void grantAdminRole_passesAuthenticatedActorAndTarget() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(1L), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(put("/api/admin/accounts/2/roles/admin"))
            .andExpect(status().isNoContent());

        verify(accountCommandService).grantAdminRole(1L, 2L);
    }
}
