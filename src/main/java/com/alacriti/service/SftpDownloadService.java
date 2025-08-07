package com.alacriti.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	public void downloadFile(String clientKey, String fileName, File destinationFile) throws Exception {
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

			try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
				sftpChannel.get(fileName, outputStream);
				logger.info("File downloaded: {} to {}", fileName, destinationFile.getAbsolutePath());
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
