package com.postforge.api.auth;

import com.postforge.api.member.dto.MemberPermission;

public interface AuthorizationService {
    boolean hasPermission(String username, String resource, String action);
    boolean hasRole(String username, String role);
    MemberPermission getPermissions(String username);
}
