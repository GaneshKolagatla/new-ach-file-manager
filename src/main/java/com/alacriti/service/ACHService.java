package com.alacriti.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import com.alacriti.model.BatchHeader;
import com.alacriti.model.EntryDetail;
import com.alacriti.model.FileHeader;
import com.alacriti.repo.BatchHeaderRepository;
import com.alacriti.repo.EntryDetailRepository;
import com.alacriti.repo.FileHeaderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ACHService {

	@Autowired
	private PGPEncryptionService encryptionService;

	@Autowired
	private SftpUploadService sftpUploadService;

	@Autowired
	private final FileHeaderRepository fileHeaderRepo;
	@Autowired
	private final BatchHeaderRepository batchHeaderRepo;
	@Autowired
	private final EntryDetailRepository entryDetailRepo;

	public String generateAndEncryptAndSendACHFile(ACHFileRequest request) throws Exception {
		LocalDateTime now = LocalDateTime.now();
		String date = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
		String time = now.format(DateTimeFormatter.ofPattern("HHmm"));

		String sanitizedName = request.getFinancialInstitutionName().replaceAll("[^a-zA-Z0-9]", "");
		String rawFileName = sanitizedName + "_" + request.getImmediateDestination() + "_" + System.currentTimeMillis()
				+ ".ach";
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
		return String.format("101%10s%10s%s%sA094101%-23s%-23s%8s", request.getImmediateDestination(),
				request.getImmediateOrigin(), date, time, request.getDestinationName(), request.getOriginName(),
				"094101" // placeholder for reference code
		);
	}

	private String buildBatchHeader(BatchRequest batch, int batchNumber) {
		return String.format("5%s%-16s%-20s%s%-10s%s   %-8s%06d", batch.getServiceClassCode(), batch.getCompanyName(),
				batch.getCompanyIdentification(), batch.getStandardEntryClassCode(), batch.getCompanyEntryDescription(),
				batch.getEffectiveEntryDate(), batch.getOriginatingDFIIdentification(), batchNumber);
	}

	private String buildEntryDetail(EntryDetailRequest entry) {
		return String.format("6%s%s%-17s%010d%-15s%-22s%s", entry.getTransactionCode(),
				entry.getReceivingDFIRoutingNumber(), entry.getDfiaAccountNumber(), entry.getAmount(),
				entry.getIndividualIdNumber(), entry.getIndividualName(), entry.getTraceNumber());
	}

	private String buildBatchControl(BatchRequest batch, int entryCount, long debit, long credit) {
		return String.format("8%s%06d%010d%010d%-10s%-10s%06d", batch.getServiceClassCode(), entryCount, debit, credit,
				"", batch.getCompanyIdentification(), batch.getBatchNumber());
	}

	private String buildFileControl(int batchCount, int totalEntries, long totalDebit, long totalCredit) {
		return String.format("9%06d%06d%010d%010d%-39s", batchCount, totalEntries, totalDebit, totalCredit, "");
	}

	@Transactional
	public void processACHFile(File file) throws IOException {
		String fileName = file.getName();
		LocalDateTime now = LocalDateTime.now();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					log.warn("Empty line found. Inserting placeholder record.");
					continue; // or insert a separate audit record if needed
				}

				char recordType = line.charAt(0);

				switch (recordType) {
				case '1': // File Header
					FileHeader fh = new FileHeader(null, "1", safeSubstring(line, 1, 3), safeSubstring(line, 3, 13),
							safeSubstring(line, 13, 23), safeSubstring(line, 23, 29), safeSubstring(line, 29, 33),
							safeSubstring(line, 33, 34), safeSubstring(line, 34, 37), safeSubstring(line, 37, 39),
							safeSubstring(line, 39, 40), safeSubstring(line, 40, 63), safeSubstring(line, 63, 86),
							safeSubstring(line, 86, 94), fileName, now);
					fileHeaderRepo.save(fh);
					break;

				case '5': // Batch Header
					BatchHeader bh = new BatchHeader(null, "5", safeSubstring(line, 1, 4), safeSubstring(line, 4, 20),
							safeSubstring(line, 20, 40), safeSubstring(line, 40, 50), safeSubstring(line, 50, 53),
							safeSubstring(line, 53, 63), safeSubstring(line, 63, 69), safeSubstring(line, 69, 75),
							safeSubstring(line, 75, 78), safeSubstring(line, 78, 79), safeSubstring(line, 79, 87),
							safeSubstring(line, 87, 94), fileName, now);
					batchHeaderRepo.save(bh);
					break;

				case '6': // Entry Detail
					EntryDetail ed = new EntryDetail(null, "6", safeSubstring(line, 1, 3), safeSubstring(line, 3, 11),
							safeSubstring(line, 11, 12), safeSubstring(line, 12, 29), safeSubstring(line, 29, 39),
							safeSubstring(line, 39, 54), safeSubstring(line, 54, 76), safeSubstring(line, 76, 78),
							safeSubstring(line, 78, 79), safeSubstring(line, 79, 94), fileName, now);
					entryDetailRepo.save(ed);
					break;

				default:
					log.warn("Skipped unknown record type: {}", recordType);
				}
			}
		}
	}

	// ✅ Add helper method here
	private String safeSubstring(String line, int start, int end) {
		if (line.length() >= end) {
			return line.substring(start, end);
		} else if (line.length() > start) {
			return line.substring(start);
		} else {
			return "";
		}
	}

}
