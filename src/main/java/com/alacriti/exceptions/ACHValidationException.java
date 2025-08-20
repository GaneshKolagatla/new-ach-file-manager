package com.alacriti.exceptions;

public class ACHValidationException extends Exception {
	private final String errorCode;
	private final String fieldName;
	private final int lineNumber;

	public ACHValidationException(String message) {
		super(message);
		errorCode = null;
		fieldName = null;
		lineNumber = -1;
	}

	public ACHValidationException(String message, String errorCode, String fieldName, int lineNumber) {
		super(message);
		this.errorCode = errorCode;
		this.fieldName = fieldName;
		this.lineNumber = lineNumber;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getFieldName() {
		return fieldName;
	}

	public int getLineNumber() {
		return lineNumber;
	}
}