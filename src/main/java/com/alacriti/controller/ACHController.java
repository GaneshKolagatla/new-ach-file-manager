package com.alacriti.controller;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alacriti.component.PGPDecryptor;
import com.alacriti.dto.ACHFileRequest;
import com.alacriti.dto.DownloadDto;
import com.alacriti.model.SftpConfig;
import com.alacriti.repo.SftpConfigRepo;
import com.alacriti.service.ACHService;
import com.alacriti.service.SftpDownloadService;

@RestController
@RequestMapping("/api/ach")
public class ACHController {

    @Autowired
    private ACHService achService;
    
    @Autowired
    private SftpConfigRepo repo;
    
    @Autowired
    private SftpDownloadService sftpDownloadService;
    
    @Autowired
    private PGPDecryptor encryptionService;
    
    private static final String PRIVATE_KEY_PATH = "keys/private_key.asc"; // in resources
	private static final String DOWNLOAD_DIR = "target/decrypted-ach";
	private static final String PASSPHRASE = "6301110431";

	private static final String OUTPUT_DIR = "target/ach-files";
	private static final String PUBLIC_KEY_PATH = "keys/public_key.asc"; // placed in src/main/resources/keys/


    @PostMapping("/generate-ach")
    public ResponseEntity<String> generateAchFile(@RequestBody ACHFileRequest request) {
        try {
            String encryptedFileName = achService.generateAndEncryptAndSendACHFile(request);
            return ResponseEntity.ok("ACH file encrypted and uploaded successfully: " + encryptedFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while processing ACH file: " + e.getMessage());
        }
    }
    @PostMapping("/download")
	public String downloadAndDecryptACHFile(@RequestBody DownloadDto requestt) {
		SftpConfig request = repo.findByClientKey(requestt.getClient_key())
			.orElseThrow(() -> new RuntimeException("SFTP config not found for client key: " + requestt.getClient_key()));

		try {
			Files.createDirectories(Paths.get(DOWNLOAD_DIR));
			String fileName = requestt.getFile_name(); // from your DownloadDto
			File encryptedFile = Paths.get(DOWNLOAD_DIR, fileName).toFile();

			// Step 1: Download file from SFTP
			sftpDownloadService.downloadFile(request.getClientKey(), fileName, encryptedFile);

			// Step 2: Decrypt
			String decryptedFileName = fileName.replace(".pgp", ".ach");
			File decryptedFile = Paths.get(DOWNLOAD_DIR, decryptedFileName).toFile();

			try (InputStream privateKeyStream = new ClassPathResource(PRIVATE_KEY_PATH).getInputStream()) {
				encryptionService.decryptFile(encryptedFile, decryptedFile, privateKeyStream, PASSPHRASE);
			}

			return "File downloaded and decrypted successfully: " + decryptedFileName;
		} catch (Exception e) {
			e.printStackTrace();
			return "Download/Decryption failed: " + e.getMessage();
		}
	}
}
 