package com.postforge.api.auth.service;

import com.postforge.domain.member.dto.CommonLoginRequest;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.entity.RefreshToken;
import com.postforge.domain.member.entity.Role;
import com.postforge.domain.member.repository.MemberRepository;
import com.postforge.domain.member.repository.RefreshTokenRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import com.postforge.global.security.dto.TokenResponse;
import com.postforge.global.security.jwt.JwtProperties;
import com.postforge.global.security.jwt.JwtTokenProvider;
import com.postforge.global.security.jwt.JwtUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommonAuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public Long register(CommonRegisterRequest request) {

        /** 사용자 인증 추가해야함 (이메일 인증) **/

        if (memberRepository.existsByUsername(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if (memberRepository.existsByUserId(request.id())) {
            throw new CustomException(ErrorCode.DUPLICATE_ID);
        }

        Member member = Member.builder()
            .username(request.name())
            .userId(request.id())
            .userPw(passwordEncoder.encode(request.pw()))
            .build();

        member.addRole(Role.USER);

        Member savedMember = memberRepository.save(member);
        log.info("새로운 회원 등록: {}", savedMember.getUsername());

        return savedMember.getId();
    }

    public TokenResponse login(CommonLoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
            UsernamePasswordAuthenticationToken.unauthenticated(request.id(), request.pw());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String accessToken = jwtUtil.createAccessToken(authentication);
        String refreshToken = jwtUtil.createRefreshToken(authentication.getName());

        saveRefreshToken(authentication.getName(), refreshToken);

        log.info("사용자 로그인: {}", authentication.getName());

        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    public TokenResponse reissueToken(String refreshToken) {

        /** 만료되면 CustomException **/
        String userId = jwtUtil.getClaims(refreshToken).getSubject();

        /** refreshToken repo에 userId 없으면 CustomException **/
        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        /** 정상적인 refreshToekn **/
        savedToken.validateToken(refreshToken);

        Member member = memberRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            userId, null, member.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getValue())).toList());

        String newToken = jwtUtil.createAccessToken(authentication);

        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(newToken)
            .refreshToken(refreshToken)
            .build();
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("사용자 로그아웃: {}", userId);
    }

    private void saveRefreshToken(String userId, String token) {
        LocalDateTime expiryDate = getLocalDateTimeFromDate(
            jwtUtil.getClaims(token).getExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
            .userId(userId)
            .token(token)
            .expiryDate(expiryDate)
            .build();

        refreshTokenRepository.save(refreshToken);
    }

    private LocalDateTime getLocalDateTimeFromDate(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }
}