package com.postforge.domain.lostark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountInfo {
    @JsonProperty("ServerName")
    private String server;
    @JsonProperty("CharacterName")
    private String name;
    @JsonProperty("CharacterLevel")
    private int level;
    @JsonProperty("CharacterClassName")
    private String className;
    @JsonProperty("ItemAvgLevel")
    private String itemLevel;
}
