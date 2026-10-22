package com.zaitul.servicedesk.events;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	/**
	 * Locks the oldest unpublished events. SKIP LOCKED lets several API replicas relay
	 * in parallel without sending the same event twice.
	 */
	@Query(value = """
			select * from outbox_event
			where published_at is null
			order by created_at
			limit :batchSize
			for update skip locked
			""", nativeQuery = true)
	List<OutboxEvent> lockUnpublished(int batchSize);

	long countByPublishedAtIsNull();

}
