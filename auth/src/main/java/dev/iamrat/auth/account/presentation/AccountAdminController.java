package dev.iamrat.auth.account.presentation;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.core.account.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/accounts")
public class AccountAdminController {
    private final AccountCommandService accountCommandService;

    @PutMapping("/{accountId}/roles/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> grantAdminRole(@AuthenticationPrincipal UserPrincipal actor,
                                                @PathVariable Long accountId) {
        accountCommandService.grantAdminRole(actor.getAccountId(), accountId);
        return ResponseEntity.noContent().build();
    }
}
