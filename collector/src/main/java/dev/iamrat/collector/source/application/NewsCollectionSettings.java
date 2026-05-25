package dev.iamrat.collector.source.application;

import java.util.List;

public interface NewsCollectionSettings {

    boolean isConfigured();

    List<String> keywords();

    int display();
}
