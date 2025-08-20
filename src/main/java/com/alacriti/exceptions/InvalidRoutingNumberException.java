package com.alacriti.exceptions;

public class InvalidRoutingNumberException extends ACHValidationException {
	public InvalidRoutingNumberException(String msg, int line) {
		super(msg, "ROUTING_INVALID", "Routing Number", line);
	}

}
