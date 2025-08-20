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

	@Autowired
	private ACHService achService;

	@Autowired
	private SftpConfigRepo repo;

	@Autowired
	private SftpDownloadService sftpDownloadService;

	@Autowired
	private ACHFileProcessorService achFileProcessorService;
	/*@Autowired
	private PGPDecryptor encryptionService;
	@Autowired
	private FileEventLoggerService remoteFileLogService;*/

	private static final String PRIVATE_KEY_PATH = "keys/private_key.asc"; // in resources
	private static final String DOWNLOAD_DIR = "target/download-ach";
	//private static final String DECRYPTED_DIR= "target/decrypted-ach";
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
	public String downloadAndDecryptACHFile(@RequestBody DownloadDto request) {
		try {
			// 1. Get SFTP config
			SftpConfig config = repo.findByClientKey(request.getClient_key()).orElseThrow(
					() -> new RuntimeException("SFTP config not found for client key: " + request.getClient_key()));

			// 2. Ensure local download directory exists
			Files.createDirectories(Paths.get(DOWNLOAD_DIR));
			File downloadDir = new File(DOWNLOAD_DIR);

			// 3. Download + Decrypt all files for today
			try (InputStream privateKeyStream = new ClassPathResource(PRIVATE_KEY_PATH).getInputStream()) {
				sftpDownloadService.downloadAndDecryptByDate(config.getClientKey(), DOWNLOAD_DIR, privateKeyStream,
						PASSPHRASE);
			}

			// 4. Parse all decrypted ACH files
			File[] decryptedFiles = downloadDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ach"));
			if (decryptedFiles == null || decryptedFiles.length == 0) {
				return "No ACH files found after decryption.";
			}

			for (File decryptedFile : decryptedFiles) {
				achFileProcessorService.processACHFile(decryptedFile);
			}

			return "File(s) downloaded, decrypted, parsed, and logged successfully.";

		} catch (Exception e) {
			e.printStackTrace();
			return "Download/Decryption/Parsing failed: " + e.getMessage();
		}
	}

}
