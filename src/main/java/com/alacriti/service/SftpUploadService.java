package com.alacriti.service;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alacriti.model.SftpConfig;
import com.alacriti.repo.SftpConfigRepo;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Service
public class SftpUploadService {

    @Autowired
    private SftpConfigRepo sftpConfigRepository;

    public void uploadFile(String clientKey, File fileToUpload) throws Exception {
        SftpConfig config = sftpConfigRepository.findByClientKey(clientKey)
                .orElseThrow(() -> new RuntimeException("No SFTP config found for clientKey: " + clientKey));

        JSch jsch = new JSch();
        Session session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
        session.setPassword(config.getPassword());

        java.util.Properties configProps = new java.util.Properties();
        configProps.put("StrictHostKeyChecking", "no");
        session.setConfig(configProps);

        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp sftpChannel = (ChannelSftp) channel;
        sftpChannel.cd(config.getRemoteDirectory());

        try (FileInputStream fis = new FileInputStream(fileToUpload)) {
            sftpChannel.put(fis, fileToUpload.getName());
        }

        sftpChannel.exit();
        session.disconnect();
    }
}
