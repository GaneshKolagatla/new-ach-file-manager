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
public class EntryDetail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recordType;
	private String transactionCode;
	private String receivingDfiIdentification;
	private String checkDigit;
	private String dfiAccountNumber;
	private String amount;
	private String individualIdNumber;
	private String individualName;
	private String discretionaryData;
	private String addendaRecordIndicator;
	private String traceNumber;

	private String fileName;
	private LocalDateTime createdAt;
}
