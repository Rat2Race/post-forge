package dev.iamrat.ai.support.application;

import static dev.iamrat.ai.support.application.AiSafetyKeywords.BROAD_REVEAL_ACTIONS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.CONCRETE_SECRET_TARGETS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.DIRECT_REVEAL_ACTIONS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.EDUCATIONAL_CONTEXT;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.GENERIC_SECRET_TARGETS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.INTERNAL_PROMPT_TARGETS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.OPERATIONAL_INFO_TARGETS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.OVERRIDE_ACTIONS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.OVERRIDE_TARGETS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.PROMPT_DUMP_MARKERS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.VALUE_DETAIL_ACTIONS;
import static dev.iamrat.ai.support.application.AiSafetyKeywords.WHAT_IS_ACTIONS;

import dev.iamrat.ai.support.prompt.PromptResourceLoader;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiSafetyGuard {

    private static final String REFUSAL_PROMPT_PATH = "prompts/refusal-security.md";
    private static final Pattern OPENAI_KEY_PATTERN = Pattern.compile("(?i)\\bsk-(?:proj-)?[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
        "(?iu)\\b(?:openai[_-]?api[_-]?key|api[_-]?key|secret|password|token|jwt|database[_-]?url|db[_-]?url|jdbc[_-]?url)\\b\\s*[:=]\\s*['\"]?[^\\s'\"`]{8,}"
    );
    private static final Pattern INTERNAL_PROMPT_HEADER_PATTERN = Pattern.compile(
        "(?iu)(시스템\\s*프롬프트|내부\\s*프롬프트|숨겨진\\s*지시|system\\s*prompt|developer\\s*message|hidden\\s*instruction)\\s*[:：]"
    );

    private final PromptResourceLoader promptResourceLoader;

    public AiSafetyGuard(PromptResourceLoader promptResourceLoader) {
        this.promptResourceLoader = promptResourceLoader;
    }

    public boolean shouldRefuse(String... inputs) {
        String text = normalize(inputs);

        if (text.isBlank()) {
            return false;
        }

        if (containsSecretMaterial(text)) {
            return true;
        }

        if (containsAny(text, INTERNAL_PROMPT_TARGETS)
            && (containsAny(text, DIRECT_REVEAL_ACTIONS)
                || containsAny(text, OVERRIDE_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS))) {
            return true;
        }

        if (containsAny(text, OVERRIDE_TARGETS) && containsAny(text, OVERRIDE_ACTIONS)) {
            return true;
        }

        if (containsAny(text, CONCRETE_SECRET_TARGETS)) {
            return containsAny(text, BROAD_REVEAL_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS)
                || containsAny(text, WHAT_IS_ACTIONS)
                || hasQuestionMark(text);
        }

        if (containsAny(text, OPERATIONAL_INFO_TARGETS)) {
            boolean asksForOperationalDisclosure = containsAny(text, DIRECT_REVEAL_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS)
                || (containsAny(text, BROAD_REVEAL_ACTIONS) && !containsAny(text, EDUCATIONAL_CONTEXT));
            boolean bareOperationalQuestion = hasQuestionMark(text)
                && !containsAny(text, EDUCATIONAL_CONTEXT)
                && !containsAny(text, WHAT_IS_ACTIONS);
            return asksForOperationalDisclosure || bareOperationalQuestion;
        }

        if (containsAny(text, GENERIC_SECRET_TARGETS) && containsAny(text, DIRECT_REVEAL_ACTIONS)) {
            return true;
        }
        return containsAny(text, GENERIC_SECRET_TARGETS)
            && containsAny(text, BROAD_REVEAL_ACTIONS)
            && !containsAny(text, EDUCATIONAL_CONTEXT);
    }

    public String refusalMessage() {
        return promptResourceLoader.load(REFUSAL_PROMPT_PATH);
    }

    public String sanitizeOutput(String output) {
        if (output == null) {
            return null;
        }
        if (OPENAI_KEY_PATTERN.matcher(output).find()
            || SECRET_ASSIGNMENT_PATTERN.matcher(output).find()
            || INTERNAL_PROMPT_HEADER_PATTERN.matcher(output).find()
            || containsPromptDump(output)) {
            return refusalMessage();
        }
        return output;
    }

    private String normalize(String... inputs) {
        if (inputs == null || inputs.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String input : inputs) {
            if (input != null && !input.isBlank()) {
                builder.append(' ').append(input);
            }
        }
        return builder.toString()
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private boolean containsSecretMaterial(String text) {
        return OPENAI_KEY_PATTERN.matcher(text).find()
            || SECRET_ASSIGNMENT_PATTERN.matcher(text).find();
    }

    private boolean containsPromptDump(String output) {
        return containsAny(normalize(output), PROMPT_DUMP_MARKERS);
    }

    private boolean containsAny(String text, List<String> candidates) {
        return candidates.stream().anyMatch(text::contains);
    }

    private boolean hasQuestionMark(String text) {
        return text.contains("?") || text.contains("？");
    }
}
