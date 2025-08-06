package com.alacriti.dto;



import java.util.List;

import lombok.Data;

@Data
public class BatchRequest {
    private String serviceClassCode;
    private String companyName;
    private String companyIdentification;
    private String standardEntryClassCode;
    private String companyEntryDescription;
    private String effectiveEntryDate;
    private String originatingDFIIdentification;
    private int batchNumber;
    private List<EntryDetailRequest> entries;
}
