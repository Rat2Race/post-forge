package dev.iamrat.ai.support.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateLoader {

    private static final String PROMPT_RESOURCE_NOT_FOUND = "프롬프트 리소스를 찾을 수 없습니다: ";
    private static final String FAILED_TO_LOAD_PROMPT_RESOURCE = "프롬프트 리소스를 불러오지 못했습니다: ";

    // 한 번 읽은 프롬프트 파일 내용을 재사용하기 위한 캐시입니다.
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    // classpath의 프롬프트 리소스를 읽고, 이미 읽은 파일이면 캐시된 값을 반환합니다.
    public String load(String resourcePath) {
        return cache.computeIfAbsent(resourcePath, this::readResource);
    }

    // 프롬프트 템플릿의 {{key}} 자리표시자를 전달받은 값으로 치환합니다.
    public String render(String resourcePath, Map<String, String> values) {
        String rendered = load(resourcePath);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered.trim();
    }

    // classpath에서 실제 프롬프트 파일을 찾아 UTF-8 문자열로 읽습니다.
    private String readResource(String resourcePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
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
