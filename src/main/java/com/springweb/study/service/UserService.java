package com.springweb.study.service;

import com.springweb.study.dto.user.request.UserUpdateRequest;
import com.springweb.study.dto.user.response.UserDeleteResponse;
import com.springweb.study.dto.user.response.UserInfoResponse;
import com.springweb.study.dto.user.response.UserUpdateResponse;
import com.springweb.study.security.repository.UserRepo;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(UUID id) {
        return userRepo.findById(id)
                .map(UserInfoResponse::from)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원"));
    }

    @Transactional
    public UserDeleteResponse deleteUser(UUID id) {
        if (!userRepo.existsById(id))
            return new UserDeleteResponse(false);

        userRepo.deleteById(id);
        return new UserDeleteResponse(true);
    }

    @Transactional
    public UserUpdateResponse updateMember(UUID id, UserUpdateRequest request) {
        return userRepo.findById(id)
                .filter(member -> encoder.matches(request.password(), member.getPassword()))
                .map(member -> {
                    member.update(request, encoder);
                    return UserUpdateResponse.of(true, member);
                })
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));
    }
}
