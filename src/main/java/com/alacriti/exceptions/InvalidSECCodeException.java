package com.alacriti.exceptions;

public class InvalidSECCodeException extends ACHValidationException {
	public InvalidSECCodeException(String msg, int line) {
		super(msg, "SEC_CODE_INVALID", "SEC Code", line);
	}

}
