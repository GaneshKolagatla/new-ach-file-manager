package com.alacriti.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.springframework.stereotype.Component;

@Component
public class PGPDecryptor {

    public void decryptFile(File encryptedFile, File outputFile, InputStream privateKeyStream, String passphrase) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Step 1: Decode the input file
        InputStream decoderStream = PGPUtil.getDecoderStream(new FileInputStream(encryptedFile));
        PGPObjectFactory pgpF = new PGPObjectFactory(decoderStream, new JcaKeyFingerprintCalculator());
        Object o = pgpF.nextObject();

        // Step 2: Get Encrypted Data List
        PGPEncryptedDataList encList;
        if (o instanceof PGPEncryptedDataList) {
            encList = (PGPEncryptedDataList) o;
        } else {
            encList = (PGPEncryptedDataList) pgpF.nextObject();
        }

        // Step 3: Load Private Key
        PGPPrivateKey privateKey = null;
        PGPPublicKeyEncryptedData encryptedData = null;

        PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(
                PGPUtil.getDecoderStream(privateKeyStream),
                new JcaKeyFingerprintCalculator()
        );

        Iterator<PGPEncryptedData> it = encList.getEncryptedDataObjects();
        while (it.hasNext()) {
            PGPPublicKeyEncryptedData pked = (PGPPublicKeyEncryptedData) it.next();
            PGPSecretKey secretKey = pgpSec.getSecretKey(pked.getKeyID());

            if (secretKey != null) {
                privateKey = secretKey.extractPrivateKey(
                        new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(passphrase.toCharArray())
                );
                encryptedData = pked;
                break;
            }
        }

        if (privateKey == null || encryptedData == null) {
            throw new IllegalArgumentException("Private key for decryption not found.");
        }

        // Step 4: Decrypt the stream
        InputStream clear = encryptedData.getDataStream(
                new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(privateKey)
        );

        // Step 5: Unwrap compressed or literal data
        PGPObjectFactory plainFact = new PGPObjectFactory(clear, new JcaKeyFingerprintCalculator());
        Object message = plainFact.nextObject();

        if (message instanceof PGPCompressedData) {
            PGPCompressedData compressedData = (PGPCompressedData) message;
            plainFact = new PGPObjectFactory(compressedData.getDataStream(), new JcaKeyFingerprintCalculator());
            message = plainFact.nextObject();
        }

        if (message instanceof PGPLiteralData) {
            PGPLiteralData literalData = (PGPLiteralData) message;
            InputStream literalStream = literalData.getInputStream();
            Files.copy(literalStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IllegalArgumentException("Unexpected PGP message format: " + message.getClass().getSimpleName());
        }

        // Step 6: Integrity check (optional)
        if (encryptedData.isIntegrityProtected() && !encryptedData.verify()) {
            throw new SecurityException("PGP message failed integrity check.");
        }
    }
}
