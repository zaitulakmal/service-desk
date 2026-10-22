package com.zaitul.servicedesk.approval;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

	@Query("""
			select s from ApprovalStep s
			left join fetch s.approver
			left join fetch s.decidedBy
			where s.ticket.id = :ticketId
			order by s.stepOrder
			""")
	List<ApprovalStep> findForTicket(long ticketId);

	/** Steps waiting on this person, either by name or through one of their roles. */
	@Query("""
			select s from ApprovalStep s
			join fetch s.ticket t
			join fetch t.requester r
			left join s.approver a
			where s.status = com.zaitul.servicedesk.approval.ApprovalStatus.PENDING
			  and (a.id = :userId or s.approverRole in :roles)
			  and r.id <> :userId
			order by s.pendingSince
			""")
	List<ApprovalStep> findPendingFor(UUID userId, Collection<String> roles);

	@Query("""
			select s from ApprovalStep s
			join fetch s.ticket
			left join fetch s.approver
			where s.status = com.zaitul.servicedesk.approval.ApprovalStatus.PENDING
			order by s.pendingSince
			""")
	List<ApprovalStep> findOldestPending(Limit limit);

}
