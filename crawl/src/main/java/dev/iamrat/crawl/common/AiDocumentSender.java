package dev.iamrat.crawl.common;

import dev.iamrat.crawl.dto.DocumentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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

    public void triggerPostGeneration(String stockCode, String corpName) {
        try {
            aiRestClient.post()
                    .uri("/ai/posts/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "stockCode", stockCode,
                            "corpName", corpName,
                            "publish", true
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("게시글 자동 생성 완료 - {} ({})", corpName, stockCode);
        } catch (Exception e) {
            log.warn("게시글 자동 생성 실패 ({}) - {}", corpName, e.getMessage());
        }
    }
}
