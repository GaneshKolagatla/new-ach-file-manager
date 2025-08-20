package com.alacriti.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

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

	public String generateAndEncryptAndSendACHFile(ACHFileRequest req) throws Exception {
		String sanitizedName = safe(req.getFinancialInstitutionName()).replaceAll("[^a-zA-Z0-9]", "");
		String routingNumber = req.getImmediateDestination();

		// format today's date as yyyyMMdd
		String todayDate = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		// gives like 20250819

		String rawFileName = sanitizedName + "" + routingNumber + "" + todayDate + ".ach";
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

			int batchEntryCount = 0;
			long batchDebit = 0;
			long batchCredit = 0;
			long batchEntryHash = 0;

			for (EntryDetailRequest entry : batch.getEntries()) {
				sb.append(buildEntryDetail(entry)).append("\n");
				totalEntryCount++;
				batchEntryCount++;

				String rdfi = safe(entry.getRdfiRoutingNumber());
				if (rdfi.length() < 8) {
					throw new IllegalArgumentException("Invalid RDFI routing number prefix: " + rdfi);
				}

				// Always use the first 8 digits for entry hash calculation
				totalEntryHash += parseLongSafe(rdfi.substring(0, 8));
				batchEntryHash += parseLongSafe(rdfi.substring(0, 8));

				if (isDebit(entry.getTransactionCode())) {
					totalDebit += entry.getAmount();
					batchDebit += entry.getAmount();
				} else if (isCredit(entry.getTransactionCode())) {
					totalCredit += entry.getAmount();
					batchCredit += entry.getAmount();
				}
			}

			sb.append(buildBatchControlRecord(batch, batchEntryCount, batchEntryHash, batchDebit, batchCredit))
					.append("\n");
		}

		// Debug totals
		System.out.println("-------- FILE CONTROL CALCULATION --------");
		System.out.println("Batch count     : " + batchCount);
		System.out.println("Entry/Addenda   : " + totalEntryCount);
		System.out.println("Entry hash raw  : " + totalEntryHash);
		System.out.println("Entry hash mod  : " + (totalEntryHash % 10000000000L));
		System.out.println("Total debit (¢) : " + totalDebit);
		System.out.println("Total credit(¢) : " + totalCredit);
		System.out.println("------------------------------------------");

		sb.append(buildFileControl(batchCount, totalEntryCount, totalEntryHash, totalDebit, totalCredit)).append("\n");

		// Pad to multiple of 10 records
		String[] lines = sb.toString().split("\n");
		int padLines = (10 - (lines.length % 10)) % 10;
		for (int i = 0; i < padLines; i++) {
			sb.append("9".repeat(94)).append("\n");
		}
		return sb.toString();
	}

	/* ---------- RECORD BUILDERS ---------- */

	private String buildFileHeader(ACHFileRequest req) {
		String header = String.format(
				"1" + "01" + "%s" + "%s" + "%s" + "%s" + "%c" + "094" + "10" + "1" + "%s" + "%s" + "%s",
				zeroPad(req.getImmediateDestination(), 10), zeroPad(req.getImmediateOrigin(), 10),
				new SimpleDateFormat("yyMMdd").format(new Date()), new SimpleDateFormat("HHmm").format(new Date()),
				generateRandomFileIdModifier().charAt(0), leftPad(req.getDestinationName(), 23),
				leftPad(req.getOriginName(), 23), leftPad("", 8));
		validateLength(header, "File Header");
		return header;
	}

	private String buildBatchHeader(BatchRequest b) {
		String header = String.format("5%03d%-16s%-20s%-10s%-3s%-10s%-6s%-6s   1%-8s%07d",
				parseIntSafe(b.getServiceClassCode()), truncateOrPad(b.getCompanyName(), 16),
				truncateOrPad(b.getCompanyDiscretionaryData(), 20), truncateOrPad(b.getCompanyIdentification(), 10),
				truncateOrPad(b.getStandardEntryClassCode(), 3), truncateOrPad(b.getCompanyEntryDescription(), 10),
				truncateOrPad(b.getCompanyDescriptiveDate(), 6), truncateOrPad(b.getEffectiveEntryDate(), 6),
				safeDfiId(b.getOriginatingDFIIdentification()), b.getBatchNumber());
		validateLength(header, "Batch Header");
		return header;
	}

	private String buildEntryDetail(EntryDetailRequest e) {
		String entry = String.format("6%-2s%08d%d%-17s%010d%-15s%-22s%-2s%d%015d", safe(e.getTransactionCode()),
				parseIntSafe(e.getRdfiRoutingNumber()), // first 8 digits
				parseIntSafe(e.getCheckDigit()), // check digit
				truncateOrPad(e.getDfiAccountNumber(), 17), e.getAmount(), truncateOrPad(e.getIndividualIdNumber(), 15),
				truncateOrPad(e.getIndividualName(), 22), truncateOrPad(e.getDiscretionaryData(), 2),
				e.getAddendaRecordIndicator(), parseLongSafe(e.getTraceNumber()));
		validateLength(entry, "Entry Detail");
		return entry;
	}

	private String buildBatchControlRecord(BatchRequest batch, int entryCount, long entryHash, long totalDebit,
			long totalCredit) {
		String reserved6 = " ".repeat(6);
		String control = String.format("8%03d%06d%010d%012d%012d%-10s%-19s%s%-8s%07d",
				parseIntSafe(batch.getServiceClassCode()), entryCount, entryHash % 10000000000L, totalDebit,
				totalCredit, truncateOrPad(batch.getCompanyIdentification(), 10), "", // 19-char message authentication code
				reserved6, safeDfiId(batch.getOriginatingDFIIdentification()), batch.getBatchNumber());
		validateLength(control, "Batch Control");
		return control;
	}

	private String buildFileControl(int batchCount, int totalEntryCount, long totalEntryHash, long totalDebit,
			long totalCredit) {
		int totalRecords = 1 + (batchCount * 2) + totalEntryCount + 1;
		int blockCount = (int) Math.ceil(totalRecords / 10.0);
		long entryHashTruncated = totalEntryHash % 10000000000L;

		String prefix = String.format("9%06d%06d%08d%010d%012d%012d", batchCount, blockCount, totalEntryCount,
				entryHashTruncated, totalDebit, totalCredit);

		String controlLine = prefix + " ".repeat(39);
		validateLength(controlLine, "File Control");
		return controlLine;
	}

	/* ---------- HELPERS ---------- */

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
		return String.format("%-" + length + "s", safe(val)).substring(0, length);
	}

	private String truncateOrPad(String val, int length) {
		return leftPad(safe(val).length() > length ? val.substring(0, length) : val, length);
	}

	private String generateRandomFileIdModifier() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		return String.valueOf(chars.charAt(new Random().nextInt(chars.length())));
	}

	private boolean isDebit(String code) {
		if (code == null)
			return false;
		switch (code) {
		// Regular debit codes
		case "27": // Checking debit
		case "37": // Savings debit
		case "47": // GL debit
		case "55": // Loan debit
			// Pre-note debit codes
		case "28": // Checking pre-note debit
		case "38": // Savings pre-note debit
		case "48": // GL pre-note debit
		case "58": // Loan pre-note debit
			return true;
		default:
			return false;
		}
	}

	private boolean isCredit(String code) {
		if (code == null)
			return false;
		switch (code) {
		// Regular credit codes
		case "22": // Checking credit
		case "32": // Savings credit
		case "42": // GL credit
		case "52": // Loan credit
			// Pre-note credit codes
		case "23": // Checking pre-note credit
		case "33": // Savings pre-note credit
		case "43": // GL pre-note credit
		case "53": // Loan pre-note credit
			return true;
		default:
			return false;
		}
	}
}
