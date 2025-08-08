package com.alacriti.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alacriti.model.EntryDetail;

@Repository
public interface EntryDetailRepository extends JpaRepository<EntryDetail, Long> {
}
