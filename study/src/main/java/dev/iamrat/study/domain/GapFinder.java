package dev.iamrat.study.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 설명에 거의 나오지 않은 핵심 항목을 찾는다. 한국어는 조사가 붙어 단어가 바뀌므로
 * 단어 안의 두 글자 조각이 절반 이상 겹치면 나온 것으로 본다.
 * 채점이 아니라 "AI 학생"이 되물을 거리를 고르는 용도다.
 */
public final class GapFinder {

    private static final double COVERED_RATIO = 0.5;

    private GapFinder() {
    }

    /**
     * 빈 페이지 글에 대부분 나온 핵심 항목의 번호. 사용자가 체크하기 전에 "언급한 것 같아요"로 미리 보여 줄 후보다.
     * 두 글자 조각이 없는 아주 짧은 항목은 겹침을 잴 수 없으니 후보로 내지 않는다.
     */
    public static List<Integer> mentionedIndexes(List<String> keyPoints, String text) {
        Set<String> mentioned = bigrams(text);
        return IntStream.range(0, keyPoints.size())
            .filter(index -> {
                Set<String> grams = bigrams(keyPoints.get(index));
                return !grams.isEmpty() && isCovered(grams, mentioned);
            })
            .boxed()
            .toList();
    }

    public static List<String> missing(List<String> keyPoints, String explanation) {
        Set<String> mentioned = bigrams(explanation);
        return keyPoints.stream()
            .filter(point -> !isCovered(bigrams(point), mentioned))
            .toList();
    }

    private static boolean isCovered(Set<String> pointGrams, Set<String> mentioned) {
        if (pointGrams.isEmpty()) {
            return true;
        }
        long hits = pointGrams.stream().filter(mentioned::contains).count();
        return hits >= pointGrams.size() * COVERED_RATIO;
    }

    private static Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            for (int i = 0; i + 2 <= word.length(); i++) {
                grams.add(word.substring(i, i + 2));
            }
        }
        return grams;
    }
}
