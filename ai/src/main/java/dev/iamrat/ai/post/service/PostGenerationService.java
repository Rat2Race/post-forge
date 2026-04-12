package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.board.post.PostWriter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PostGenerationService.class);

    private static final String AI_USER_ID = "ai-post-generator";
    private static final String AI_NICKNAME = "AI 분석가";

    private static final String SOURCE_DART = "dart";
    private static final String SOURCE_DART_FINANCIAL = "dart-financial";
    private static final String SOURCE_KRX_PRICE = "krx-price";
    private static final String SOURCE_NAVER_NEWS = "naver-news";

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final PostWriter postWriter;
    private final OutputGuardrail outputGuardrail;

    public PostGenerationService(
            ChatModel chatModel,
            VectorStore vectorStore,
            ObjectMapper objectMapper,
            PostWriter postWriter,
            OutputGuardrail outputGuardrail
    ) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.postWriter = postWriter;
        this.outputGuardrail = outputGuardrail;
    }

    private static final String SYSTEM_PROMPT = """
            당신은 주식 시장 분석가입니다.

            [분석 방법]
            1. 재무 수치를 확인하고 전년 동기 대비 증감률을 해석
            2. 공시 유형(정기공시/주요사항/지분변동)에 따라 의미를 분석
            3. 가격/거래량 데이터에서 시장 반응 강도를 해석
            4. 관련 뉴스에서 시장 센티먼트와 수급 동향을 파악
            5. 과거 유사 시점의 데이터와 비교하여 패턴 분석
            6. 이를 종합하여 단기/중장기 방향성을 객관적으로 서술

            [규칙]
            - 투자 권유 금지, 정보 전달 목적만
            - 재무 수치는 반드시 출처(DART)와 함께 정확히 인용
            - 증감률은 직접 계산된 값을 사용
            - 뉴스는 시장 분위기 참고용, 팩트와 구분하여 서술
            - 가격/거래량 변화가 의미하는 시장 반응을 반드시 설명
            - 확실하지 않은 해석은 "~로 보인다", "~가능성이 있다"로 표현
            - 출처(DART 접수번호, 뉴스 링크)를 반드시 포함
            - 반드시 단기 방향성, 중장기 방향성, 상충 신호, 핵심 주의점을 포함

            [응답 형식]
            반드시 아래 JSON 형식으로만 응답하세요. JSON 외의 텍스트는 포함하지 마세요:
            {
              "title": "게시글 제목",
              "summary": "1-2줄 요약",
              "content": "마크다운 형식의 본문 (## 핵심 요약, ## 공시 분석, ## 시장 반응, ## 뉴스 동향, ## 방향성 판단, ## 상충 신호, ## 주의점, ## 출처 섹션 포함)",
              "tags": ["태그1", "태그2", "태그3"]
            }
            """;

    public GeneratedPost generate(String stockCode, String corpName) {
        log.info("게시글 생성 시작 - 종목코드: {}", stockCode);

        List<Document> disclosures = searchBySourceAndStock(SOURCE_DART, stockCode, 5);

        if (corpName == null && !disclosures.isEmpty()) {
            Object name = disclosures.getFirst().getMetadata().get("corpName");
            if (name != null) corpName = name.toString();
        }

        List<Document> financials = searchBySourceAndStock(SOURCE_DART_FINANCIAL, stockCode, 3);
        List<Document> prices = searchBySourceAndStock(SOURCE_KRX_PRICE, stockCode, 3);

        String newsQuery = corpName != null ? corpName : stockCode;
        List<Document> news = searchBySource(SOURCE_NAVER_NEWS, newsQuery, 10);

        List<Document> history = searchSimilar(newsQuery + " 실적 공시 분석", 5);

        String userPrompt = buildUserPrompt(stockCode, corpName, disclosures, financials, prices, news, history);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        ));

        String responseText = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        log.info("게시글 생성 완료 - 종목코드: {} (공시 {}건, 재무 {}건, 가격 {}건, 뉴스 {}건)",
                stockCode, disclosures.size(), financials.size(), prices.size(), news.size());

        return outputGuardrail.sanitize(parseResponse(responseText));
    }

    public Long publish(GeneratedPost post) {
        Long postId = postWriter.write(
                post.title(), post.content(), post.summary(), post.tags(),
                AI_USER_ID, AI_NICKNAME
        );
        log.info("게시글 Board에 등록 완료 - postId: {}", postId);
        return postId;
    }

    private List<Document> searchBySourceAndStock(String source, String stockCode, int topK) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            var filter = b.and(
                    b.eq("source", source),
                    b.eq("stockCode", stockCode)
            ).build();

            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(stockCode)
                            .topK(topK)
                            .filterExpression(filter)
                            .build()
            );
        } catch (Exception e) {
            log.warn("벡터 검색 실패 (source={}, stockCode={}): {}", source, stockCode, e.getMessage());
            return List.of();
        }
    }

    private List<Document> searchBySource(String source, String query, int topK) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            var filter = b.eq("source", source).build();

            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .filterExpression(filter)
                            .build()
            );
        } catch (Exception e) {
            log.warn("벡터 검색 실패 (source={}): {}", source, e.getMessage());
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
                                   List<Document> prices,
                                   List<Document> news,
                                   List<Document> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 분석 대상\n");
        sb.append("- 종목코드: ").append(stockCode).append("\n");
        if (corpName != null) {
            sb.append("- 회사명: ").append(corpName).append("\n");
        }
        sb.append("\n");

        appendSection(sb, "## 공시 정보", disclosures);
        appendSection(sb, "## 재무 수치", financials);
        appendSection(sb, "## 가격/거래량 데이터", prices);
        appendSection(sb, "## 관련 뉴스", news);
        appendSection(sb, "## 과거 연관 데이터", history);

        sb.append("위 데이터를 종합 분석하여 단기(1~3일)와 중장기(1~3개월) 방향성 판단 글을 작성해주세요.");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String header, List<Document> docs) {
        if (!docs.isEmpty()) {
            sb.append(header).append("\n");
            for (Document doc : docs) {
                sb.append(doc.getText()).append("\n\n");
            }
        }
    }

    private GeneratedPost parseResponse(String responseText) {
        try {
            String json = extractJson(responseText);
            return objectMapper.readValue(json, GeneratedPost.class);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패, 기본 형식으로 변환: {}", e.getMessage());
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

        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return text.substring(jsonStart, jsonEnd + 1);
        }

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
