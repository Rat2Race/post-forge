package dev.iamrat.collector.source.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceCollectionService {

    private final List<DataSourceCollector> collectors;

    public SourceCollectionResult trigger(String source) {
        List<String> availableSources = availableSources();
        DataSourceCollector target = collectors.stream()
            .filter(collector -> collector.getSourceName().equals(source))
            .findFirst()
            .orElse(null);

        if (target == null) {
            return SourceCollectionResult.unknown(source, availableSources);
        }

        target.collect();
        return SourceCollectionResult.collected(source, availableSources);
    }

    private List<String> availableSources() {
        return collectors.stream()
            .map(DataSourceCollector::getSourceName)
            .toList();
    }
}
