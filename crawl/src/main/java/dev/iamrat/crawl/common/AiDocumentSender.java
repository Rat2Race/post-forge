package dev.iamrat.crawl.common;

import dev.iamrat.crawl.dto.DocumentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiDocumentSender {

    private final RestClient aiRestClient;

    public boolean send(List<DocumentRequest> requests) {
        try {
            aiRestClient.post()
                    .uri("/ai/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requests)
                    .retrieve()
                    .toBodilessEntity();
            log.info("AI 모듈에 {}건의 문서 전송 완료", requests.size());
            return true;
        } catch (Exception e) {
            log.warn("AI 모듈 전송 실패 ({}건) - {}", requests.size(), e.getMessage());
            return false;
        }
    }
}
