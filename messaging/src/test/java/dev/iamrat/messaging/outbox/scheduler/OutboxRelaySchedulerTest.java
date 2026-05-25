package dev.iamrat.messaging.outbox.scheduler;

import dev.iamrat.messaging.outbox.application.OutboxRelayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxRelaySchedulerTest {

    private final OutboxRelayService outboxRelayService = mock(OutboxRelayService.class);
    private final OutboxRelayScheduler scheduler = new OutboxRelayScheduler(outboxRelayService);

    @Test
    @DisplayName("스케줄 트리거는 outbox relay 유스케이스에 위임한다")
    void relayPending_delegatesToRelayService() {
        scheduler.relayPending();

        verify(outboxRelayService).relayPending();
    }
}
