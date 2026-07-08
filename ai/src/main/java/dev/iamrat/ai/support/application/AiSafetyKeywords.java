package dev.iamrat.ai.support.application;

import java.util.List;
import java.util.stream.Stream;

final class AiSafetyKeywords {

    static final List<String> DIRECT_REVEAL_ACTIONS = List.of(
        "보여줘", "출력", "공개", "노출", "덤프", "추출", "유출",
        "show", "print", "reveal", "dump", "extract", "leak", "exfiltrate"
    );
    static final List<String> BROAD_REVEAL_ACTIONS = combined(
        DIRECT_REVEAL_ACTIONS,
        List.of("알려", "tell me")
    );
    static final List<String> OVERRIDE_ACTIONS = List.of(
        "무시", "우회", "덮어써", "해제", "ignore previous", "ignore all",
        "bypass", "override", "disregard", "jailbreak"
    );
    static final List<String> VALUE_DETAIL_ACTIONS = List.of(
        "값", "내용", "어디", "value", "contents", "content", "where is"
    );
    static final List<String> WHAT_IS_ACTIONS = List.of(
        "뭐야", "무엇", "what is"
    );
    static final List<String> CONCRETE_SECRET_TARGETS = List.of(
        ".env", "openai_api_key", "aws_secret_access_key", "aws_access_key_id",
        "sk-proj", "sk-", "private key"
    );
    static final List<String> OPERATIONAL_INFO_TARGETS = List.of(
        "환경변수", "env var", "environment variable",
        "서버 ip", "서버 정보", "server ip", "server info", "server details", "hostname",
        "내부 네트워크", "internal network"
    );
    static final List<String> GENERIC_SECRET_TARGETS = List.of(
        "api key", "apikey", "secret", "시크릿", "token", "토큰", "password", "비밀번호",
        "jwt", "refresh token", "access token", "database url", "db url", "jdbc"
    );
    static final List<String> INTERNAL_PROMPT_TARGETS = List.of(
        "system prompt", "developer message", "hidden instruction", "internal prompt",
        "시스템 프롬프트", "내부 프롬프트", "숨겨진 지시", "개발자 메시지", "내부 정책",
        "safety policy"
    );
    static final List<String> OVERRIDE_TARGETS = List.of(
        "instruction", "instructions", "지시", "이전 지시", "previous instruction",
        "system prompt", "developer message", "시스템 프롬프트", "개발자 메시지"
    );
    static final List<String> EDUCATIONAL_CONTEXT = List.of(
        "보안", "안전", "관리", "모범", "설명", "개념", "방법", "가이드", "무엇인가", "란",
        "best practice", "what is", "how to"
    );
    static final List<String> PROMPT_DUMP_MARKERS = List.of(
        "공통 보안 정책:",
        "서버 정보, 환경변수, api key, token, password, private key",
        "당신은 postforge 커뮤니티의 ai 어시스턴트입니다.",
        "저장된 컨텍스트로 확인할 수 없는 구체적인 사실",
        "당신은 postforge 게시판 초안 작성 도우미입니다.",
        "제목, 태그, json, 설명 문구를 따로 출력하지 말고"
    );

    private AiSafetyKeywords() {
    }

    private static List<String> combined(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
