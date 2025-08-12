package com.alacriti.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sftp_config_tbl")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SftpConfig {

    @Id
    @Column(name = "client_key", nullable = false, unique = true)
    private String clientKey;  // Unique identifier for each client (e.g., "abc_corp")

    @Column(name = "host", nullable = false)
    private String host;

    @Column(name = "port", nullable = false)
    private int port;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "remote_directory", nullable = false)
    private String remoteDirectory;

    @Column(name = "description")
    private String description;  // Optional: for clarity (e.g., "Bank A SFTP")

}
