package com.alacriti.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alacriti.model.DownloadEvent;
import com.alacriti.repo.DownloadEventRepository;

@Service
public class DownloadEventService {

	@Autowired
	private DownloadEventRepository repo;

	public DownloadEvent createEvent(String clientKey, String fileName, String status, String remarks) {
		DownloadEvent ev = new DownloadEvent();
		ev.setClientKey(clientKey);
		ev.setFileName(fileName);
		ev.setDownloadTime(LocalDateTime.now());
		ev.setStatus(status);
		ev.setRemarks(remarks);
		return repo.save(ev);
	}
}
