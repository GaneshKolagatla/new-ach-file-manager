package com.alacriti.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BatchRequest {
    private String serviceClassCode;           // e.g., 220
    private String companyName;                // 16 chars max
    private String companyDiscretionaryData;   // 20 chars, optional
    private String companyIdentification;      // 10 chars
    private String standardEntryClassCode;     // e.g., PPD, CCD
    private String companyEntryDescription;    // 10 chars
    private String companyDescriptiveDate;     // 6 chars, optional
    private String effectiveEntryDate;         // YYMMDD
    // Settlement Date will be blank initially
    @JsonProperty("originatingDFI")
    private String originatingDFIIdentification;
 // First 8 digits of routing number
    private int batchNumber;                   // Sequential within file
    private List<EntryDetailRequest> entries;  // Entry details
}
