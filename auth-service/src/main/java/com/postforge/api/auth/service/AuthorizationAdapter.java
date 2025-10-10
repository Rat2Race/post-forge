package com.postforge.api.auth.service;

import com.postforge.api.member.dto.MemberPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizationAdapter implements com.postforge.api.auth.AuthorizationService {

    private final AuthorizationServiceImpl authorizationService;

    @Override
    public boolean hasPermission(String username, String resource, String action) {
        return authorizationService.hasPermission(username, resource, action);
    }

    @Override
    public boolean hasRole(String username, String role) {
        return authorizationService.hasRole(username, role);
    }

    @Override
    public MemberPermission getPermissions(String username) {
        return authorizationService.getPermissions(username);
    }
}
