package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alacriti.model.FileHeader;

@Repository
public interface FileHeaderRepository extends JpaRepository<FileHeader, Long> {
}
