package dev.iamrat.auth.member.entity;

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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@Table(
    name = "members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_members_provider_provider_id",
        columnNames = {"provider", "provider_id"}
    ),
    indexes = @Index(name = "idx_members_created_at", columnList = "created_at")
)
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String userId;

    @Column
    private String userPw;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;
    
    @Column(length = 20)
    private String provider;
    
    @Column(length = 100)
    private String providerId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "member_roles",
        joinColumns = @JoinColumn(name = "member_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_member_roles_member_role",
            columnNames = {"member_id", "role"}
        )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public List<SimpleGrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getValue()))
            .toList();
    }
    
    public void addRole(Role role) {
        this.roles.add(role);
    }
    
    public void updateProfile(String nickname) {
        this.nickname = nickname;
    }
    
    public void changePassword(String newPassword, PasswordEncoder passwordEncoder) {
        this.userPw  = passwordEncoder.encode(newPassword);
    }
}
