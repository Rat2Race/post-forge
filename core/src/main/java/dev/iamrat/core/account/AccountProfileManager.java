package dev.iamrat.core.account;

public interface AccountProfileManager {
    AccountProfileDetails getProfile(Long accountId);

    void updateNickname(Long accountId, String nickname);

    void updatePassword(Long accountId, String currentPassword, String newPassword);
}
