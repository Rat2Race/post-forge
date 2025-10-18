package com.postforge.domain.member.repository;

import com.postforge.domain.member.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUserId(String username);
    Optional<RefreshToken> deleteByUserId(String userId);

    String userId(String userId);
}