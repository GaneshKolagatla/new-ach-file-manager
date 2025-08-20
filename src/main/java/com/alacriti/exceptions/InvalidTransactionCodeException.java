package com.alacriti.exceptions;

public class InvalidTransactionCodeException extends ACHValidationException {
	public InvalidTransactionCodeException(String msg, int line) {
		super(msg, "TXN_CODE_INVALID", "Transaction Code", line);
	}

}
