package com.zaitul.servicedesk.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

	@Query("select c from TicketComment c join fetch c.author where c.ticket.id = :ticketId order by c.createdAt, c.id")
	List<TicketComment> findForTicket(long ticketId);

}
