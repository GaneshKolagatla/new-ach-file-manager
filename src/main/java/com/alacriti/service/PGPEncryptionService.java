package com.alacriti.service;

import java.io.File;
import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.alacriti.component.PGPDecryptor;
import com.alacriti.component.PGPEncryptor;

@Service
public class PGPEncryptionService {

    public File encryptACHFile(File inputFile, File outputFile, InputStream publicKeyStream) throws Exception {
        // Encrypt the file using the public key
        PGPEncryptor.encryptFile(inputFile, outputFile, publicKeyStream);
        return outputFile;
    }

    public File decryptACHFile(File encryptedFile, File outputFile, InputStream privateKeyStream, String passphrase) throws Exception {
        // Decrypt the file using the private key and passphrase
        PGPDecryptor decryptor = new PGPDecryptor();
        decryptor.decryptFile(encryptedFile, outputFile, privateKeyStream, passphrase);
        return outputFile;
    }
}
