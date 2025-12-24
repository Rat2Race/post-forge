package com.postforge.email.entity;

import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "email_verifications")
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean verified = false;

    @PrePersist
    public void prePersist() {
        if(this.expiryDate == null) {
            this.expiryDate = LocalDateTime.now().plusMinutes(30);
        }
    }

    public void verifyToken() {
        if(LocalDateTime.now().isAfter(this.expiryDate)) {
            throw new CustomException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        if(this.verified) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        this.verified = true;
    }

    public void updateToken(String token, LocalDateTime expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }
}
