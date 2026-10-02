package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.core.study.StudyAssistant;
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
    @DisplayName("외부 호출 없이 학습 LLM 경로를 조립한다")
    void contextLoads() {
        assertThat(context.getBean(StudyAssistant.class)).isNotNull();
    }
}
