package com.zaitul.servicedesk.common;

/** A request field broke a rule that annotations cannot express. Mapped to HTTP 400. */
public class InvalidFieldException extends RuntimeException {

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return this.field;
	}

}
