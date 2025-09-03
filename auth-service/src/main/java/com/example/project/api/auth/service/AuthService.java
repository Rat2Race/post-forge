package com.example.project.api.auth.service;

import com.example.project.domain.member.dto.LoginRequest;
import com.example.project.domain.member.dto.SignupRequest;
import com.example.project.domain.member.entity.Member;
import com.example.project.domain.member.entity.Role;
import com.example.project.domain.member.repository.MemberRepository;
import com.example.project.global.exception.CustomException;
import com.example.project.global.exception.ErrorCode;
import com.example.project.global.security.dto.TokenResponse;
import com.example.project.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    
    public Long signup(SignupRequest request) {
        // 중복 체크
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        
        // 회원 생성
        Member member = Member.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .nickname(request.getNickname())
            .build();
        
        // 기본 권한 부여
        member.addRole(Role.USER);
        
        Member savedMember = memberRepository.save(member);
        log.info("새로운 회원 가입: {}", savedMember.getUsername());
        
        return savedMember.getId();
    }
    
    public TokenResponse login(LoginRequest request) {
        // 인증 시도
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        
        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());
        
        // Refresh Token을 Redis에 저장 (7일)
        redisTemplate.opsForValue().set(
            "RT:" + authentication.getName(),
            refreshToken,
            7, TimeUnit.DAYS
        );
        
        log.info("사용자 로그인: {}", authentication.getName());
        
        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public TokenResponse reissueToken(String refreshToken) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        // 토큰에서 사용자명 추출
        String username = jwtTokenProvider.getUsername(refreshToken);
        
        // Redis에서 저장된 Refresh Token 확인
        String savedRefreshToken = redisTemplate.opsForValue().get("RT:" + username);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        // 새로운 Access Token 생성을 위한 인증 정보 조회
        Member member = memberRepository.findByUsernameWithRoles(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        // Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, 
                member.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getValue()))
                    .toList());
        
        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        
        log.info("토큰 재발급: {}", username);
        
        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public void logout(String username) {
        // Redis에서 Refresh Token 삭제
        if (redisTemplate.hasKey("RT:" + username)) {
            redisTemplate.delete("RT:" + username);
            log.info("사용자 로그아웃: {}", username);
        }
    }
}