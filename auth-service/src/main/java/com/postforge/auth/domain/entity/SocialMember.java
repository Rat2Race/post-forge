package com.postforge.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@IdClass(SocialMemberKey.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SocialMember {
    @Id
    @Column(name = "provider")
    private String provider;

    @Id
    @Column(name = "identifier")
    private String identifier;

    @Column(name = "user_id")
    private Long userId;
}
