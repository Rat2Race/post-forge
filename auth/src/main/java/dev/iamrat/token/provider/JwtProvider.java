package dev.iamrat.token.provider;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.login.dto.CustomUserDetails;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.token.dto.JwtResponse;
import dev.iamrat.token.service.JwtService;
import io.jsonwebtoken.Claims;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtService jwtService;
    private final MemberService memberService;

    public JwtResponse createToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String nickname = userDetails.getNickname();
        
        String accessToken = jwtService.generateAccessToken(authentication.getName(), nickname, authentication.getAuthorities());
        String refreshToken = jwtService.generateRefreshToken(authentication.getName());
        
        jwtService.saveOrUpdateRefreshToken(authentication.getName(), refreshToken);
        
        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public JwtResponse reissueToken(String refreshToken) {
        String userId = jwtService.parseClaims(refreshToken).getSubject();
        
        jwtService.getRefreshToken(userId).validateToken(refreshToken);
        
        Member member = memberService.findByUserId(userId);
        
        String newAccessToken = jwtService.generateAccessToken(userId, member.getNickname(), member.getAuthorities());
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        
        jwtService.saveOrUpdateRefreshToken(userId, newRefreshToken);
        
        return JwtResponse.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }
    
    public void deleteToken(String userId) {
        jwtService.deleteRefreshToken(userId);
    }
    
    public Authentication resolveAuthentication(String token) {
        Claims claims = jwtService.parseClaims(token);
        CustomUserDetails principal = buildUserDetails(claims);
 
        log.debug("[JWT] UserDetails 조회 성공 - authorities={}", principal.getAuthorities());
        
        return UsernamePasswordAuthenticationToken.authenticated(
            principal, token, principal.getAuthorities()
        );
    }
    
    private CustomUserDetails buildUserDetails(Claims claims) {
        String userId = claims.getSubject();
        
        if (userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        log.debug("[JWT] Claims 파싱 성공 - subject={}", userId);
        
        String nickname = claims.get("nickname", String.class);
        List<?> rawRoles = claims.get("roles", List.class);
        
        Collection<? extends GrantedAuthority> authorities = rawRoles != null
            ? rawRoles.stream()
                .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                .collect(Collectors.toList())
            : Collections.emptyList();
        
        return new CustomUserDetails(userId, "", nickname, authorities);
    }
}
