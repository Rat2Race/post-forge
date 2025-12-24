package com.postforge.member.service;

import com.postforge.member.dto.CommonLoginRequest;
import com.postforge.member.dto.CommonRegisterRequest;
import com.postforge.email.entity.EmailVerification;
import com.postforge.member.entity.Member;
import com.postforge.token.entity.RefreshToken;
import com.postforge.member.entity.Role;
import com.postforge.email.repository.EmailVerificationRepository;
import com.postforge.member.repository.MemberRepository;
import com.postforge.token.repository.RefreshTokenRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import com.postforge.token.dto.TokenResponse;
import com.postforge.token.service.JwtUtil;
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
@Transactional(readOnly = true)
@Slf4j
public class CommonAuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public Long register(CommonRegisterRequest request) {

        EmailVerification verification = emailVerificationRepository.findByEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_CODE_NOT_FOUND));

        if(!verification.getVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (memberRepository.existsByUserName(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if (memberRepository.existsByUserId(request.id())) {
            throw new CustomException(ErrorCode.DUPLICATE_ID);
        }

        Member member = Member.builder()
            .userName(request.name())
            .userId(request.id())
            .userPw(passwordEncoder.encode(request.pw()))
            .email(request.email())
            .nickname(request.nickname())
            .build();

        member.addRole(Role.USER);

        Member savedMember = memberRepository.save(member);
        log.info("새로운 회원 등록: {}", savedMember.getUserName());

        emailVerificationRepository.delete(verification);

        return savedMember.getId();
    }

    @Transactional
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

        /** JWT(RT) 검증 + userId 추출 **/
        String userId = jwtUtil.getClaims(refreshToken).getSubject();

        /** DB 검증 (조회 + 값 매칭) **/
        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
        savedToken.validateToken(refreshToken);

        Member member = memberRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        /** AT 발급 (인증된 사용자) **/
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            userId, null,
            member.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getValue()))
                .toList());

        String newToken = jwtUtil.createAccessToken(authentication);

        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(newToken)
            .refreshToken(refreshToken)
            .build();
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("사용자 로그아웃: {}", userId);
    }

    private void saveRefreshToken(String userId, String token) {
        LocalDateTime expiryDate = getLocalDateTimeFromDate(
            jwtUtil.getClaims(token).getExpiration());

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId).orElse(null);

        if (refreshToken == null) {
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

    private LocalDateTime getLocalDateTimeFromDate(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }
}