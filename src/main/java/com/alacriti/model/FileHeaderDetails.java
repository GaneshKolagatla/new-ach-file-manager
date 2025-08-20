package com.alacriti.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
@Table(name = "file_header_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHeaderDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recordType;
	private String priorityCode;
	private String immediateDestination;
	private String immediateOrigin;
	private LocalDate fileCreationDate;
	private LocalTime fileCreationTime;
	private String fileIdModifier;
	private String recordSize;
	private String blockingFactor;
	private String formatCode;
	private String destinationName;
	private String originName;
	private String referenceCode;

	private String clientKey;
	private String fileName;
	private LocalDateTime createdAt;
}
