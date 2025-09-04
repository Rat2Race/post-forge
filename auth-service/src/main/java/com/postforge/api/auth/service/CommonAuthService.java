package com.postforge.api.auth.service;

import com.postforge.common.RoleType;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import com.postforge.domain.member.entity.Member;
import com.postforge.domain.member.repository.MemberRepository;
import com.postforge.domain.member.repository.SocialMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonAuthService {

    private final SocialMemberRepository socialMemberRepository;
    private final MemberRepository userRepository;
    private final PasswordEncoder passwordEncoder;

//	@Transactional
//	public void createUser(Account account) {
//		userRepository.save(account);
//	}
//
//	@Transactional
//	public Account findMemberBySocialIdentifier(String provider, String identifier) {
//		return socialMemberRepository
//				.findById(new SocialMemberKey(provider, identifier))
//				.flatMap(socialMember -> userRepository.findById(socialMember.getUserId()))
//				.orElse(null);
//	}
//
//	@Transactional
//	public Account createMemberWithSocialIdentifier(String username, String provider, String identifier) {
//		Account account = new Account(0L, username, "", "", 0);
//		userRepository.save(account);
//		socialMemberRepository.save(new SocialMember(provider, identifier, account.getId()));
//		return account;
//	}

    @Transactional
    public Long save(CommonRegisterRequest request) {
        if (request.id() == null || request.pw() == null) {
            throw new IllegalArgumentException("사용자 ID 또는 PW 값을 넣어야 합니다.");
        }

        if (request.id().length() < 8) {
            throw new IllegalArgumentException("사용자 ID는 8자 이상 입니다.");
        }

        if (request.pw().length() < 8) {
            throw new IllegalArgumentException("사용자 PW는 8자 이상 입니다.");
        }

        if (userRepository.findByUsername(request.name()).isPresent()) {
            throw new IllegalArgumentException("중복된 사용자 입니다.");
        }

        if (userRepository.findByUserId(request.id()).isPresent()) {
            throw new IllegalArgumentException("중복된 아이디 입니다.");
        }

        Member member = Member.builder()
            .username(request.name())
            .userId(request.id())
            .userPw(passwordEncoder.encode(request.pw()))
            .build();

        return userRepository.save(member).getId();
    }
}
