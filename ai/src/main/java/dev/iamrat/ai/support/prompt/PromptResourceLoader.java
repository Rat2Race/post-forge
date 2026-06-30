package dev.iamrat.ai.support.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class PromptResourceLoader {

    private static final String PROMPT_RESOURCE_NOT_FOUND = "프롬프트 리소스를 찾을 수 없습니다: ";
    private static final String FAILED_TO_LOAD_PROMPT_RESOURCE =
        "프롬프트 리소스를 불러오지 못했습니다: ";

    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    public String load(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, this::readResource);
    }

    public String render(String resourcePath, Map<String, String> values) {
        String rendered = load(resourcePath);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered.trim();
    }

    private String readResource(String resourcePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
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
