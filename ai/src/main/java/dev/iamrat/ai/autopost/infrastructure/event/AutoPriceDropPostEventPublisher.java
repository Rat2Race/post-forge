package dev.iamrat.ai.autopost.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.ai.autopost.application.AutoPriceDropPostService;
import dev.iamrat.ai.autopost.application.PriceDropDetectedPayload;
import dev.iamrat.core.event.DomainEvent;
import dev.iamrat.core.event.EventPublisher;
import dev.iamrat.core.event.EventType;
import dev.iamrat.price.tracking.application.PriceSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoPriceDropPostEventPublisher implements EventPublisher {

    private final ObjectMapper objectMapper;
    private final AutoPriceDropPostService autoPriceDropPostService;

    @Override
    public boolean supports(EventType eventType) {
        return PriceSnapshotService.PRICE_DROP_DETECTED.equals(eventType.value());
    }

    @Override
    public void publish(DomainEvent event) {
        autoPriceDropPostService.createPriceDropPost(event.getEventId(), readPayload(event));
    }

    private PriceDropDetectedPayload readPayload(DomainEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), PriceDropDetectedPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("PriceDropDetectedEvent payload is not readable", e);
        }
    }
}
