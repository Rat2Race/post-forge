package dev.iamrat.ai.chat.application;

import dev.iamrat.ai.search.domain.SearchResult;
import java.util.List;
import java.util.stream.Collectors;

final class TestChatPromptTemplate implements ChatPromptTemplate {

    @Override
    public String chatSystemPrompt(List<SearchResult> context) {
        String contextSuffix = context.isEmpty()
            ? "\n\n관련 참고 문서 없음:\n검색은 정상 처리되었지만 질문과 직접 관련된 저장 문서를 찾지 못했습니다."
            : "\n\n참고할 컨텍스트:\n" + context.stream()
                .map(SearchResult::text)
                .collect(Collectors.joining("\n\n"));
        return "chat-system" + contextSuffix;
    }
}
