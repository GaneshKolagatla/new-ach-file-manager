package com.alacriti.service;

import com.alacriti.dto.ACHFileRequest;
import com.alacriti.dto.BatchRequest;
import com.alacriti.dto.EntryDetailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Service
public class ACHService {

    @Autowired
    private PGPEncryptionService encryptionService;

    @Autowired
    private SftpUploadService sftpUploadService;

    public String generateAndEncryptAndSendACHFile(ACHFileRequest req) throws Exception {
        String sanitizedName = safe(req.getFinancialInstitutionName()).replaceAll("[^a-zA-Z0-9]", "");
        String rawFileName = sanitizedName + "_" + req.getImmediateDestination() + "_" + System.currentTimeMillis() + ".ach";
        String encryptedFileName = rawFileName + ".pgp";

        Files.createDirectories(Paths.get("target", "ach-files"));
        File rawFile = Paths.get("target", "ach-files", rawFileName).toFile();

        String achContent = buildACHFileContent(req);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rawFile))) {
            writer.write(achContent);
        }

        File encryptedFile = Paths.get("target", "ach-files", encryptedFileName).toFile();
        File publicKeyFile = new File("src/main/resources/keys/vatsalya_public.asc");

        if (!publicKeyFile.exists()) {
            throw new RuntimeException("PGP public key not found for clientKey: " + req.getClientKey());
        }

        try (InputStream publicKeyStream = new FileInputStream(publicKeyFile)) {
            encryptionService.encryptACHFile(rawFile, encryptedFile, publicKeyStream);
        }

        sftpUploadService.uploadFile(req.getClientKey(), encryptedFile);
        return encryptedFileName;
    }

    // ---------------------- Build ACH file ----------------------

    private String buildACHFileContent(ACHFileRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildFileHeader(req)).append("\n");

        int batchCount = 0;
        int totalEntryCount = 0;
        long totalDebit = 0;
        long totalCredit = 0;
        long totalEntryHash = 0;

        for (BatchRequest batch : req.getBatches()) {
            batchCount++;
            sb.append(buildBatchHeader(batch)).append("\n");

            for (EntryDetailRequest entry : batch.getEntries()) {
                sb.append(buildEntryDetail(entry)).append("\n");
                totalEntryCount++;
                totalEntryHash += parseLongSafe(entry.getRdfiRoutingNumber());

                if (isDebit(entry.getTransactionCode())) {
                    totalDebit += entry.getAmount();
                } else if (isCredit(entry.getTransactionCode())) {
                    totalCredit += entry.getAmount();
                }
            }

            sb.append(buildBatchControl(batch)).append("\n");
        }

        sb.append(buildFileControl(batchCount, totalEntryCount, totalEntryHash, totalDebit, totalCredit)).append("\n");

        // Padding to multiple of 10 lines
        String[] lines = sb.toString().split("\n");
        int padLines = (10 - (lines.length % 10)) % 10;
        for (int i = 0; i < padLines; i++) {
            sb.append("9".repeat(94)).append("\n");
        }

        return sb.toString();
    }

    // ---------------------- Record Builders ----------------------

    private String buildFileHeader(ACHFileRequest req) {
        String header = String.format(
                "1%02d%s%s%s%s%s%03d%02d%d%-23s%-23s%-8s",
                1,
                zeroPad(req.getImmediateDestination(), 10),
                zeroPad(req.getImmediateOrigin(), 10),
                new SimpleDateFormat("yyMMdd").format(new Date()),
                new SimpleDateFormat("HHmm").format(new Date()),
                generateRandomFileIdModifier(),
                94,
                10,
                1,
                leftPad(req.getDestinationName(), 23),
                leftPad(req.getOriginName(), 23),
                ""
        );
        validateLength(header, "File Header");
        return header;
    }

    private String buildBatchHeader(BatchRequest b) {
        String header = String.format(
                "5%03d%-16s%-20s%-10s%-3s%-10s%-6s%-6s   1%-8s%07d",
                parseIntSafe(b.getServiceClassCode()),
                truncateOrPad(b.getCompanyName(), 16),
                truncateOrPad(b.getCompanyDiscretionaryData(), 20),
                truncateOrPad(b.getCompanyIdentification(), 10),
                truncateOrPad(b.getStandardEntryClassCode(), 3),
                truncateOrPad(b.getCompanyEntryDescription(), 10),
                truncateOrPad(b.getCompanyDescriptiveDate(), 6),
                truncateOrPad(b.getEffectiveEntryDate(), 6),
                safeDfiId(b.getOriginatingDFIIdentification()),
                b.getBatchNumber()
        );
        validateLength(header, "Batch Header");
        return header;
    }

    private String buildEntryDetail(EntryDetailRequest e) {
        String entry = String.format(
                "6%-2s%08d%1d%-17s%010d%-15s%-22s%-2s%d%015d",
                safe(e.getTransactionCode()),
                parseIntSafe(e.getRdfiRoutingNumber()),
                parseIntSafe(e.getCheckDigit()),
                safe(e.getDfiAccountNumber()),
                e.getAmount(),
                safe(e.getIndividualIdNumber()),
                safe(e.getIndividualName()),
                safe(e.getDiscretionaryData()),
                e.getAddendaRecordIndicator(),
                parseLongSafe(e.getTraceNumber())
        );
        validateLength(entry, "Entry Detail");
        return entry;
    }

    private String buildBatchControl(BatchRequest batch) {
        int entryCount = batch.getEntries().size();
        long entryHash = batch.getEntries().stream()
                .mapToLong(e -> parseLongSafe(e.getRdfiRoutingNumber().substring(0, 8)))
                .sum();
        long totalDebit = batch.getEntries().stream()
                .filter(e -> isDebit(e.getTransactionCode()))
                .mapToLong(EntryDetailRequest::getAmount)
                .sum();
        long totalCredit = batch.getEntries().stream()
                .filter(e -> isCredit(e.getTransactionCode()))
                .mapToLong(EntryDetailRequest::getAmount)
                .sum();

        String control = String.format(
                "8%03d%06d%010d%012d%012d%-10s%-19s%-6s%-8s%07d",
                parseIntSafe(batch.getServiceClassCode()),
                entryCount,
                entryHash % 10000000000L,
                totalDebit,
                totalCredit,
                truncateOrPad(batch.getCompanyIdentification(), 10),
                "",
                "",
                safeDfiId(batch.getOriginatingDFIIdentification()),
                batch.getBatchNumber()
        );
        validateLength(control, "Batch Control");
        return control;
    }
    		//filecontrol
    
    private String buildFileControl(int batchCount, int totalEntryCount, long entryHash, long totalDebit, long totalCredit) {
        int totalRecords = 1 + batchCount * 2 + totalEntryCount + 1;  // Header + batch headers/batch controls + entries + file control
        int blockCount = (int) Math.ceil(totalRecords / 10.0);

        // Format fixed-length numeric fields with zero-padding
        String prefix = String.format(
            "9%06d%06d%010d%012d%012d",
            batchCount,
            blockCount,
            entryHash % 10000000000L,
            totalDebit,
            totalCredit
        );

        // Pad the remaining length with spaces to total 94
        // Total length so far
        int lenSoFar = prefix.length();
        int padLength = 94 - lenSoFar;
        if (padLength < 0) {
            throw new IllegalStateException("File Control prefix too long: " + lenSoFar + " chars");
        }
        String reserved = " ".repeat(padLength);

        String controlLine = prefix + reserved;

        if (controlLine.length() != 94) {
            throw new IllegalStateException("File Control length must be 94 but was " + controlLine.length() + " [" + controlLine + "]");
        }

        return controlLine;
    }

    
     


    // ---------------------- Utility Methods ----------------------

    private void validateLength(String s, String label) {
        if (s.length() != 94) {
            throw new IllegalStateException(label + " length must be 94 but was " + s.length() + " [" + s + "]");
        }
    }

    private String safe(String val) {
        return val == null ? "" : val;
    }

    private int parseIntSafe(String val) {
        try {
            return Integer.parseInt(safe(val).replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLongSafe(String val) {
        try {
            return Long.parseLong(safe(val).replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0L;
        }
    }

    private String safeDfiId(String val) {
        return String.format("%-8s", safe(val)).substring(0, 8);
    }

    private String zeroPad(String val, int length) {
        return String.format("%" + length + "s", safe(val)).replace(' ', '0');
    }

    private String leftPad(String val, int length) {
        return String.format("%-" + length + "s", safe(val));
    }

    private String truncateOrPad(String val, int length) {
        return leftPad(safe(val).length() > length ? val.substring(0, length) : val, length);
    }

    private String generateRandomFileIdModifier() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return String.valueOf(chars.charAt(new Random().nextInt(chars.length())));
    }

    private boolean isDebit(String code) {
        return "27".equals(code) || "37".equals(code);
    }

    private boolean isCredit(String code) {
        return "22".equals(code) || "32".equals(code);
    }
}
