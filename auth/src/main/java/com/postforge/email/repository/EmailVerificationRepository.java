package com.postforge.email.repository;

import com.postforge.email.entity.EmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByToken(String token);
    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiryDate < :dateTime")
    void deleteByExpiryDateBefore(@Param("dateTime") LocalDateTime dateTime);
}
