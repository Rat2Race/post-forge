package dev.iamrat.core.event;

public interface EventPublisher {

    boolean supports(EventType eventType);

    void publish(DomainEvent event);
}
