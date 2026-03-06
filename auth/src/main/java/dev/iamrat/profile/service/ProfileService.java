package dev.iamrat.profile.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.member.entity.Member;
import dev.iamrat.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member getMember(String userId) {
        return memberRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateNickname(String userId, String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        Member member = getMember(userId);
        member.updateProfile(nickname);
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        Member member = getMember(userId);

        if (member.getUserPw() == null) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD, "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다");
        }

        if (!passwordEncoder.matches(currentPassword, member.getUserPw())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        member.changePassword(newPassword, passwordEncoder);
    }
}
