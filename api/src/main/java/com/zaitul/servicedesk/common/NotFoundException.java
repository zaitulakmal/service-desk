package com.zaitul.servicedesk.common;

/** Base class for "the thing you asked for does not exist" errors. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
