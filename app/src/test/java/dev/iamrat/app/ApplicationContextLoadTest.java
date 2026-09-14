package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.search.infrastructure.vector.PgVectorProperties;
import dev.iamrat.ai.support.infrastructure.llm.LlmProperties;
import dev.iamrat.core.board.post.LaunchNewsPostDraftGenerator;
import dev.iamrat.ingest.news.application.DailyDigestScheduler;
import dev.iamrat.ingest.news.application.LaunchNewsPublishScheduler;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.config.import=optional:classpath:application-monitoring.yml")
@ActiveProfiles("test")
class ApplicationContextLoadTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("외부 호출 없이 게시 파이프라인을 조립하고 스케줄러는 실행하지 않는다")
    void contextLoads() {
        assertThat(context.getBean(PublishLaunchNewsUseCase.class)).isNotNull();
        assertThat(context.getBean(LaunchNewsPostDraftGenerator.class)).isNotNull();
        assertThat(context.getBeansOfType(LaunchNewsPublishScheduler.class)).isEmpty();
        assertThat(context.getBeansOfType(DailyDigestScheduler.class)).isEmpty();
    }

    @Test
    @DisplayName("임베딩 요청 차원과 vector_store 차원이 같은 값으로 묶여 provider를 바꿔도 어긋나지 않는다")
    void embeddingDimensionsAreBoundToVectorStoreDimensions() {
        Integer requestDimensions = context.getBean(LlmProperties.class)
            .getEmbedding().getOptions().getDimensions();

        assertThat(requestDimensions)
            .as("app.llm.embedding.options.dimensions 바인딩이 빠지면 상용 provider가 다른 차원을 돌려준다")
            .isEqualTo(context.getBean(PgVectorProperties.class).getDimensions());
    }
}
