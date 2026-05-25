package dev.iamrat.ai.support.infrastructure.openai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiPropertiesTest {

    @Test
    @DisplayName("spring.ai.openai prefix로 OpenAI 설정을 바인딩한다")
    void openAiProperties_usesOpenAiPrefix() {
        ConfigurationProperties annotation =
            OpenAiProperties.class.getAnnotation(ConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("spring.ai.openai");
    }

    @Test
    @DisplayName("OpenAI 공용 설정에서 OpenAI properties 바인딩을 활성화한다")
    void openAiConfig_enablesOpenAiProperties() {
        EnableConfigurationProperties annotation =
            OpenAiConfig.class.getAnnotation(EnableConfigurationProperties.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(OpenAiProperties.class);
    }
}
