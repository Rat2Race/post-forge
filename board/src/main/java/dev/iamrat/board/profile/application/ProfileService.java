package dev.iamrat.board.profile.application;

import dev.iamrat.core.account.AccountProfileDetails;
import dev.iamrat.core.account.AccountProfileManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccountProfileManager accountProfileManager;

    public AccountProfileDetails getProfile(Long accountId) {
        return accountProfileManager.getProfile(accountId);
    }

    public void updateNickname(Long accountId, String nickname) {
        accountProfileManager.updateNickname(accountId, nickname);
    }

    public void updatePassword(Long accountId, String currentPassword, String newPassword) {
        accountProfileManager.updatePassword(accountId, currentPassword, newPassword);
    }
}
