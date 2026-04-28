package dev.iamrat.ai.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.prompt.PromptTemplateLoader;
import dev.iamrat.board.post.PostCategory;
import dev.iamrat.board.post.PostWriter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PostGenerationService.class);

    private static final String AI_USER_ID = "ai-post-generator";
    private static final String AI_NICKNAME = "AI 분석가";
    private static final String SOURCE_NAVER_NEWS = "naver-news";
    private static final String NEWS_ANALYSIS_SYSTEM_PROMPT_PATH = "prompts/news-analysis-system.md";
    private static final String NEWS_ANALYSIS_USER_PROMPT_PATH = "prompts/news-analysis-user.md";

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final PostWriter postWriter;
    private final OutputGuardrail outputGuardrail;
    private final PromptTemplateLoader promptTemplateLoader;

    public GeneratedPost generateNewsAnalysis(String keyword, String articleTitle, String articleContent, String originalLink) {
        log.info("뉴스 분석 게시글 생성 시작 - keyword={}, link={}", keyword, originalLink);

        List<Document> relatedNews = searchBySource(SOURCE_NAVER_NEWS, keyword, 5);
        List<Document> history = searchSimilar(keyword + " 트렌드 분석", 3);

        String userPrompt = buildNewsUserPrompt(keyword, articleTitle, articleContent, originalLink, relatedNews, history);
        Prompt prompt = new Prompt(List.of(
            new SystemMessage(promptTemplateLoader.load(NEWS_ANALYSIS_SYSTEM_PROMPT_PATH)),
            new UserMessage(userPrompt)
        ));

        String responseText = chatModel.call(prompt)
            .getResult()
            .getOutput()
            .getText();

        log.info("뉴스 분석 게시글 생성 완료 - keyword={} (관련 뉴스 {}건, 과거 데이터 {}건)",
            keyword, relatedNews.size(), history.size());

        return outputGuardrail.sanitize(parseResponse(responseText));
    }

    public Long publish(GeneratedPost post) {
        Long postId = postWriter.write(
            post.title(),
            post.content(),
            post.summary(),
            post.tags(),
            AI_USER_ID,
            AI_NICKNAME,
            PostCategory.AI_ANALYSIS
        );
        log.info("게시글 Board에 등록 완료 - postId: {}", postId);
        return postId;
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

    private String buildNewsUserPrompt(
        String keyword,
        String articleTitle,
        String articleContent,
        String originalLink,
        List<Document> relatedNews,
        List<Document> history
    ) {
        return promptTemplateLoader.render(
            NEWS_ANALYSIS_USER_PROMPT_PATH,
            Map.of(
                "keyword", safe(keyword),
                "articleTitle", safe(articleTitle),
                "originalLink", safe(originalLink),
                "articleContent", safe(articleContent),
                "relatedNewsSection", renderSection("## 관련 뉴스 묶음", relatedNews),
                "historySection", renderSection("## 과거 유사 뉴스/분석", history)
            )
        );
    }

    private String renderSection(String header, List<Document> docs) {
        if (!docs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append("\n");
            for (Document doc : docs) {
                sb.append(doc.getText()).append("\n\n");
            }
            return sb.toString();
        }
        return "";
    }

    private GeneratedPost parseResponse(String responseText) {
        try {
            String json = extractJson(responseText);
            return objectMapper.readValue(json, GeneratedPost.class);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패, 기본 형식으로 변환: {}", e.getMessage());
            return new GeneratedPost(
                "트렌드 분석",
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
