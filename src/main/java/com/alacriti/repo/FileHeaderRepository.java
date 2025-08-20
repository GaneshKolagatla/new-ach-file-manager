package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alacriti.model.FileHeaderDetails;

@Repository
public interface FileHeaderRepository extends JpaRepository<FileHeaderDetails, Long> {
}