package dev.iamrat.collector.source.application;

import java.util.List;

public record SourceCollectionResult(
    String source,
    boolean collected,
    List<String> availableSources
) {

    public static SourceCollectionResult collected(String source, List<String> availableSources) {
        return new SourceCollectionResult(source, true, List.copyOf(availableSources));
    }

    public static SourceCollectionResult unknown(String source, List<String> availableSources) {
        return new SourceCollectionResult(source, false, List.copyOf(availableSources));
    }
}
