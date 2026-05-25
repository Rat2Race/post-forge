package dev.iamrat.messaging.outbox.infrastructure.persistence;

import dev.iamrat.messaging.outbox.application.OutboxMessageStore;
import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxMessageRepository implements OutboxMessageStore {

    private final OutboxMessageJpaRepository jpaRepository;

    @Override
    public OutboxMessage save(OutboxMessage message) {
        return jpaRepository.save(message);
    }

    @Override
    public Optional<OutboxMessage> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<OutboxMessage> findClaimableForUpdate(int limit, int maxRetries) {
        return jpaRepository.findClaimableForUpdate(limit, maxRetries);
    }
}
