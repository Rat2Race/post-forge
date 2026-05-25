package dev.iamrat.messaging.outbox.infrastructure.persistence;

import dev.iamrat.messaging.outbox.domain.OutboxMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxMessageJpaRepository extends JpaRepository<OutboxMessage, Long> {

    @Query(value = """
        select *
          from outbox_events
         where status in ('PENDING', 'FAILED')
           and available_at <= current_timestamp
           and retry_count < :maxRetries
         order by available_at asc, id asc
         limit :limit
         for update skip locked
        """, nativeQuery = true)
    List<OutboxMessage> findClaimableForUpdate(@Param("limit") int limit, @Param("maxRetries") int maxRetries);
}
