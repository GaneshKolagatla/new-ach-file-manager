package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alacriti.model.DownloadEvent;

public interface DownloadEventRepository extends JpaRepository<DownloadEvent, Long> {

}