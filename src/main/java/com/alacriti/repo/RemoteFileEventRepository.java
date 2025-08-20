package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alacriti.model.RemoteFileEvent;

public interface RemoteFileEventRepository extends JpaRepository<RemoteFileEvent, Long> {
	long countByEventType(String eventType);
}
