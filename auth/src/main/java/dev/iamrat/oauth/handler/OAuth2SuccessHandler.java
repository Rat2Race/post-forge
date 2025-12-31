package dev.iamrat.oauth.handler;

import dev.iamrat.oauth.dto.CustomOAuth2User;
import dev.iamrat.token.dto.TokenResponse;
import dev.iamrat.token.entity.RefreshToken;
import dev.iamrat.token.provider.JwtProvider;
import dev.iamrat.token.repository.RefreshTokenRepository;
import dev.iamrat.token.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtProvider jwtProvider;
    private final TokenService tokenService;
    
    @Value("${spring.cors.allowed-origins:http://localhost:5173}")
    private String frontendUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        TokenResponse token = tokenService.createToken(authentication);
        
        log.info("OAuth2 로그인 성공, JWT 발급: userId={}", jwtProvider.getClaims(token.accessToken()).getId());
        
        String redirectUrl = frontendUrl + "/oauth/callback"
            + "?accessToken=" + token.accessToken()
            + "&refreshToken=" + token.refreshToken();
        
        response.sendRedirect(redirectUrl);
    }
}