package com.alacriti.dto;

import java.util.List;

import lombok.Data;

@Data
public class ACHFileRequest {
    private String immediateDestination;
    private String immediateOrigin;
    private String fileIdModifier;
    private String destinationName;
    private String originName;
    private String financialInstitutionName;
    private String clientKey;
    private List<BatchRequest> batches;
}
