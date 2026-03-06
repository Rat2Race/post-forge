package dev.iamrat.profile.controller;

import dev.iamrat.login.dto.CustomUserDetails;
import dev.iamrat.member.dto.MemberResponse;
import dev.iamrat.member.entity.Member;
import dev.iamrat.profile.dto.PasswordChangeRequest;
import dev.iamrat.profile.dto.ProfileUpdateRequest;
import dev.iamrat.profile.service.ProfileService;
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
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        Member member = profileService.getMember(userDetails.userId());

        MemberResponse response = MemberResponse.builder()
            .id(member.getId())
            .userId(member.getUserId())
            .roles(member.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<String> updateNickname(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid ProfileUpdateRequest request) {

        profileService.updateNickname(userDetails.userId(), request.nickname());
        return ResponseEntity.ok("닉네임 변경 완료");
    }

    @PatchMapping("/password")
    public ResponseEntity<String> changePassword(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid PasswordChangeRequest request) {

        profileService.changePassword(
            userDetails.userId(),
            request.currentPassword(),
            request.newPassword()
        );
        return ResponseEntity.ok("비밀번호 변경 완료");
    }
}
