package dev.iamrat.messaging.outbox.application;

import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import java.util.List;
import java.util.Optional;

public interface OutboxMessageStore {

    OutboxMessage save(OutboxMessage message);

    Optional<OutboxMessage> findById(Long id);

    List<OutboxMessage> findClaimableForUpdate(int limit, int maxRetries);
}
