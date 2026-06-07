package dev.iamrat.board.profile.presentation;

import dev.iamrat.board.profile.application.ProfileService;
import dev.iamrat.board.profile.presentation.dto.ProfileNicknameUpdateRequest;
import dev.iamrat.board.profile.presentation.dto.ProfilePasswordUpdateRequest;
import dev.iamrat.board.profile.presentation.dto.ProfileResponse;
import dev.iamrat.core.account.UserPrincipal;
import dev.iamrat.core.global.dto.MessageResponse;
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
@RequestMapping("/user/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ProfileResponse.from(profileService.getProfile(user.getAccountId())));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MessageResponse> updateNickname(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody @Valid ProfileNicknameUpdateRequest request
    ) {
        profileService.updateNickname(user.getAccountId(), request.nickname());
        return ResponseEntity.ok(MessageResponse.of("닉네임 변경 완료"));
    }

    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> updatePassword(
        @AuthenticationPrincipal UserPrincipal user,
        @RequestBody @Valid ProfilePasswordUpdateRequest request
    ) {
        profileService.updatePassword(user.getAccountId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호 변경 완료"));
    }
}
