package dev.iamrat.ai.generation.application;

import dev.iamrat.ai.generation.domain.GeneratedPost;
import dev.iamrat.ai.search.application.SemanticSearchService;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.ai.support.application.TextGenerationClient;
import dev.iamrat.core.ai.post.NewsAnalysisPostPublisher;
import dev.iamrat.core.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostWriteCommand;
import dev.iamrat.core.board.post.PostWriter;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostGenerationService implements NewsAnalysisPostPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostGenerationService.class);

    private static final String AI_NICKNAME = "AI 분석가";

    private final PostWriter postWriter;
    private final OutputGuardrail outputGuardrail;
    private final GeneratedPostParser generatedPostParser;
    private final TextGenerationClient textGenerationClient;
    private final AiPromptTemplate aiPromptTemplate;
    private final SemanticSearchService semanticSearchService;

    @Override
    public Long publishNewsAnalysis(NewsAnalysisPostRequest request) {
        GeneratedPost post = generateNewsAnalysis(
            request.keyword(),
            request.articleTitle(),
            request.articleContent(),
            request.originalLink()
        );
        return publish(post);
    }

    public GeneratedPost generateNewsAnalysis(String keyword, String articleTitle, String articleContent, String originalLink) {
        log.info("뉴스 분석 게시글 생성 시작 - keyword={}, link={}", keyword, originalLink);

        List<SearchResult> relatedNews = searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, keyword, 5);
        List<SearchResult> history = searchSimilar(keyword + " 트렌드 분석", 3);

        String responseText = textGenerationClient.generate(
            aiPromptTemplate.newsAnalysisSystemPrompt(),
            aiPromptTemplate.newsAnalysisUserPrompt(
                keyword,
                articleTitle,
                articleContent,
                originalLink,
                relatedNews,
                history
            )
        );

        log.info("뉴스 분석 게시글 생성 완료 - keyword={} (관련 뉴스 {}건, 과거 데이터 {}건)",
            keyword, relatedNews.size(), history.size());

        return outputGuardrail.sanitize(generatedPostParser.parse(responseText));
    }

    public Long publish(GeneratedPost post) {
        Long postId = postWriter.write(new PostWriteCommand(
            post.title(),
            post.content(),
            post.summary(),
            post.tags(),
            null,
            AI_NICKNAME,
            PostCategory.AI_ANALYSIS
        ));
        log.info("게시글 Board에 등록 완료 - postId: {}", postId);
        return postId;
    }

    private List<SearchResult> searchBySource(String source, String query, int topK) {
        try {
            return semanticSearchService.searchBySource(source, query, topK);
        } catch (Exception e) {
            log.warn("벡터 검색 실패 (source={}): {}", source, e.getMessage());
            return List.of();
        }
    }

    private List<SearchResult> searchSimilar(String query, int topK) {
        try {
            return semanticSearchService.searchSimilar(query, topK);
        } catch (Exception e) {
            log.warn("유사도 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

}
