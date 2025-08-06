package com.alacriti.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alacriti.dto.ACHFileRequest;
import com.alacriti.dto.BatchRequest;
import com.alacriti.dto.EntryDetailRequest;

@Service
public class ACHService {

    @Autowired
    private PGPEncryptionService encryptionService;

    @Autowired
    private SftpUploadService sftpUploadService;

    public String generateAndEncryptAndSendACHFile(ACHFileRequest request) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmm"));

        String sanitizedName = request.getFinancialInstitutionName().replaceAll("[^a-zA-Z0-9]", "");
        String rawFileName = sanitizedName + "_" + request.getImmediateDestination() + "_" + System.currentTimeMillis() + ".ach";
        String encryptedFileName = rawFileName + ".pgp";

        // Create directory if not exists
        Files.createDirectories(Paths.get("target", "ach-files"));

        File rawFile = Paths.get("target", "ach-files", rawFileName).toFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(rawFile));

        int blockCount = 0;
        int totalEntryAddendaCount = 0;
        long totalDebitAmount = 0;
        long totalCreditAmount = 0;

        // File Header (1)
        writer.write(buildFileHeader(request, date, time));
        writer.newLine();
        blockCount++;

        int batchCount = 0;

        for (BatchRequest batch : request.getBatches()) {
            batchCount++;
            int entryCount = 0;
            long batchDebit = 0;
            long batchCredit = 0;

            // Batch Header (5)
            writer.write(buildBatchHeader(batch, batch.getBatchNumber()));
            writer.newLine();
            blockCount++;

            for (EntryDetailRequest entry : batch.getEntries()) {
                writer.write(buildEntryDetail(entry));
                writer.newLine();
                blockCount++;
                entryCount++;

                if (entry.getTransactionCode().startsWith("2")) {
                    totalCreditAmount += entry.getAmount();
                    batchCredit += entry.getAmount();
                } else {
                    totalDebitAmount += entry.getAmount();
                    batchDebit += entry.getAmount();
                }
            }

            totalEntryAddendaCount += entryCount;

            // Batch Control (8)
            writer.write(buildBatchControl(batch, entryCount, batchDebit, batchCredit));
            writer.newLine();
            blockCount++;
        }

        // File Control (9)
        writer.write(buildFileControl(batchCount, totalEntryAddendaCount, totalDebitAmount, totalCreditAmount));
        writer.newLine();
        blockCount++;

        // Padding
        int padLines = (10 - (blockCount % 10)) % 10;
        for (int i = 0; i < padLines; i++) {
            writer.write("9".repeat(94));
            writer.newLine();
        }

        writer.close();

        // Encrypt the file
        File encryptedFile = Paths.get("target", "ach-files", encryptedFileName).toFile();

        // Load public key from resources or known path
        File publicKeyFile = new File("src/main/resources/keys/vatsalya_public.asc");
        if (!publicKeyFile.exists()) {
            throw new RuntimeException("Public key not found for clientKey: " + request.getClientKey());
        }

        try (InputStream publicKeyStream = new FileInputStream(publicKeyFile)) {
            encryptionService.encryptACHFile(rawFile, encryptedFile, publicKeyStream);
        }

        // Upload encrypted file to SFTP
        sftpUploadService.uploadFile(request.getClientKey(), encryptedFile);

        return encryptedFileName;
    }

    private String buildFileHeader(ACHFileRequest request, String date, String time) {
        return String.format("101%10s%10s%s%sA094101%-23s%-23s%8s",
                request.getImmediateDestination(),
                request.getImmediateOrigin(),
                date,
                time,
                request.getDestinationName(),
                request.getOriginName(),
                "094101" // placeholder for reference code
        );
    }

    private String buildBatchHeader(BatchRequest batch, int batchNumber) {
        return String.format("5%s%-16s%-20s%s%-10s%s   %-8s%06d",
                batch.getServiceClassCode(),
                batch.getCompanyName(),
                batch.getCompanyIdentification(),
                batch.getStandardEntryClassCode(),
                batch.getCompanyEntryDescription(),
                batch.getEffectiveEntryDate(),
                batch.getOriginatingDFIIdentification(),
                batchNumber
        );
    }

    private String buildEntryDetail(EntryDetailRequest entry) {
        return String.format("6%s%s%-17s%010d%-15s%-22s%s",
                entry.getTransactionCode(),
                entry.getReceivingDFIRoutingNumber(),
                entry.getDfiaAccountNumber(),
                entry.getAmount(),
                entry.getIndividualIdNumber(),
                entry.getIndividualName(),
                entry.getTraceNumber()
        );
    }

    private String buildBatchControl(BatchRequest batch, int entryCount, long debit, long credit) {
        return String.format("8%s%06d%010d%010d%-10s%-10s%06d",
                batch.getServiceClassCode(),
                entryCount,
                debit,
                credit,
                "",
                batch.getCompanyIdentification(),
                batch.getBatchNumber()
        );
    }

    private String buildFileControl(int batchCount, int totalEntries, long totalDebit, long totalCredit) {
        return String.format("9%06d%06d%010d%010d%-39s",
                batchCount,
                totalEntries,
                totalDebit,
                totalCredit,
                ""
        );
    }
}
