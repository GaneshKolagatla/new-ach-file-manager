package com.alacriti.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alacriti.dto.ACHFileRequest;
import com.alacriti.dto.DownloadDto;
import com.alacriti.model.SftpConfig;
import com.alacriti.repo.SftpConfigRepo;
import com.alacriti.service.ACHFileProcessorService;
import com.alacriti.service.ACHService;
import com.alacriti.service.SftpDownloadService;

@RestController
@RequestMapping("/api/ach")
public class ACHController {

	private static final Logger log = LoggerFactory.getLogger(ACHController.class);

	@Autowired
	private ACHService achService;

	@Autowired
	private SftpConfigRepo repo;

	@Autowired
	private SftpDownloadService sftpDownloadService;

	@Autowired
	private ACHFileProcessorService achFileProcessorService;

	private static final String PRIVATE_KEY_PATH = "keys/private_key.asc";
	private static final String DOWNLOAD_DIR = "target/download-ach";
	private static final String DECRYPTED_DIR = "target/decrypted-ach";
	private static final String PASSPHRASE = "8823027374";

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
	public String downloadAndDecryptACHFile(@RequestBody DownloadDto request) {
		try {
			SftpConfig config = repo.findByClientKey(request.getClient_key()).orElseThrow(
					() -> new RuntimeException("SFTP config not found for client key: " + request.getClient_key()));

			Files.createDirectories(Paths.get(DOWNLOAD_DIR));
			Files.createDirectories(Paths.get(DECRYPTED_DIR));

			// Use today's date automatically
			String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

			sftpDownloadService.downloadAndDecryptByDate(config, today, DOWNLOAD_DIR, DECRYPTED_DIR, PRIVATE_KEY_PATH,
					PASSPHRASE);

			File[] decryptedFiles = new File(DECRYPTED_DIR).listFiles(file -> file.isFile());

			if (decryptedFiles == null || decryptedFiles.length == 0) {
				log.info("No ACH files found for today.");
				return "No ACH files found for today.";
			}

			int processedCount = 0;
			int skippedCount = 0;

			for (File decryptedFile : decryptedFiles) {
				if (decryptedFile.getName().toLowerCase().endsWith(".ach") && decryptedFile.length() > 0) {
					achFileProcessorService.processACHFile(decryptedFile);//error
					log.info("✅ Processed file: {}", decryptedFile.getName());
					processedCount++;
				} else {
					log.warn("⚠️ Skipped file: {}", decryptedFile.getName());
					skippedCount++;
				}
			}

			return String.format("Today's files: %d processed and %d skipped due to empty content so that is all done.",
					processedCount, skippedCount);

		} catch (Exception e) {
			e.printStackTrace();
			log.error("Download/Decryption/Parsing failed: {}", e.getMessage());
			return "Download/Decryption/Parsing failed: " + e.getMessage();
		}
	}
}
