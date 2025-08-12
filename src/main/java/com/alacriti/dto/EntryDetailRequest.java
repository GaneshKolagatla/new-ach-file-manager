package com.alacriti.dto;

import lombok.Data;

@Data
public class EntryDetailRequest {

    // NACHA Field 2 – Transaction Code (e.g., 22 for Checking Credit, 27 for Checking Debit)
    private String transactionCode;

    // NACHA Field 3 – RDFI Routing Number (first 8 digits of the bank's routing number)
    private String rdfiRoutingNumber;

    // NACHA Field 4 – Check Digit (9th digit of routing number)
    private String checkDigit;

    // NACHA Field 5 – DFI Account Number
    private String dfiAccountNumber;

    // NACHA Field 6 – Amount in cents (e.g., 1000 for $10.00)
    private int amount;

    // NACHA Field 7 – Individual Identification Number
    private String individualIdNumber;

    // NACHA Field 8 – Individual Name
    private String individualName;

    // NACHA Field 9 – Discretionary Data (optional)
    private String discretionaryData;

    // NACHA Field 10 – Addenda Record Indicator (0 = no addenda, 1 = addenda exists)
    private int addendaRecordIndicator;

    // NACHA Field 11 – Trace Number (8-digit ODFI routing + 7-digit sequence)
    private String traceNumber;

    // NACHA Field 12 – Originating DFI Identification (first 8 digits of ODFI's routing number)
    private String originatingDfiId;
}
