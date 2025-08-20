package com.alacriti.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "batch_header_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchHeaderDetails {

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
	private LocalDate effectiveEntryDate;
	private String settlementDate;
	private String originatorStatusCode;
	private String originatingDFIIdentification;
	private String batchNumber;

	private String fileName;
	private LocalDateTime createdAt;

	// Optional relationship if needed
	// @ManyToOne
	// @JoinColumn(name = "file_header_id")
	// private FileHeaderDetails fileHeader;
}
