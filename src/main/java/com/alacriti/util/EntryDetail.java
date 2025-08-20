package com.alacriti.util;

import lombok.Data;

@Data
public class EntryDetail {
	public String recordTypeCode;
	public String transactionCode;
	public String receivingDFIIdentification;
	public String checkDigit;
	public String dfiAccountNumber;
	public String amount;
	public String individualIdentificationNumber;
	public String individualName;
	public String discretionaryData;
	public String addendaRecordIndicator;
	public String traceNumber;
}