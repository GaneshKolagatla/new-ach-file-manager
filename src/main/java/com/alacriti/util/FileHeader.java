package com.alacriti.util;

import lombok.Data;

@Data
public class FileHeader {
	public String recordTypeCode;
	public String priorityCode;
	public String immediateDestination;
	public String immediateOrigin;
	public String fileCreationDate;
	public String fileCreationTime;
	public String fileIdModifier;
	public String recordSize;
	public String blockingFactor;
	public String formatCode;
	public String immediateDestinationName;
	public String immediateOriginName;
	public String referenceCode;
}