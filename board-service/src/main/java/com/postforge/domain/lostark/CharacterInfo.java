package com.postforge.domain.lostark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterInfo {
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

    // ✅ 추가 필요
//    @JsonProperty("ArkPassive")
//    private ArkPassive arkPassive;  // 음속돌파, 뭉가 정보

//    @JsonProperty("Stats")
//    private List<Stat> stats;  // 치명타, 특화 등
}