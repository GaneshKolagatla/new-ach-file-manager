package com.alacriti.service;

import java.io.File;
import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.alacriti.component.PGPEncryptor;

@Service
public class PGPEncryptionService {


	    public File encryptACHFile(File inputFile, File outputFile, InputStream publicKeyStream) throws Exception {
	        // Encrypts inputFile using the given public key stream and writes to outputFile
	        PGPEncryptor.encryptFile(inputFile, outputFile, publicKeyStream);
	        return outputFile;
	    }
	}

