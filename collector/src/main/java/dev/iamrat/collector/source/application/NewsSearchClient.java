package dev.iamrat.collector.source.application;

import dev.iamrat.collector.item.domain.CollectedItem;
import java.util.List;

public interface NewsSearchClient {

    List<CollectedItem> search(String keyword, int display);
}
