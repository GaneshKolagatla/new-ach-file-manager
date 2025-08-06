package com.alacriti.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alacriti.model.SftpConfig;

public interface SftpConfigRepo extends JpaRepository<SftpConfig, String> {
	Optional<SftpConfig> findByClientKey(String clientKey);
}
