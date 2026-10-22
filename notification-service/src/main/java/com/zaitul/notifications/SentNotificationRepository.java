package com.zaitul.notifications;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotification, Long> {

	List<SentNotification> findByTicketId(long ticketId);

}
