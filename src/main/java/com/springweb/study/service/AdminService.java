package com.springweb.study.service;

import com.springweb.study.common.RoleType;
import com.springweb.study.dto.user.response.UserInfoResponse;
import com.springweb.study.repository.UserRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public List<UserInfoResponse> getMembers() {
        return userRepo.findAllByType(RoleType.USER.name()).stream()
                .map(UserInfoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserInfoResponse> getAdmins() {
        return userRepo.findAllByType(RoleType.ADMIN.name()).stream()
                .map(UserInfoResponse::from)
                .toList();
    }
}
