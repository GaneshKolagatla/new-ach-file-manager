package com.alacriti.util;

import lombok.Data;

@Data
public class BatchHeader {
	public String recordTypeCode;
	public String serviceClassCode;
	public String companyName;
	public String companyDiscretionaryData;
	public String companyIdentification;
	public String standardEntryClassCode;
	public String companyEntryDescription;
	public String companyDescriptiveDate;
	public String effectiveEntryDate;
	public String settlementDate;
	public String originatorStatusCode;
	public String originatingDFI;
	public String batchNumber;
}