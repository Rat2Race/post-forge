package dev.iamrat.ai.support.application;

public interface TextGenerationClient {
    String generate(String systemPrompt, String userPrompt);
}
