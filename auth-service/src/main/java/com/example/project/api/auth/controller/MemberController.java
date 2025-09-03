package com.example.project.api.auth.controller;

import com.example.project.domain.member.dto.MemberResponse;
import com.example.project.global.security.service.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    
    // 본인 정보 조회 (USER, ADMIN)
    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<MemberResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberResponse response = MemberResponse.builder()
            .id(userDetails.getId())
            .username(userDetails.getUsername())
            .email(userDetails.getEmail())
            .roles(userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    // 모든 회원 조회 (ADMIN)
    @GetMapping("/admin/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAllMembers() {
        return ResponseEntity.ok("ADMIN: 모든 회원 목록");
    }
    
    // 매니저 전용 (MANAGER, ADMIN)
    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<String> getManagerDashboard() {
        return ResponseEntity.ok("MANAGER: 대시보드");
    }
}