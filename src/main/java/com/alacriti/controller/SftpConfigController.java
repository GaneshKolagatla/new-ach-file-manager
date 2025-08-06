package com.alacriti.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alacriti.model.SftpConfig;
import com.alacriti.repo.SftpConfigRepo;

@RestController
@RequestMapping("/api/sftp-configs")
public class SftpConfigController {

    @Autowired
    private SftpConfigRepo repo;

    // Create
    @PostMapping
    public ResponseEntity<SftpConfig> create(@RequestBody SftpConfig config) {
        return ResponseEntity.ok(repo.save(config));
    }

    // Read All
    @GetMapping
    public ResponseEntity<List<SftpConfig>> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    // Read One by clientKey
    @GetMapping("/{clientKey}")
    public ResponseEntity<SftpConfig> getByClientKey(@PathVariable String clientKey) {
        return repo.findByClientKey(clientKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/{clientKey}")
    public ResponseEntity<SftpConfig> update(@PathVariable String clientKey, @RequestBody SftpConfig updatedConfig) {
        return repo.findByClientKey(clientKey)
                .map(existing -> {
                    updatedConfig.setClientKey(clientKey);
                    return ResponseEntity.ok(repo.save(updatedConfig));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete
    @DeleteMapping("/{clientKey}")
    public ResponseEntity<Object> delete(@PathVariable String clientKey) {
        return repo.findByClientKey(clientKey)
                .map(existing -> {
                    repo.delete(existing);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
