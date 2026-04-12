package dev.iamrat.crawl.common;

import dev.iamrat.crawl.common.dto.DocumentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AiDocumentSender {

    private static final Logger log = LoggerFactory.getLogger(AiDocumentSender.class);

    private static final String DOCUMENTS_ENDPOINT = "/internal/crawl/documents";
    private static final String POST_GENERATION_ENDPOINT = "/internal/crawl/posts/generate";

    private final RestClient aiRestClient;

    public AiDocumentSender(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

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
            log.info("메인 앱 internal API에 {}건의 문서 전송 완료", requests.size());
            return true;
        } catch (Exception e) {
            log.error("메인 앱 internal 문서 전송 실패 - {}건, endpoint={}, error={}",
                    requests.size(), DOCUMENTS_ENDPOINT, e.getMessage(), e);
            return false;
        }
    }

    public boolean triggerPostGeneration(String stockCode, String corpName) {
        try {
            aiRestClient.post()
                    .uri(POST_GENERATION_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "stockCode", stockCode,
                            "corpName", corpName
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("메인 앱에 게시글 생성 요청 완료 - {} ({})", corpName, stockCode);
            return true;
        } catch (Exception e) {
            log.error("메인 앱 게시글 생성 요청 실패 - {} ({}), error={}", corpName, stockCode, e.getMessage(), e);
            return false;
        }
    }
}
