package com.alacriti.util;

import lombok.Data;

@Data
public class BatchControl {
	public String recordTypeCode;
	public String serviceClassCode;
	public String entryAddendaCount;
	public String entryHash;
	public String totalDebitEntryDollarAmount;
	public String totalCreditEntryDollarAmount;
	public String companyIdentification;
	public String messageAuthenticationCode;
	public String reserved;
	public String originatingDFI;
	public String batchNumber;
}