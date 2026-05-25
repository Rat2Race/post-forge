package dev.iamrat.core.event;

public interface DomainEventRecorder {

    void record(EventType eventType, String aggregateType, String aggregateId, Object payload);
}
