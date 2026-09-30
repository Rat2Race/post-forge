package dev.iamrat.ai.support.application;

public interface TextGenerationClient {
    String generate(String systemPrompt, String userPrompt);

    String generateForPublishing(String systemPrompt, String userPrompt);
}
