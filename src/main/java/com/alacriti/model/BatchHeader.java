package com.alacriti.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchHeader {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recordType;
	private String serviceClassCode;
	private String companyName;
	private String companyDiscretionaryData;
	private String companyIdentification;
	private String standardEntryClassCode;
	private String companyEntryDescription;
	private String companyDescriptiveDate;
	private String effectiveEntryDate;
	private String settlementDate;
	private String originatorStatusCode;
	private String originatingDfiIdentification;
	private String batchNumber;

	private String fileName;
	private LocalDateTime createdAt;
}
