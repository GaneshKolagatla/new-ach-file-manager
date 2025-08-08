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
public class FileHeader {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String recordType;
	private String priorityCode;
	private String immediateDestination;
	private String immediateOrigin;
	private String fileCreationDate;
	private String fileCreationTime;
	private String fileIdModifier;
	private String recordSize;
	private String blockingFactor;
	private String formatCode;
	private String immediateDestinationName;
	private String immediateOriginName;
	private String referenceCode;

	private String fileName;
	private LocalDateTime createdAt;
}
