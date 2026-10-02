package dev.iamrat.auth.account.presentation;

import dev.iamrat.auth.account.application.AccountCommandService;
import dev.iamrat.auth.account.application.AccountQueryService;
import dev.iamrat.auth.account.domain.Account;
import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.account.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/account")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class AccountController {

    private final AccountCommandService accountCommandService;
    private final AccountQueryService accountQueryService;

    @GetMapping
    public ResponseEntity<AccountResponse> getMyAccount(
            @AuthenticationPrincipal UserPrincipal userDetails) {

        Account account = accountQueryService.findWithRolesById(userDetails.getAccountId())
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid AccountUpdateRequest request) {

        accountCommandService.updateNickname(
                userDetails.getAccountId(),
                request.nickname());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid PasswordUpdateRequest request) {

        accountCommandService.updatePassword(
                userDetails.getAccountId(),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
