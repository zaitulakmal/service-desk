package com.zaitul.servicedesk.attachment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

	@Query("select a from TicketAttachment a join fetch a.uploadedBy where a.ticket.id = :ticketId order by a.createdAt, a.id")
	List<TicketAttachment> findForTicket(long ticketId);

	Optional<TicketAttachment> findByIdAndTicketId(long id, long ticketId);

}
