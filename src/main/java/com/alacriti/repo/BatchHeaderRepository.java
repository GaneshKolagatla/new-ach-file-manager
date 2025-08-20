package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alacriti.model.BatchHeaderDetails;

@Repository
public interface BatchHeaderRepository extends JpaRepository<BatchHeaderDetails, Long> {
}