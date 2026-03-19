package dev.iamrat.profile.controller;

import dev.iamrat.member.dto.MemberResponse;
import dev.iamrat.member.entity.Member;
import dev.iamrat.profile.dto.PasswordChangeRequest;
import dev.iamrat.profile.dto.ProfileUpdateRequest;
import dev.iamrat.profile.service.ProfileService;
import dev.iamrat.security.dto.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
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

        MemberResponse response = MemberResponse.builder()
            .id(member.getId())
            .userId(member.getUserId())
            .nickname(member.getNickname())
            .provider(member.getProvider())
            .isOAuthUser(member.getProvider() != null)
            .roles(member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<String> updateNickname(
        @AuthenticationPrincipal UserPrincipal userDetails,
        @RequestBody @Valid ProfileUpdateRequest request) {

        profileService.updateNickname(userDetails.getUserId(), request.nickname());
        return ResponseEntity.ok("닉네임 변경 완료");
    }

    @PatchMapping("/password")
    public ResponseEntity<String> changePassword(
        @AuthenticationPrincipal UserPrincipal userDetails,
        @RequestBody @Valid PasswordChangeRequest request) {

        profileService.changePassword(
            userDetails.getUserId(),
            request.currentPassword(),
            request.newPassword()
        );
        return ResponseEntity.ok("비밀번호 변경 완료");
    }
}
