package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alacriti.model.FileSummaryDetails;

public interface FileSummaryRepository extends JpaRepository<FileSummaryDetails, Long> {
}
