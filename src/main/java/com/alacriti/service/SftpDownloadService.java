package com.alacriti.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alacriti.component.PGPDecryptor;
import com.alacriti.model.SftpConfig;
import com.alacriti.repo.SftpConfigRepo;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Service
public class SftpDownloadService {

	private static final Logger logger = LoggerFactory.getLogger(SftpDownloadService.class);

	@Autowired
	private SftpConfigRepo sftpConfigRepository;

	@Autowired
	private PGPDecryptor pgpDecryptor;

	/**
	 * Downloads all files containing today's date from SFTP,
	 * then decrypts them into .ach files.
	 */
	public void downloadAndDecryptByDate(String clientKey, String downloadPath, InputStream privateKeyStream,
			String passphrase) throws Exception {

		SftpConfig config = sftpConfigRepository.findByClientKey(clientKey)
				.orElseThrow(() -> new RuntimeException("No SFTP config found for clientKey: " + clientKey));

		Session session = null;
		ChannelSftp sftpChannel = null;

		try {
			logger.info("Connecting to SFTP for clientKey: {}", clientKey);
			JSch jsch = new JSch();
			session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
			session.setPassword(config.getPassword());

			Properties configProps = new Properties();
			configProps.put("StrictHostKeyChecking", "no");
			session.setConfig(configProps);

			session.connect();
			logger.info("SFTP session connected for clientKey: {}", clientKey);

			Channel channel = session.openChannel("sftp");
			channel.connect();
			sftpChannel = (ChannelSftp) channel;

			sftpChannel.cd(config.getRemoteDirectory());
			logger.info("Changed to remote directory: {}", config.getRemoteDirectory());

			String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			logger.info("Looking for files containing date: {}", today);

			Vector<ChannelSftp.LsEntry> files = sftpChannel.ls(".");
			File downloadDir = new File(downloadPath);
			if (!downloadDir.exists()) {
				downloadDir.mkdirs();
			}

			boolean fileFound = false;

			for (ChannelSftp.LsEntry entry : files) {
				String fileName = entry.getFilename();
				if (fileName.contains(today)) {
					fileFound = true;
					File destinationFile = new File(downloadDir, fileName);
					try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
						sftpChannel.get(fileName, outputStream);
						logger.info("File downloaded: {} -> {}", fileName, destinationFile.getAbsolutePath());
					}
				}
			}

			if (!fileFound) {
				logger.warn("No files found containing date {} in directory {}", today, config.getRemoteDirectory());
			} else {
				// Call decryptor on downloaded folder
				logger.info("Starting decryption of downloaded files...");
				pgpDecryptor.decryptAllFiles(downloadDir, privateKeyStream, passphrase);
				logger.info("Decryption process completed for all files.");
			}

		} finally {
			if (sftpChannel != null && sftpChannel.isConnected()) {
				sftpChannel.exit();
				logger.info("SFTP channel exited.");
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
				logger.info("SFTP session disconnected.");
			}
		}
	}
}
