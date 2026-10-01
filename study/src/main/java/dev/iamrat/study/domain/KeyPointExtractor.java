package dev.iamrat.study.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 빈 페이지 정리 뒤 대조할 핵심 항목을 자료에서 뽑는다.
 * 마크다운 제목·목록이 있으면 그것을, 없으면 문단마다 첫 문장을 쓴다.
 */
public final class KeyPointExtractor {

    private static final int LIMIT = 20;
    private static final Pattern MARKER = Pattern.compile("^(#{1,6}|[-*+]|\\d+[.)])\\s+");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s");

    private KeyPointExtractor() {
    }

    public static List<String> extract(String content) {
        Set<String> points = new LinkedHashSet<>();
        boolean inCodeFence = false;
        for (String line : content.split("\\R")) {
            if (line.strip().startsWith("```")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            Matcher marker = MARKER.matcher(line.strip());
            if (!inCodeFence && marker.find()) {
                addIfPresent(points, line.strip().substring(marker.end()));
            }
        }
        if (points.isEmpty()) {
            for (String paragraph : content.split("\\R\\s*\\R")) {
                addIfPresent(points, SENTENCE_END.split(paragraph.strip(), 2)[0]);
            }
        }
        return points.stream().limit(LIMIT).toList();
    }

    private static void addIfPresent(Set<String> points, String text) {
        String point = text.strip().replaceAll("\\s+", " ");
        if (!point.isEmpty()) {
            points.add(point);
        }
    }
}
