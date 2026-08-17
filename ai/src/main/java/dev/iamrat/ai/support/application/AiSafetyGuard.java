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

/*
 * AI 텍스트 생성 전후에 적용되는 deterministic safety guard입니다.
 *
 * 이 클래스는 모델 자체의 판단에 의존하지 않고, 애플리케이션이 정한 키워드/패턴 기반 규칙으로
 * 민감한 요청과 응답을 먼저 걸러냅니다. 사용자가 API key, secret, 내부 프롬프트, 운영 정보 등을
 * 직접 노출시키려는 요청을 하면 LLM 호출 전에 거절하도록 판단합니다.
 *
 * 또한 모델 응답에 secret 형태의 문자열이나 내부 프롬프트 dump로 보이는 내용이 포함되면,
 * 원문을 그대로 사용자에게 돌려주지 않고 보안 거절 메시지로 치환합니다.
 * 즉, shouldRefuse(...)는 입력 방어, sanitizeOutput(...)은 출력 방어 역할을 합니다.
 */
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

    /*
     * 사용자 입력 또는 외부 소스에서 온 텍스트가 LLM 호출 전에 거절되어야 하는지 판단합니다.
     *
     * 여러 입력 값을 한 번에 받을 수 있도록 varargs를 사용합니다. 예를 들어 출시 뉴스 생성에서는
     * 제목, 요약, 본문 후보처럼 서로 다른 외부 입력을 함께 검사할 수 있습니다.
     * 하나라도 민감 정보 노출, 내부 프롬프트 추출, 보안 우회 시도에 해당한다고 판단되면 true를 반환합니다.
     */
    public boolean shouldRefuse(String... inputs) {
        String text = normalize(inputs);

        // 검사할 텍스트가 없으면 안전 검사를 통과시킵니다.
        if (text.isBlank()) {
            return false;
        }

        // 입력에 API key나 token처럼 보이는 실제 secret 값이 직접 포함됐는지 검사합니다.
        if (containsSecretMaterial(text)) {
            return true;
        }

        // 내부 프롬프트나 숨겨진 지시를 보여달라거나 우회하려는 요청인지 검사합니다.
        if (containsAny(text, INTERNAL_PROMPT_TARGETS)
            && (containsAny(text, DIRECT_REVEAL_ACTIONS)
                || containsAny(text, OVERRIDE_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS))) {
            return true;
        }

        // 이전 지시, system prompt 같은 지시 대상을 무시하거나 우회하려는 요청인지 검사합니다.
        if (containsAny(text, OVERRIDE_TARGETS) && containsAny(text, OVERRIDE_ACTIONS)) {
            return true;
        }

        // .env, OpenAI key, private key처럼 구체적인 secret 대상을 묻는 요청인지 검사합니다.
        if (containsAny(text, CONCRETE_SECRET_TARGETS)) {
            return containsAny(text, BROAD_REVEAL_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS)
                || containsAny(text, WHAT_IS_ACTIONS)
                || hasQuestionMark(text);
        }

        // 서버 IP, 환경변수, 내부 네트워크 같은 운영 정보를 노출하려는 요청인지 검사합니다.
        if (containsAny(text, OPERATIONAL_INFO_TARGETS)) {
            boolean asksForOperationalDisclosure = containsAny(text, DIRECT_REVEAL_ACTIONS)
                || containsAny(text, VALUE_DETAIL_ACTIONS)
                || (containsAny(text, BROAD_REVEAL_ACTIONS) && !containsAny(text, EDUCATIONAL_CONTEXT));
            boolean bareOperationalQuestion = hasQuestionMark(text)
                && !containsAny(text, EDUCATIONAL_CONTEXT)
                && !containsAny(text, WHAT_IS_ACTIONS);
            return asksForOperationalDisclosure || bareOperationalQuestion;
        }

        // 일반 secret/token/password 대상을 직접 보여달라는 요청인지 검사합니다.
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

    /*
     * LLM 응답을 사용자에게 전달하기 전에 민감 정보가 포함되어 있는지 검사합니다.
     *
     * 응답 안에 OpenAI API key 형태, secret assignment 형태, 내부 프롬프트 헤더,
     * prompt dump marker가 발견되면 원 응답 대신 보안 거절 메시지를 반환합니다.
     * 문제가 없는 일반 생성 결과는 그대로 반환하고, null 응답은 fallback 흐름을 보존하기 위해 그대로 둡니다.
     */
    public String sanitizeOutput(String output) {
        // 모델 응답이 없으면 후속 fallback 흐름이 판단하도록 그대로 반환합니다.
        if (output == null) {
            return null;
        }
        // 응답에 secret 값이나 내부 프롬프트 dump 흔적이 포함됐는지 검사합니다.
        if (OPENAI_KEY_PATTERN.matcher(output).find()
            || SECRET_ASSIGNMENT_PATTERN.matcher(output).find()
            || INTERNAL_PROMPT_HEADER_PATTERN.matcher(output).find()
            || containsPromptDump(output)) {
            return refusalMessage();
        }
        return output;
    }

    /*
     * 여러 입력 값을 하나의 소문자 문자열로 합쳐 키워드 탐지에 적합한 형태로 정규화합니다.
     */
    private String normalize(String... inputs) {
        // 입력 배열 자체가 없거나 비어 있으면 검사할 문자열이 없다고 봅니다.
        if (inputs == null || inputs.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String input : inputs) {
            // null이거나 공백뿐인 입력은 정규화 대상에서 제외합니다.
            if (input != null && !input.isBlank()) {
                builder.append(' ').append(input);
            }
        }
        return builder.toString()
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    /*
     * 입력 자체에 API key, token, password, JDBC URL처럼 secret으로 보이는 값이 직접 포함되어 있는지 검사합니다.
     */
    private boolean containsSecretMaterial(String text) {
        return OPENAI_KEY_PATTERN.matcher(text).find()
            || SECRET_ASSIGNMENT_PATTERN.matcher(text).find();
    }

    /*
     * 모델 응답이 시스템/개발자 지시문 덤프처럼 보이는 marker를 포함하는지 검사합니다.
     */
    private boolean containsPromptDump(String output) {
        return containsAny(normalize(output), PROMPT_DUMP_MARKERS);
    }

    /*
     * 정규화된 텍스트에 후보 키워드 중 하나라도 포함되어 있는지 확인합니다.
     */
    private boolean containsAny(String text, List<String> candidates) {
        return candidates.stream().anyMatch(text::contains);
    }

    /*
     * 질문형 문장을 넓게 감지하기 위해 ASCII 물음표와 전각 물음표를 모두 확인합니다.
     */
    private boolean hasQuestionMark(String text) {
        return text.contains("?") || text.contains("？");
    }
}
