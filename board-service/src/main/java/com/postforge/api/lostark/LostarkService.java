package com.postforge.api.lostark;

import com.postforge.domain.lostark.AccountInfo;
import com.postforge.domain.lostark.CharacterInfo;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class LostarkService {

    private final RestClient restClient;

    @Value("${lostark.url}")
    private String lostarkUrl;

    @Value("${lostark.secret}")
    private String lostarkSecret;

    public List<AccountInfo> getAccountInform(String characterName) {

        URI uri = UriComponentsBuilder
            .fromHttpUrl(lostarkUrl)
            .path("/characters/{characterName}/siblings")
            .buildAndExpand(characterName)
            .encode()
            .toUri();

        return restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + lostarkSecret)
            .retrieve()
            .body(new ParameterizedTypeReference<List<AccountInfo>>(){});
    }

    public List<CharacterInfo> getCharacterInform(String characterName, String filters) {
        URI uri = UriComponentsBuilder
            .fromHttpUrl(lostarkUrl)
            .path("/armories/characters/{characterName}")
            .query(filters)
            .buildAndExpand(characterName)
            .encode()
            .toUri();

        return restClient.get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + lostarkSecret)
            .retrieve()
            .body(new ParameterizedTypeReference<List<CharacterInfo>>(){});

    }
}
