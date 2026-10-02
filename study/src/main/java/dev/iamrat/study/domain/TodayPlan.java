package dev.iamrat.study.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 오늘 할 것에 빈 페이지와 문제를 섞는 규칙.
 * - 빈 페이지는 하루에 recallLimit개까지, 예정이 이른 자료부터 넣는다.
 * - 빈 페이지는 같은 자료의 문제보다 먼저 둔다. 문제를 먼저 풀면 단서가 생겨 자유 회상이 오염된다.
 * - 묶음(빈 페이지 + 그 자료의 문제, 또는 문제 하나)은 가장 이른 예정 시각 순으로 놓고, 전체를 limit개로 자른다.
 */
public final class TodayPlan {

    public enum Kind { QUESTION, RECALL }

    /** 빈 페이지면 id와 sourceId가 모두 자료 id다. */
    public record Due(Kind kind, long id, long sourceId, LocalDateTime dueAt) {
    }

    public record Plan(List<Due> items, int remaining) {
    }

    private static final Comparator<Due> BY_TIME = Comparator.comparing(Due::dueAt).thenComparing(Due::id);

    private TodayPlan() {
    }

    public static Plan of(List<Due> questions, List<Due> recalls, int limit, int recallLimit) {
        List<Due> chosenRecalls = recalls.stream().sorted(BY_TIME).limit(recallLimit).toList();
        Set<Long> recallSources = chosenRecalls.stream().map(Due::sourceId).collect(Collectors.toSet());

        List<List<Due>> blocks = new ArrayList<>();
        for (Due recall : chosenRecalls) {
            List<Due> block = new ArrayList<>();
            block.add(recall);
            questions.stream().filter(q -> q.sourceId() == recall.sourceId()).sorted(BY_TIME).forEach(block::add);
            blocks.add(block);
        }
        questions.stream()
            .filter(q -> !recallSources.contains(q.sourceId()))
            .forEach(q -> blocks.add(List.of(q)));
        blocks.sort(Comparator.comparing((List<Due> block) -> block.stream().map(Due::dueAt).min(Comparator.naturalOrder()).orElseThrow())
            .thenComparing(block -> block.get(0).id()));

        List<Due> items = blocks.stream().flatMap(List::stream).limit(limit).toList();
        return new Plan(items, questions.size() + recalls.size() - items.size());
    }
}
