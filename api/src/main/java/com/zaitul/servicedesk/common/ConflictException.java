package com.zaitul.servicedesk.common;

/** Base class for requests that clash with the current state of a resource. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
