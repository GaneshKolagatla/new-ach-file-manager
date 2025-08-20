package com.alacriti.model;

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
@Table(name = "entry_details_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntryDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String batchNumber;
	private String recordType;
	private String transactionCode;
	private String rdfiRoutingNumber;
	private String checkDigit;
	private String rdfiAccountNumber;
	private Integer amount;
	private String individualIdNumber;
	private String individualName;
	private String discretionaryData;
	private Integer addendaRecordIndicator;
	private String traceNumber;
	private String receivingDFI;
	private String originatingDfiId;

	private String fileName;
	private LocalDateTime createdAt;

	// Optional relationship if needed
	// @ManyToOne
	// @JoinColumn(name = "batch_header_id")
	// private BatchHeaderDetails batchHeader;
}
