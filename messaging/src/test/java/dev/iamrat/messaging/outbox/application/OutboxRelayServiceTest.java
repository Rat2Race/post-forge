package dev.iamrat.messaging.outbox.application;

import dev.iamrat.core.event.DomainEvent;
import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import dev.iamrat.messaging.outbox.domain.OutboxStatus;
import dev.iamrat.messaging.publisher.application.EventPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxMessageStore outboxMessageStore;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("relay가 비활성화되어 있으면 outbox를 조회하지 않는다")
    void relayPending_disabled_doesNotQueryOutbox() {
        OutboxRelayPolicy outboxRelayPolicy = TestOutboxRelayPolicy.disabled();
        OutboxRelayService relay = new OutboxRelayService(
            outboxMessageStore,
            List.of(eventPublisher),
            outboxRelayPolicy,
            new OutboxRetryService(outboxRelayPolicy),
            transactionManager
        );

        int publishedCount = relay.relayPending();

        assertThat(publishedCount).isZero();
        verify(outboxMessageStore, never()).findClaimableForUpdate(anyInt(), anyInt());
    }

    @Test
    @DisplayName("claim한 이벤트를 지원 dispatcher로 전달하고 published 상태로 마킹한다")
    void relayPending_dispatchesClaimedEvent() {
        OutboxRelayPolicy outboxRelayPolicy = TestOutboxRelayPolicy.enabled();
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        OutboxMessage message = OutboxMessage.pending("PostCreated", "post", "1", "{\"postId\":1}");
        ReflectionTestUtils.setField(message, "id", 1L);
        given(outboxMessageStore.findClaimableForUpdate(50, 5)).willReturn(List.of(message));
        given(outboxMessageStore.findById(1L)).willReturn(Optional.of(message));
        given(eventPublisher.supports(EventType.from("PostCreated"))).willReturn(true);

        OutboxRelayService relay = new OutboxRelayService(
            outboxMessageStore,
            List.of(eventPublisher),
            outboxRelayPolicy,
            new OutboxRetryService(outboxRelayPolicy),
            transactionManager
        );

        int publishedCount = relay.relayPending();

        assertThat(publishedCount).isEqualTo(1);
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("지원하는 publisher가 없으면 이벤트를 실패 처리하고 retry를 예약한다")
    void relayPending_withoutSupportedPublisher_marksFailed() {
        OutboxRelayPolicy outboxRelayPolicy = TestOutboxRelayPolicy.enabled();
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        OutboxMessage message = OutboxMessage.pending("PostCreated", "post", "1", "{\"postId\":1}");
        ReflectionTestUtils.setField(message, "id", 1L);
        given(outboxMessageStore.findClaimableForUpdate(50, 5)).willReturn(List.of(message));
        given(outboxMessageStore.findById(1L)).willReturn(Optional.of(message));
        given(eventPublisher.supports(EventType.from("PostCreated"))).willReturn(false);

        OutboxRelayService relay = new OutboxRelayService(
            outboxMessageStore,
            List.of(eventPublisher),
            outboxRelayPolicy,
            new OutboxRetryService(outboxRelayPolicy),
            transactionManager
        );

        int publishedCount = relay.relayPending();

        assertThat(publishedCount).isZero();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getLastError()).contains("No publisher supports event type: PostCreated");
        verify(eventPublisher, never()).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("publisher 발행 실패는 이벤트를 실패 처리하고 retry를 예약한다")
    void relayPending_publisherFails_marksFailed() {
        OutboxRelayPolicy outboxRelayPolicy = TestOutboxRelayPolicy.enabled();
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        OutboxMessage message = OutboxMessage.pending("PostCreated", "post", "1", "{\"postId\":1}");
        ReflectionTestUtils.setField(message, "id", 1L);
        given(outboxMessageStore.findClaimableForUpdate(50, 5)).willReturn(List.of(message));
        given(outboxMessageStore.findById(1L)).willReturn(Optional.of(message));
        given(eventPublisher.supports(EventType.from("PostCreated"))).willReturn(true);
        willThrow(new IllegalStateException("broker down"))
            .given(eventPublisher)
            .publish(any(DomainEvent.class));

        OutboxRelayService relay = new OutboxRelayService(
            outboxMessageStore,
            List.of(eventPublisher),
            outboxRelayPolicy,
            new OutboxRetryService(outboxRelayPolicy),
            transactionManager
        );

        int publishedCount = relay.relayPending();

        assertThat(publishedCount).isZero();
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(message.getRetryCount()).isEqualTo(1);
        assertThat(message.getLastError()).isEqualTo("broker down");
        verify(eventPublisher).publish(any(DomainEvent.class));
    }
}
