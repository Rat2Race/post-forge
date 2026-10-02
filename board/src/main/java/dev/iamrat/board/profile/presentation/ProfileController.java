package dev.iamrat.board.profile.presentation;

import dev.iamrat.board.profile.application.ProfileService;
import dev.iamrat.core.account.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ProfileResponse.from(profileService.getProfile(user.getAccountId())));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody @Valid ProfileNicknameUpdateRequest request
    ) {
        profileService.updateNickname(user.getAccountId(), request.nickname());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody @Valid ProfilePasswordUpdateRequest request
    ) {
        profileService.updatePassword(user.getAccountId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
