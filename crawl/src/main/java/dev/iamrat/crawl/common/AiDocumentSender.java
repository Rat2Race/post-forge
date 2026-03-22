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

    private static final String DOCUMENTS_ENDPOINT = "/ai/documents";
    private static final String POST_GENERATION_ENDPOINT = "/ai/posts/generate";

    private final RestClient aiRestClient;

    public boolean send(List<DocumentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return true;
        }

        try {
            aiRestClient.post()
                    .uri(DOCUMENTS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requests)
                    .retrieve()
                    .toBodilessEntity();
            log.info("AI 모듈에 {}건의 문서 전송 완료", requests.size());
            return true;
        } catch (Exception e) {
            log.error("AI 모듈 문서 전송 실패 - {}건, endpoint={}, error={}",
                    requests.size(), DOCUMENTS_ENDPOINT, e.getMessage(), e);
            return false;
        }
    }

    public void triggerPostGeneration(String stockCode, String corpName) {
        try {
            aiRestClient.post()
                    .uri(POST_GENERATION_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "stockCode", stockCode,
                            "corpName", corpName,
                            "publish", true
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("게시글 자동 생성 요청 완료 - {} ({})", corpName, stockCode);
        } catch (Exception e) {
            log.error("게시글 자동 생성 요청 실패 - {} ({}), error={}", corpName, stockCode, e.getMessage(), e);
        }
    }
}
