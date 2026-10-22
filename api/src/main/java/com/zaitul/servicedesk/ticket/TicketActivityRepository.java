package com.zaitul.servicedesk.ticket;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, Long> {

	@Query("select a from TicketActivity a where a.ticket.id = :ticketId order by a.createdAt desc, a.id desc")
	List<TicketActivity> findForTicket(long ticketId);

	/** The newest activity of each ticket, for the "Latest update" column. */
	@Query("""
			select a from TicketActivity a
			where a.ticket.id in :ticketIds
			  and a.id = (select max(b.id) from TicketActivity b where b.ticket.id = a.ticket.id)
			""")
	List<TicketActivity> findLatestForTickets(Collection<Long> ticketIds);

}
