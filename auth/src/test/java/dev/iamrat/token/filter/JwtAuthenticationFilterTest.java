package dev.iamrat.token.filter;


import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.token.provider.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    JwtProvider jwtProvider;
    
    @Mock
    FilterChain filterChain;
    
    @InjectMocks
    JwtAuthenticationFilter filter;
    
    @AfterEach
    void securityContextClear() {
        SecurityContextHolder.clearContext();
    }
    
    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 저장하고 필터를 통과한다")
    void doFilter_validToken_setAuthenticationAndPass() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockRequest.addHeader("Authorization", "Bearer valid-token");
        
        Authentication authentication = mock(Authentication.class);
        given(jwtProvider.resolveAuthentication("valid-token"))
            .willReturn(authentication);
        
        filter.doFilterInternal(mockRequest, mockResponse, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication())
            .isEqualTo(authentication);
        verify(filterChain).doFilter(mockRequest, mockResponse);
    }
    
    @Test
    @DisplayName("만료된 토큰이면 SecurityContext에 인증 정보 없이 필터를 통과한다")
    void doFilter_expiredToken_continuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockRequest.addHeader("Authorization", "Bearer expired-token");
        
        given(jwtProvider.resolveAuthentication("expired-token"))
            .willThrow(new CustomException(ErrorCode.EXPIRED_TOKEN));
        
        filter.doFilterInternal(mockRequest, mockResponse, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(mockRequest, mockResponse);
    }
    
    @Test
    @DisplayName("Authorization 헤더가 없으면 SecurityContext에 인증 정보 없이 필터를 통과한다")
    void doFilter_noAuthorizationHeader_continuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        
        filter.doFilterInternal(mockRequest, mockResponse, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(mockRequest, mockResponse);
    }
    
    @Test
    @DisplayName("Bearer 접두사가 없으면 SecurityContext에 인증 정보 없이 필터를 통과한다")
    void doFilter_noBearerPrefix_continuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockRequest.addHeader("Authorization", "not-Bear-token");
        
        filter.doFilterInternal(mockRequest, mockResponse, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(mockRequest, mockResponse);
    }
    
    @Test
    @DisplayName("예상치 못한 예외가 발생해도 SecurityContext에 인증 정보 없이 필터를 통과한다")
    void doFilter_unexpectedException_continuesWithoutAuthentication() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockRequest.addHeader("Authorization", "Bearer unexpected-token");
        
        given(jwtProvider.resolveAuthentication("unexpected-token"))
            .willThrow(RuntimeException.class);
        
        filter.doFilterInternal(mockRequest, mockResponse, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        verify(filterChain).doFilter(mockRequest, mockResponse);
    }
}