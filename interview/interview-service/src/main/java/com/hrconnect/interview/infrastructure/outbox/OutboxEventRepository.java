package com.hrconnect.interview.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findUnpublishedEventsWithLimit(@Param("limit") int limit, @Param("maxRetry") int maxRetry);

    default List<OutboxEvent> findUnpublishedEventsWithLimit(int limit) {
        return findUnpublishedEventsWithLimit(limit, 5);
    }
}
