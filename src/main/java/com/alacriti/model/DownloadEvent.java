package com.alacriti.model;

//this is the file details table creation class .....
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "file_details_tbl")
public class DownloadEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "client_key", nullable = false)
	private String clientKey;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "download_time", nullable = false)
	private LocalDateTime downloadTime;

	@Column(name = "status", nullable = false)
	private String status; // e.g., "SUCCESS" or "FAILURE"

	@Column(name = "remarks")
	private String remarks;

}
