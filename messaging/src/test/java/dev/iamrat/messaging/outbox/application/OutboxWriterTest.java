package dev.iamrat.messaging.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iamrat.core.event.EventType;
import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import dev.iamrat.messaging.outbox.domain.OutboxStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock
    private OutboxMessageStore outboxMessageStore;

    private OutboxWriter outboxWriter;

    @BeforeEach
    void setUp() {
        outboxWriter = new OutboxWriter(outboxMessageStore, new ObjectMapper());
    }

    @Test
    @DisplayName("write는 payload를 JSON으로 직렬화해 PENDING outbox 메시지로 저장한다")
    void write_savesPendingOutboxMessage() {
        given(outboxMessageStore.save(any(OutboxMessage.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        OutboxMessage saved = outboxWriter.write(
            "CommentCreated",
            "comment",
            "42",
            Map.of("commentId", 42L, "postId", 7L)
        );

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageStore).save(captor.capture());

        OutboxMessage message = captor.getValue();
        assertThat(saved).isSameAs(message);
        assertThat(message.getEventType()).isEqualTo("CommentCreated");
        assertThat(message.getAggregateType()).isEqualTo("comment");
        assertThat(message.getAggregateId()).isEqualTo("42");
        assertThat(message.getPayload()).contains("\"commentId\":42");
        assertThat(message.getPayload()).contains("\"postId\":7");
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("record는 core event 기록 계약을 PENDING outbox 메시지로 변환한다")
    void record_savesPendingOutboxMessage() {
        given(outboxMessageStore.save(any(OutboxMessage.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        outboxWriter.record(
            EventType.from("PostCreated"),
            "post",
            "7",
            Map.of("postId", 7L)
        );

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageStore).save(captor.capture());

        OutboxMessage message = captor.getValue();
        assertThat(message.getEventType()).isEqualTo("PostCreated");
        assertThat(message.getAggregateType()).isEqualTo("post");
        assertThat(message.getAggregateId()).isEqualTo("7");
        assertThat(message.getPayload()).contains("\"postId\":7");
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}
