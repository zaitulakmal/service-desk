package com.zaitul.servicedesk.ticket;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

	/** Loads requester and assignee with the tickets, avoiding one extra query per row. */
	@Override
	@EntityGraph(attributePaths = { "requester", "assignee" })
	Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

	@Query("select t.status as status, count(t) as total from Ticket t where t.requester.id = :requesterId group by t.status")
	List<StatusCount> countByStatusForRequester(UUID requesterId);

	long countByStatusIn(Collection<TicketStatus> statuses);

	long countByStatus(TicketStatus status);

	long countBySlaBreachedAtAfter(Instant since);

	@Query("select t.category as category, count(t) as total from Ticket t where t.status in :statuses group by t.category")
	List<CategoryCount> countByCategory(Collection<TicketStatus> statuses);

	@Query(value = """
			select avg(extract(epoch from (first_response_at - created_at)))
			from ticket
			where created_at >= :since and first_response_at is not null
			""", nativeQuery = true)
	@Nullable Double averageFirstResponseSeconds(Instant since);

	@Query("""
			select t from Ticket t join fetch t.requester left join fetch t.assignee
			where t.status in :statuses and t.slaDueAt is not null
			order by t.slaDueAt
			""")
	List<Ticket> findBySlaDue(Collection<TicketStatus> statuses, Limit limit);

	/** Overdue tickets not yet flagged, locked so two API replicas never flag the same one. */
	@Query(value = """
			select * from ticket
			where status in ('OPEN', 'IN_PROGRESS') and sla_breached_at is null and sla_due_at < :now
			order by sla_due_at
			limit :limit
			for update skip locked
			""", nativeQuery = true)
	List<Ticket> lockOverdue(Instant now, int limit);

	interface StatusCount {

		TicketStatus getStatus();

		long getTotal();

	}

	interface CategoryCount {

		TicketCategory getCategory();

		long getTotal();

	}

}
