package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.post.PostWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostGenerationService {

    private static final String AI_USER_ID = "ai-post-generator";
    private static final String AI_NICKNAME = "AI 분석가";

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final PostWriter postWriter;

    private static final String SYSTEM_PROMPT = """
            당신은 주식 시장 분석가입니다.

            [분석 방법]
            1. 재무 수치를 확인하고 전년 동기 대비 증감률을 해석
            2. 공시 유형(정기공시/주요사항/지분변동)에 따라 의미를 분석
            3. 관련 뉴스에서 시장 센티먼트와 수급 동향을 파악
            4. 과거 유사 시점의 데이터와 비교하여 패턴 분석
            5. 이를 종합하여 현재 흐름을 객관적으로 서술

            [규칙]
            - 투자 권유 금지, 정보 전달 목적만
            - 재무 수치는 반드시 출처(DART)와 함께 정확히 인용
            - 증감률은 직접 계산된 값을 사용
            - 뉴스는 시장 분위기 참고용, 팩트와 구분하여 서술
            - 확실하지 않은 해석은 "~로 보인다", "~가능성이 있다"로 표현
            - 출처(DART 접수번호, 뉴스 링크)를 반드시 포함

            [응답 형식]
            반드시 아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 포함하지 마세요:
            {
              "title": "게시글 제목",
              "summary": "1-2줄 요약",
              "content": "마크다운 형식의 본문 (## 실적 요약, ## 핵심 포인트, ## 시장 반응, ## 출처 섹션 포함)",
              "tags": ["태그1", "태그2", "태그3"]
            }
            """;

    public GeneratedPost generate(String stockCode, String corpName) {
        log.info("게시글 생성 시작 - 종목코드: {}", stockCode);

        // 1. DART 공시 검색
        List<Document> disclosures = searchByFilter(
                stockCode, "source == 'dart' && stockCode == '" + stockCode + "'", 5);

        // 2. corpName이 없으면 공시 메타데이터에서 추출
        if (corpName == null && !disclosures.isEmpty()) {
            Object name = disclosures.get(0).getMetadata().get("corpName");
            if (name != null) corpName = name.toString();
        }

        // 3. 재무 수치 검색
        List<Document> financials = searchByFilter(
                stockCode, "source == 'dart-financial' && stockCode == '" + stockCode + "'", 3);

        // 4. 관련 뉴스 검색 (회사명으로 유사도 검색)
        String newsQuery = corpName != null ? corpName : stockCode;
        List<Document> news = searchByFilter(
                newsQuery, "source == 'naver-news'", 10);

        // 5. 과거 유사 데이터 검색
        List<Document> history = searchSimilar(newsQuery + " 실적 공시 분석", 5);

        // 6. 프롬프트 조립
        String userPrompt = buildUserPrompt(stockCode, corpName, disclosures, financials, news, history);

        // 7. LLM 호출
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        ));

        String responseText = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        log.info("게시글 생성 완료 - 종목코드: {} (공시 {}건, 재무 {}건, 뉴스 {}건)",
                stockCode, disclosures.size(), financials.size(), news.size());

        return parseResponse(responseText);
    }

    public Long publish(GeneratedPost post) {
        Long postId = postWriter.write(post.title(), post.content(), AI_USER_ID, AI_NICKNAME);
        log.info("게시글 Board에 등록 완료 - postId: {}", postId);
        return postId;
    }

    private List<Document> searchByFilter(String query, String filterExpression, int topK) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .filterExpression(filterExpression)
                            .build()
            );
        } catch (Exception e) {
            log.warn("벡터 검색 실패 (filter={}): {}", filterExpression, e.getMessage());
            return List.of();
        }
    }

    private List<Document> searchSimilar(String query, int topK) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );
        } catch (Exception e) {
            log.warn("유사도 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(String stockCode, String corpName,
                                   List<Document> disclosures,
                                   List<Document> financials,
                                   List<Document> news,
                                   List<Document> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 분석 대상\n");
        sb.append("- 종목코드: ").append(stockCode).append("\n");
        if (corpName != null) {
            sb.append("- 회사명: ").append(corpName).append("\n");
        }
        sb.append("\n");

        if (!disclosures.isEmpty()) {
            sb.append("## 공시 정보\n");
            for (Document doc : disclosures) {
                sb.append(doc.getText()).append("\n\n");
            }
        }

        if (!financials.isEmpty()) {
            sb.append("## 재무 수치\n");
            for (Document doc : financials) {
                sb.append(doc.getText()).append("\n\n");
            }
        }

        if (!news.isEmpty()) {
            sb.append("## 관련 뉴스\n");
            for (Document doc : news) {
                sb.append(doc.getText()).append("\n\n");
            }
        }

        if (!history.isEmpty()) {
            sb.append("## 과거 연관 데이터\n");
            for (Document doc : history) {
                sb.append(doc.getText()).append("\n\n");
            }
        }

        sb.append("위 데이터를 종합 분석하여 시장 분석 글을 작성해주세요.");
        return sb.toString();
    }

    private GeneratedPost parseResponse(String responseText) {
        try {
            String json = extractJson(responseText);
            return objectMapper.readValue(json, GeneratedPost.class);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패, 기본 형식으로 변환", e);
            return new GeneratedPost(
                    "시장 분석",
                    responseText.length() > 100 ? responseText.substring(0, 100) + "..." : responseText,
                    responseText,
                    List.of()
            );
        }
    }

    private String extractJson(String text) {
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
