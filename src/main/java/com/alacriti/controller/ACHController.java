package com.alacriti.controller;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

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
import com.alacriti.model.DownloadEvent;
import com.alacriti.model.SftpConfig;
import com.alacriti.repo.DownloadEventRepository;
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
	private DownloadEventRepository eventRepository; // this is injected by Spring

	@Autowired
	private PGPDecryptor encryptionService;

	private static final String PRIVATE_KEY_PATH = "keys/my-private-key.asc"; // in resources
	private static final String DOWNLOAD_DIR = "target/decrypted-ach";
	private static final String PASSPHRASE = "8823027374";

	@SuppressWarnings("unused")
	private static final String OUTPUT_DIR = "target/ach-files";
	@SuppressWarnings("unused")
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
		SftpConfig request = repo.findByClientKey(requestt.getClient_key()).orElseThrow(
				() -> new RuntimeException("SFTP config not found for client key: " + requestt.getClient_key()));

		try {
			Files.createDirectories(Paths.get(DOWNLOAD_DIR));
			String fileName = requestt.getFile_name(); // .pgp
			File encryptedFile = Paths.get(DOWNLOAD_DIR, fileName).toFile();

			// Step 1: Download file from SFTP
			sftpDownloadService.downloadFile(request.getClientKey(), fileName, encryptedFile);

			// Step 2: Decrypt
			String decryptedFileName = fileName.replace(".pgp", "");
			File decryptedFile = Paths.get(DOWNLOAD_DIR, decryptedFileName).toFile();

			try (InputStream privateKeyStream = new ClassPathResource(PRIVATE_KEY_PATH).getInputStream()) {
				encryptionService.decryptFile(encryptedFile, decryptedFile, privateKeyStream, PASSPHRASE);
			}

			// Step 3: Parse .ach and insert records
			achService.processACHFile(decryptedFile); // this is where the parsing happens

			// Step 4: Save download event
			DownloadEvent event = new DownloadEvent();
			event.setClientKey(request.getClientKey());
			event.setFileName(fileName);
			event.setDownloadTime(LocalDateTime.now());
			event.setStatus("SUCCESS");
			event.setRemarks("File downloaded, decrypted, and processed.");
			eventRepository.save(event);

			return "File downloaded, decrypted, and processed successfully: " + decryptedFileName;

		} catch (Exception e) {
			e.printStackTrace();

			// Log failed download
			DownloadEvent event = new DownloadEvent();
			event.setClientKey(request.getClientKey());
			event.setFileName(requestt.getFile_name());
			event.setDownloadTime(LocalDateTime.now());
			event.setStatus("FAILURE");
			event.setRemarks("Error: " + e.getMessage());
			eventRepository.save(event);

			return "Download/Decryption failed: " + e.getMessage();
		}
	}

}
