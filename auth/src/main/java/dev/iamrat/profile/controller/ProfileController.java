package dev.iamrat.profile.controller;

import dev.iamrat.member.dto.MemberResponse;
import dev.iamrat.login.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<MemberResponse> getMyProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        MemberResponse response = MemberResponse.builder()
            .userId(userDetails.userId())
            .roles(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAllMembers() {
        /** 따로 Admin 서비스 만들어서 넣으면 좋을거같음 **/
        return ResponseEntity.ok("ADMIN: 모든 회원 목록");
    }
}
