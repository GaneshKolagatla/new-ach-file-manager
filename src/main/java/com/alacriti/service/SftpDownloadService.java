package com.alacriti.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.alacriti.model.SftpConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SftpDownloadService {

	private final PGPEncryptionService pgpEncryptionService;

	public SftpDownloadService(PGPEncryptionService pgpEncryptionService) {
		this.pgpEncryptionService = pgpEncryptionService;
	}

	public void downloadAndDecryptByDate(SftpConfig config, String date, String downloadDir, String decryptedDir,
			String privateKeyPath, String passphrase) throws Exception {

		List<File> downloadedFiles = downloadFilesFromSftp(config, date, downloadDir);

		if (downloadedFiles.isEmpty()) {
			log.info("⚠️ No files found on SFTP for date {}", date);
			return;
		}

		File decryptedFolder = new File(decryptedDir);
		if (!decryptedFolder.exists())
			decryptedFolder.mkdirs();

		for (File encryptedFile : downloadedFiles) {
			log.info("⬇️ Downloaded: {}", encryptedFile.getName());

			// Remove .pgp suffix for decrypted file
			String decryptedFileName = encryptedFile.getName().endsWith(".pgp")
					? encryptedFile.getName().substring(0, encryptedFile.getName().length() - 4)
					: encryptedFile.getName();

			File decryptedFile = new File(decryptedFolder, decryptedFileName);

			try (InputStream keyStream = new ClassPathResource(privateKeyPath).getInputStream()) {
				pgpEncryptionService.decryptACHFile(encryptedFile, decryptedFile, keyStream, passphrase);

				if (decryptedFile.length() > 0) {
					log.info("✅ Successfully decrypted: {}", decryptedFile.getAbsolutePath());
				} else {
					log.warn("⚠️ Decrypted file is empty, skipping: {}", decryptedFile.getName());
					decryptedFile.delete();
				}
			} catch (Exception e) {
				log.error("❌ Failed to decrypt file {}: {}", encryptedFile.getName(), e.getMessage());
			}
		}
	}

	private List<File> downloadFilesFromSftp(SftpConfig config, String date, String downloadDir) throws Exception {
		List<File> downloadedFiles = new ArrayList<>();

		JSch jsch = new JSch();
		Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
		session.setPassword(config.getPassword());
		session.setConfig("StrictHostKeyChecking", "no");
		session.connect();

		ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
		sftp.connect();

		List<ChannelSftp.LsEntry> files = new ArrayList<>();
		sftp.ls(config.getRemoteDirectory()).forEach(obj -> files.add((ChannelSftp.LsEntry) obj));

		for (ChannelSftp.LsEntry entry : files) {
			String fileName = entry.getFilename();

			if (entry.getAttrs().isDir() || fileName.startsWith("."))
				continue;

			// Only download files containing today's date
			if (!fileName.contains(date))
				continue;

			File localFile = new File(downloadDir, fileName);
			try (FileOutputStream fos = new FileOutputStream(localFile)) {
				sftp.get(config.getRemoteDirectory() + "/" + fileName, fos);
			}

			downloadedFiles.add(localFile);
			log.info("⬇️ File downloaded from SFTP: {}", fileName);
		}

		sftp.disconnect();
		session.disconnect();

		return downloadedFiles;
	}
}
