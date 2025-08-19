package com.postforge.auth.domain.entity;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class SocialMemberKey implements Serializable {
    private String provider;
    private String identifier;
}
