package dev.iamrat.collector.source.application;

public interface DataSourceCollector {
    void collect();
    String getSourceName();
}
