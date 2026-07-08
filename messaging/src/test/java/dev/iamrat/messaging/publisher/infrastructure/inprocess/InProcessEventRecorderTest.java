package dev.iamrat.messaging.publisher.infrastructure.inprocess;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.event.DomainEvent;
import dev.iamrat.core.event.EventPublisher;
import dev.iamrat.core.event.EventType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class InProcessEventRecorderTest {

    @Mock
    private EventPublisher supportedPublisher;

    @Mock
    private EventPublisher unsupportedPublisher;

    @Test
    @DisplayName("트랜잭션이 없으면 지원 이벤트를 즉시 발행한다")
    void recordWithoutTransactionPublishesSupportedEventImmediately() {
        EventType eventType = EventType.from("PriceDropDetectedEvent");
        given(supportedPublisher.supports(eventType)).willReturn(true);
        given(unsupportedPublisher.supports(eventType)).willReturn(false);
        InProcessEventRecorder recorder = new InProcessEventRecorder(
            List.of(supportedPublisher, unsupportedPublisher),
            new ObjectMapper()
        );

        recorder.record(eventType, "Product", "7", Map.of("productId", 7L));

        verify(supportedPublisher).publish(argThat((DomainEvent event) ->
            event.getEventType().equals("PriceDropDetectedEvent")
                && event.getAggregateType().equals("Product")
                && event.getAggregateId().equals("7")
                && event.getPayload().contains("\"productId\":7")
                && event.getEventId() != null
        ));
        verify(unsupportedPublisher, never()).publish(argThat(event -> true));
    }

    @Test
    @DisplayName("트랜잭션 안에서는 commit 이후 지원 이벤트를 발행한다")
    void recordInTransactionPublishesSupportedEventAfterCommit() {
        EventType eventType = EventType.from("PriceDropDetectedEvent");
        given(supportedPublisher.supports(eventType)).willReturn(true);
        InProcessEventRecorder recorder = new InProcessEventRecorder(
            List.of(supportedPublisher),
            new ObjectMapper()
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            recorder.record(eventType, "Product", "7", Map.of("productId", 7L));

            verify(supportedPublisher, never()).publish(argThat(event -> true));
            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());

            verify(supportedPublisher).publish(argThat((DomainEvent event) ->
                event.getEventType().equals("PriceDropDetectedEvent")
                    && event.getAggregateType().equals("Product")
                    && event.getAggregateId().equals("7")
            ));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
