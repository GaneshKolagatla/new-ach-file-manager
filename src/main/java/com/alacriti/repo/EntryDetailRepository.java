package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alacriti.model.EntryDetails;

@Repository
public interface EntryDetailRepository extends JpaRepository<EntryDetails, Long> {
}