package dev.iamrat.common;

import dev.iamrat.common.dto.InternalDocumentPayload;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InternalCrawlClient {

    private static final Logger log = LoggerFactory.getLogger(InternalCrawlClient.class);

    private static final String DOCUMENTS_ENDPOINT = "/internal/crawl/documents";
    private static final String POST_GENERATION_ENDPOINT = "/internal/crawl/posts/generate";

    private final RestClient internalApiRestClient;

    public InternalCrawlClient(RestClient internalApiRestClient) {
        this.internalApiRestClient = internalApiRestClient;
    }

    public boolean sendDocuments(List<InternalDocumentPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return true;
        }

        try {
            internalApiRestClient.post()
                    .uri(DOCUMENTS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payloads)
                    .retrieve()
                    .toBodilessEntity();
            log.info("메인 앱 internal API에 {}건의 문서 전송 완료", payloads.size());
            return true;
        } catch (Exception e) {
            log.error("메인 앱 internal 문서 전송 실패 - {}건, endpoint={}, error={}",
                    payloads.size(), DOCUMENTS_ENDPOINT, e.getMessage(), e);
            return false;
        }
    }

    public boolean requestPostGeneration(String stockCode, String corpName) {
        try {
            internalApiRestClient.post()
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

