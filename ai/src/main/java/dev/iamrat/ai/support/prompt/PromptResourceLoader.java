package dev.iamrat.ai.support.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PromptResourceLoader {

    private static final String PROMPT_RESOURCE_NOT_FOUND = "프롬프트 리소스를 찾을 수 없습니다: ";
    private static final String FAILED_TO_LOAD_PROMPT_RESOURCE = "프롬프트 리소스를 불러오지 못했습니다: ";
    private static final List<String> PROMPT_RESOURCE_PATHS = List.of(
        "prompts/chat-system.md",
        "prompts/launch-news-draft-system.md",
        "prompts/news-analysis-system.md",
        "prompts/news-analysis-user.md",
        "prompts/refusal-security.md",
        "prompts/safety-policy.md"
    );

    private final Map<String, String> prompts;

    public PromptResourceLoader() {
        Map<String, String> loaded = new HashMap<>();
        for (String resourcePath : PROMPT_RESOURCE_PATHS) {
            loaded.put(resourcePath, readResource(resourcePath));
        }
        prompts = Map.copyOf(loaded);
    }

    public String load(String resourcePath) {
        String prompt = prompts.get(resourcePath);
        if (prompt == null) {
            throw new IllegalStateException(promptResourceNotFound(resourcePath));
        }
        return prompt;
    }

    public String render(String resourcePath, Map<String, String> values) {
        String rendered = load(resourcePath);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered.trim();
    }

    private static String readResource(String resourcePath) {
        ClassLoader classLoader = PromptResourceLoader.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException(promptResourceNotFound(resourcePath));
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException(failedToLoadPromptResource(resourcePath), e);
        }
    }

    private static String promptResourceNotFound(String resourcePath) {
        return PROMPT_RESOURCE_NOT_FOUND + resourcePath;
    }

    private static String failedToLoadPromptResource(String resourcePath) {
        return FAILED_TO_LOAD_PROMPT_RESOURCE + resourcePath;
    }
}
