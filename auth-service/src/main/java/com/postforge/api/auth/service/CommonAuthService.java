package com.postforge.api.auth.service;

import com.postforge.domain.member.dto.CommonLoginRequest;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.entity.Role;
import com.postforge.domain.member.repository.MemberRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import com.postforge.global.security.dto.TokenResponse;
import com.postforge.global.security.jwt.JwtTokenProvider;
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
public class CommonAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    public Long signup(CommonRegisterRequest request) {

        if (memberRepository.existsByUsername(request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if (memberRepository.existsByUserId(request.id())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
            .username(request.name())
            .userId(request.id())
            .userPw(passwordEncoder.encode(request.pw()))
            .build();

        member.addRole(Role.USER);

        Member savedMember = memberRepository.save(member);
        log.info("새로운 회원 가입: {}", savedMember.getUsername());

        return savedMember.getId();
    }

    public TokenResponse login(CommonLoginRequest request) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(request.id(), request.pw());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());

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

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String username = jwtTokenProvider.getUsername(refreshToken);

        String savedRefreshToken = redisTemplate.opsForValue().get("RT:" + username);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Member member = memberRepository.findByUsernameWithRoles(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null,
                member.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getValue()))
                    .toList());

        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);

        log.info("토큰 재발급: {}", username);

        return TokenResponse.builder()
            .grantType("Bearer")
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .build();
    }

    public void logout(String username) {
        if (redisTemplate.hasKey("RT:" + username)) {
            redisTemplate.delete("RT:" + username);
            log.info("사용자 로그아웃: {}", username);
        }
    }
}