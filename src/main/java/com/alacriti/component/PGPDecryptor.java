package com.alacriti.component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;

public class PGPDecryptor {

	public void decryptSingleFile(File encryptedFile, File outputFile, InputStream privateKeyStream, String passphrase)
			throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		try (InputStream encryptedData = new BufferedInputStream(new FileInputStream(encryptedFile))) {
			InputStream decoderStream = PGPUtil.getDecoderStream(encryptedData);

			JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(decoderStream);
			Object object = pgpFactory.nextObject();

			if (!(object instanceof PGPEncryptedDataList)) {
				object = pgpFactory.nextObject(); // skip marker
			}

			PGPEncryptedDataList encList = (PGPEncryptedDataList) object;
			Iterator<PGPEncryptedData> it = encList.getEncryptedDataObjects();

			// Load private key
			PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
					PGPUtil.getDecoderStream(privateKeyStream), new JcaKeyFingerprintCalculator());

			PGPPrivateKey privateKey = null;
			PGPPublicKeyEncryptedData encData = null;

			while (it.hasNext() && privateKey == null) {
				encData = (PGPPublicKeyEncryptedData) it.next();
				PGPSecretKey secretKey = pgpSec.getSecretKey(encData.getKeyID());

				if (secretKey != null) {
					privateKey = secretKey.extractPrivateKey(
							new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray()));
				}
			}

			if (privateKey == null) {
				throw new IllegalArgumentException("No private key found for decryption!");
			}

			InputStream clearData = encData
					.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey));

			JcaPGPObjectFactory plainFactory = new JcaPGPObjectFactory(clearData);
			Object message = plainFactory.nextObject();

			if (message instanceof PGPCompressedData) {
				PGPCompressedData compressedData = (PGPCompressedData) message;
				JcaPGPObjectFactory compressedFactory = new JcaPGPObjectFactory(compressedData.getDataStream());
				message = compressedFactory.nextObject();
			}

			if (message instanceof PGPLiteralData) {
				PGPLiteralData literalData = (PGPLiteralData) message;
				try (InputStream literalStream = literalData.getInputStream();
						OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
					literalStream.transferTo(out);
				}
			} else {
				throw new PGPException("Decryption failed: unexpected message type " + message.getClass().getName());
			}

			if (outputFile.length() == 0) {
				throw new java.io.IOException("Decryption completed but file is empty!");
			}
		}
	}
}
