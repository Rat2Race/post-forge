package dev.iamrat.token.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.token.dto.TokenResponse;
import dev.iamrat.token.entity.RefreshToken;
import dev.iamrat.token.provider.JwtProvider;
import dev.iamrat.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    
    public TokenResponse createToken(Authentication authentication) {
        String accessToken = jwtProvider.createAccessToken(authentication);
        String refreshToken = jwtProvider.createRefreshToken(authentication.getName());
        
        saveRefreshToken(authentication.getName(), refreshToken);
        
        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public TokenResponse reissueToken(String refreshToken) {
        String userId = jwtProvider.getClaims(refreshToken).getSubject();
        
        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        savedToken.validateToken(refreshToken);
        
        Member member = memberService.findByUserId(userId);
        
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userId, null,
                member.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getValue()))
                    .toList()
            );
        
        String newAccessToken = jwtProvider.createAccessToken(authentication);
        
        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .build();
    }
    
    public void deleteRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
    
    private void saveRefreshToken(String userId, String token) {
        LocalDateTime expiryDate = jwtProvider.getClaims(token)
            .getExpiration()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
            .orElse(null);
        
        if(refreshToken == null) {
            refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
        } else {
            refreshToken.updateToken(token, expiryDate);
        }
        
        refreshTokenRepository.save(refreshToken);
    }
}
