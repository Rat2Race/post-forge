package dev.iamrat.auth.account.controller;

import dev.iamrat.auth.account.dto.AccountResponse;
import dev.iamrat.auth.account.dto.AccountUpdateRequest;
import dev.iamrat.auth.account.dto.PasswordUpdateRequest;
import dev.iamrat.auth.account.entity.Account;
import dev.iamrat.auth.account.service.AccountService;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/account")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<AccountResponse> getMyAccount(
            @AuthenticationPrincipal UserPrincipal userDetails) {

        Account account = accountService.findWithRolesById(userDetails.getAccountId());
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MessageResponse> updateNickname(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid AccountUpdateRequest request) {

        accountService.updateNickname(userDetails.getAccountId(), request.nickname());
        return ResponseEntity.ok(MessageResponse.of("닉네임 변경 완료"));
    }

    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid PasswordUpdateRequest request) {

        accountService.updatePassword(
                userDetails.getAccountId(),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호 변경 완료"));
    }
}
