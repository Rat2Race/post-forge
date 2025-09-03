package com.postforge.board.posts.service;

import com.postforge.common.RoleType;
import com.postforge.board.posts.domain.dto.user.response.UserInfoResponse;
import com.postforge.board.posts.domain.repository.UserRepo;
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