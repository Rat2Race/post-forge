package dev.iamrat.auth.profile.controller;

import dev.iamrat.core.global.security.UserPrincipal;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.auth.member.dto.MemberResponse;
import dev.iamrat.auth.member.entity.Member;
import dev.iamrat.auth.profile.dto.PasswordChangeRequest;
import dev.iamrat.auth.profile.dto.ProfileUpdateRequest;
import dev.iamrat.auth.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<MemberResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userDetails) {

        Member member = profileService.getMember(userDetails.getUserId());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<MessageResponse> updateNickname(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid ProfileUpdateRequest request) {

        profileService.updateNickname(userDetails.getUserId(), request.nickname());
        return ResponseEntity.ok(MessageResponse.of("닉네임 변경 완료"));
    }

    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @RequestBody @Valid PasswordChangeRequest request) {

        profileService.changePassword(
                userDetails.getUserId(),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.ok(MessageResponse.of("비밀번호 변경 완료"));
    }
}
