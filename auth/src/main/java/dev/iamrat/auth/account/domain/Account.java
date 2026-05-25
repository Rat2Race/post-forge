package dev.iamrat.auth.account.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_accounts_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_accounts_nickname", columnNames = "nickname"),
                @UniqueConstraint(name = "uk_accounts_provider_provider_id", columnNames = {"provider", "provider_id"})
        },
        indexes = {
                @Index(name = "idx_accounts_status", columnList = "status")
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Account {
    public static final String LOCAL_PROVIDER = "LOCAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "provider", nullable = false, length = 30)
    @Builder.Default
    private String provider = LOCAL_PROVIDER;

    @Column(name = "provider_id", length = 150)
    private String providerId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "account_roles",
            joinColumns = @JoinColumn(name = "account_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_account_roles_account_role", columnNames = {"account_id", "role"})
            }
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Set<AccountRole> roles = new HashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static Account createLocal(String username, String encodedPassword, String email, String nickname) {
        Account account = Account.builder()
            .username(username)
            .password(encodedPassword)
            .email(email)
            .nickname(nickname)
            .provider(LOCAL_PROVIDER)
            .providerId(null)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }

    public static Account createOAuth(String provider, String providerId, String email, String nickname) {
        Account account = Account.builder()
            .username(provider.toLowerCase(Locale.ROOT) + "_" + providerId)
            .password(null)
            .email(email)
            .nickname(nickname)
            .provider(provider)
            .providerId(providerId)
            .build();
        account.addRole(AccountRole.USER);
        return account;
    }

    public void addRole(AccountRole role) {
        this.roles.add(role);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateStatus(AccountStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status.equals(AccountStatus.ACTIVE);
    }

    public boolean isLocalAccount() {
        return LOCAL_PROVIDER.equals(provider);
    }
}
