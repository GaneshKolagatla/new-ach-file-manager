package com.alacriti.dto;

import lombok.Data;

@Data
public class EntryDetailRequest {
    private String transactionCode;
    private String receivingDFIRoutingNumber;  // Now full 9-digit routing number
    private String dfiaAccountNumber;
    private long amount;
    private String individualIdNumber;
    private String individualName;
    private String traceNumber;
}

