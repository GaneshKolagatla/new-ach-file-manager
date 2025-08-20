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
@Table(name = "file_summary_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSummaryDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recordType;
	private String batchCount;
	private String blockCount;
	private String entryAddendaCount;
	private String entryHash;
	private String totalDebitEntryDollarAmount;
	private String totalCreditEntryDollarAmount;
	private String reserved;

	private String fileName;
	private LocalDateTime createdAt;
}
