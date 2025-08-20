package com.alacriti.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.alacriti.model.RemoteFileEvent;
import com.alacriti.repo.RemoteFileEventRepository;

@Service
public class FileEventLoggerService {

	private final RemoteFileEventRepository repo;

	public FileEventLoggerService(RemoteFileEventRepository repo) {
		this.repo = repo;
	}

	/**
	 * Create a new event record in its own transaction so it isn't rolled back by parser failures.
	 * eventType: "UPLOAD" or "DOWNLOAD"
	 */
	public RemoteFileEvent createEvent(String clientKey, String fileName, String eventType) {
		RemoteFileEvent ev = new RemoteFileEvent();
		ev.setClientKey(clientKey);
		ev.setFileName(fileName);
		ev.setEventType(eventType.toUpperCase());
		ev.setSequenceNo(generateSequenceNo(eventType)); // e.g., U101 or D101
		ev.setEventTime(LocalDateTime.now());
		ev.setStatus("STARTED");
		return repo.save(ev);
	}

	/**
	 * Update an existing event's status safely in its own transaction.
	 */
	public void updateEventStatus(Long id, String status, String remarks) {
		repo.findById(id).ifPresent(ev -> {
			ev.setStatus(status.toUpperCase());
			ev.setRemarks(remarks);
			ev.setEventTime(LocalDateTime.now());
			repo.save(ev);
		});
	}

	/**
	 * Generate a unique sequence number based on event type.
	 * Example: U101, D202
	 */
	private String generateSequenceNo(String eventType) {
		String prefix = eventType.equalsIgnoreCase("UPLOAD") ? "U" : "D";
		long count = repo.countByEventType(eventType.toUpperCase()) + 1;
		return prefix + String.format("%03d", count); // U001, D002, etc.
	}
}
