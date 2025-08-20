package com.alacriti.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "remote_file_tbl")
public class RemoteFileEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "client_key", nullable = false, length = 50)
	private String clientKey;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "event_type", nullable = false, length = 20) // DOWNLOAD / UPLOAD
	private String eventType;

	@Column(name = "sequence_no", length = 20) // e.g., U101, D101 for unique tracking
	private String sequenceNo;

	@Column(name = "status", nullable = false, length = 20) // STARTED / SUCCESS / FAILED
	private String status;

	@Column(name = "event_time", nullable = false)
	private LocalDateTime eventTime;

	@Column(name = "remarks", length = 500)
	private String remarks;
}
