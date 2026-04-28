package dev.iamrat.ai.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateLoader {

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
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Prompt resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt resource: " + resourcePath, e);
        }
    }
}
