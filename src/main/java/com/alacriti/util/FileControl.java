package com.alacriti.util;

import lombok.Data;

@Data
public class FileControl {
	public String recordTypeCode;
	public String batchCount;
	public String blockCount;
	public String entryAddendaCount;
	public String entryHash;
	public String totalDebitEntryDollarAmount;
	public String totalCreditEntryDollarAmount;
	public String reserved;
}