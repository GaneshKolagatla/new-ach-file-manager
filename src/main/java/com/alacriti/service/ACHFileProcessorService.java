package com.alacriti.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.alacriti.exceptions.ACHValidationException;
import com.alacriti.model.BatchHeaderDetails;
import com.alacriti.model.EntryDetails;
import com.alacriti.model.FileHeaderDetails;
import com.alacriti.model.FileSummaryDetails;
import com.alacriti.repo.BatchHeaderRepository;
import com.alacriti.repo.EntryDetailRepository;
import com.alacriti.repo.FileHeaderRepository;
import com.alacriti.repo.FileSummaryRepository;
import com.alacriti.util.ACHFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ACHFileProcessorService {

	private final FileHeaderRepository fileHeaderRepo;
	private final BatchHeaderRepository batchHeaderRepo;
	private final EntryDetailRepository entryDetailRepo;
	private final FileSummaryRepository fileSummaryRepo;
	private final BatchDataValidatorImpl validator; // inject validator

	public void processACHFile(File file) throws IOException, ACHValidationException {

		ACHFile achFile = parseACHFile(file, null);

		log.info("Starting ACH parsing for file: {} (size: {} bytes)", file.getAbsolutePath(), file.length());
		//validation stuff
		try {
			validator.validate(achFile);
			log.info("Validation successful for file: {}", file.getName());
		} catch (ACHValidationException ex) {
			log.error("Validation failed for file: {} -> {}", file.getName(), ex.getMessage());
			throw ex; // stop here, don’t insert into DB
		}
	}

	public ACHFile parseACHFile(File file, String clientKey) throws FileNotFoundException, IOException {
		String fileName = file.getName();
		LocalDateTime now = LocalDateTime.now();
		FileHeaderDetails currentFileHeader = null;
		BatchHeaderDetails currentBatchHeader = null;

		int lineNumber = 0;
		int fileHeaderCount = 0, batchHeaderCount = 0, entryDetailCount = 0;

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String rawLine;
			boolean fileSummarySaved = false;
			while ((rawLine = reader.readLine()) != null) {
				lineNumber++;
				if (rawLine.trim().isEmpty()) {
					log.debug("Line {} empty - skipping", lineNumber);
					continue;
				}

				String line = rawLine.length() < 94 ? String.format("%-94s", rawLine) : rawLine;
				char recordType = line.charAt(0);

				try {

					switch (recordType) {

					case '1': // File Header
						currentFileHeader = parseFileHeader(line, fileName, clientKey, now);
						currentFileHeader = fileHeaderRepo.save(currentFileHeader);
						fileHeaderCount++;
						log.info("Saved FileHeader (id={}) at line {}", currentFileHeader.getId(), lineNumber);
						break;

					case '5': // Batch Header
						if (currentFileHeader == null) {
							log.warn("Batch header before file header at line {}. Skipping.", lineNumber);
							break;
						}
						currentBatchHeader = parseBatchHeader(line, fileName, now);
						currentBatchHeader = batchHeaderRepo.save(currentBatchHeader);
						batchHeaderCount++;
						log.info("Saved BatchHeader (id={}) at line {}", currentBatchHeader.getId(), lineNumber);
						break;

					case '6': // Entry Detail
						EntryDetails entry = parseEntryDetail(line, currentBatchHeader, fileName, now);
						entryDetailRepo.save(entry);
						entryDetailCount++;
						log.info("Saved EntryDetail (id={}) at line {}", entry.getId(), lineNumber);
						break;
					case '9': // File Summary (File Control)
						if (!fileSummarySaved) {
							FileSummaryDetails fileSummary = new FileSummaryDetails();
							fileSummary.setRecordType(extractField(line, 0, 1, false));
							fileSummary.setBatchCount(stripLeadingZeros(extractField(line, 1, 7, false)));
							fileSummary.setBlockCount(extractField(line, 7, 13, false)); // keep block count as-is
							fileSummary.setEntryAddendaCount(extractField(line, 13, 21, false));
							fileSummary.setEntryHash(extractField(line, 21, 31, false));
							fileSummary.setTotalDebitEntryDollarAmount(
									stripLeadingZeros(extractField(line, 31, 43, false)));
							fileSummary.setTotalCreditEntryDollarAmount(
									stripLeadingZeros(extractField(line, 43, 55, false)));
							fileSummary.setReserved(extractField(line, 55, 94, false));
							fileSummary.setFileName(fileName);
							fileSummary.setCreatedAt(now);

							fileSummaryRepo.save(fileSummary);
							fileSummarySaved = true;

							log.info("Saved FileSummary (id={}) at line {}", fileSummary.getId(), lineNumber);
						} else {
							log.debug("Skipping padding line {} for record type 9", lineNumber);
						}
						break;

					default:
						log.debug("Line {} unknown record type '{}'", lineNumber, recordType);
					}

				} catch (Exception perLineEx) {
					log.error("Error parsing line {}: {} — line: '{}'", lineNumber, perLineEx.getMessage(), rawLine,
							perLineEx);
				}
			}
		}

		log.info("Parsing summary for {}: FileHeaders={}, BatchHeaders={}, EntryDetails={}", fileName, fileHeaderCount,
				batchHeaderCount, entryDetailCount);
		return null;
	}

	// === Helper Parsers ===
	private FileHeaderDetails parseFileHeader(String line, String fileName, String clientKey, LocalDateTime now) {
		FileHeaderDetails fileHeader = new FileHeaderDetails();
		fileHeader.setRecordType(extractField(line, 0, 1, true));
		fileHeader.setPriorityCode(extractField(line, 1, 3, true));
		fileHeader.setImmediateDestination(extractField(line, 3, 13, true));
		fileHeader.setImmediateOrigin(extractField(line, 13, 23, true));

		String dateStr = extractField(line, 23, 29, true);
		LocalDate creationDate = LocalDate
				.parse("20" + dateStr.substring(0, 2) + "-" + dateStr.substring(2, 4) + "-" + dateStr.substring(4, 6));
		fileHeader.setFileCreationDate(creationDate);

		String timeStr = extractField(line, 29, 33, false);
		fileHeader.setFileCreationTime(timeStr.isEmpty() ? null
				: LocalTime.of(Integer.parseInt(timeStr.substring(0, 2)), Integer.parseInt(timeStr.substring(2, 4))));

		fileHeader.setFileIdModifier(extractField(line, 33, 34, true));
		fileHeader.setRecordSize(extractField(line, 34, 37, true));
		fileHeader.setBlockingFactor(extractField(line, 37, 39, true));
		fileHeader.setFormatCode(extractField(line, 39, 40, true));
		fileHeader.setDestinationName(extractField(line, 40, 63, false));
		fileHeader.setOriginName(extractField(line, 63, 86, false));
		fileHeader.setReferenceCode(extractField(line, 86, 94, false));

		fileHeader.setClientKey(clientKey);
		fileHeader.setFileName(fileName);
		fileHeader.setCreatedAt(now);
		return fileHeader;
	}

	private BatchHeaderDetails parseBatchHeader(String line, String fileName, LocalDateTime now) {
		BatchHeaderDetails batch = new BatchHeaderDetails();
		batch.setRecordType(extractField(line, 0, 1, true));
		batch.setServiceClassCode(extractField(line, 1, 4, true));
		batch.setCompanyName(extractField(line, 4, 20, true));
		batch.setCompanyDiscretionaryData(extractField(line, 20, 40, false));
		batch.setCompanyIdentification(extractField(line, 40, 50, true));
		batch.setStandardEntryClassCode(extractField(line, 50, 53, true));
		batch.setCompanyEntryDescription(extractField(line, 53, 63, false));
		batch.setCompanyDescriptiveDate(extractField(line, 63, 69, false));

		String effDateStr = extractField(line, 69, 75, false);
		if (!effDateStr.isEmpty()) {
			batch.setEffectiveEntryDate(LocalDate.parse("20" + effDateStr.substring(0, 2) + "-"
					+ effDateStr.substring(2, 4) + "-" + effDateStr.substring(4, 6)));
		}

		batch.setSettlementDate(extractField(line, 75, 78, false));
		batch.setOriginatorStatusCode(extractField(line, 78, 79, false));
		batch.setOriginatingDFIIdentification(extractField(line, 79, 87, true));
		batch.setBatchNumber(extractField(line, 87, 94, true));
		batch.setFileName(fileName);
		batch.setCreatedAt(now);
		return batch;
	}

	private EntryDetails parseEntryDetail(String line, BatchHeaderDetails batch, String fileName, LocalDateTime now) {
		EntryDetails entry = new EntryDetails();
		if (batch != null) {
			entry.setBatchNumber(batch.getBatchNumber());
			entry.setOriginatingDfiId(batch.getOriginatingDFIIdentification());
		} else {
			entry.setBatchNumber("UNKNOWN"); // fallback if batch missing
			entry.setOriginatingDfiId(null);
		}

		entry.setRecordType(extractField(line, 0, 1, true));
		entry.setTransactionCode(extractField(line, 1, 3, true));
		entry.setRdfiRoutingNumber(extractField(line, 3, 11, true));
		entry.setCheckDigit(extractField(line, 11, 12, true));
		entry.setRdfiAccountNumber(extractField(line, 12, 29, true));

		String amtStr = extractField(line, 29, 39, false);
		try {
			entry.setAmount(amtStr.isEmpty() ? 0 : Integer.parseInt(amtStr));
		} catch (NumberFormatException nfe) {
			entry.setAmount(0);
		}

		entry.setIndividualIdNumber(extractField(line, 39, 54, false));
		entry.setIndividualName(extractField(line, 54, 76, false));
		entry.setDiscretionaryData(extractField(line, 76, 78, false));
		String addInd = extractField(line, 78, 79, false);
		entry.setAddendaRecordIndicator(addInd.isEmpty() ? 0 : Integer.parseInt(addInd));
		entry.setTraceNumber(extractField(line, 79, 94, true));
		entry.setReceivingDFI(entry.getRdfiRoutingNumber());
		entry.setFileName(fileName);
		entry.setCreatedAt(now);

		return entry;
	}

	private String extractField(String line, int start, int end, boolean required) {
		if (line.length() < end) {
			// Pad line if it's shorter than expected
			line = String.format("%-" + end + "s", line);
		}
		String val = line.substring(start, end).trim();
		// Return empty string if required or not, never throw exception
		return val;
	}

	//not used may be 
	@SuppressWarnings("unused")
	private String extractField(String line, int start, int end, boolean required, boolean optionalTrim) {
		String val = line.substring(start, end);
		if (required && val.trim().isEmpty()) {
			throw new IllegalArgumentException("Required field missing at pos " + start + "-" + end);
		}
		return optionalTrim ? val.trim() : val;
	}

	private String stripLeadingZeros(String value) {
		if (value == null)
			return "";
		String trimmed = value.trim().replaceFirst("^0+(?!$)", "");
		return trimmed.isEmpty() ? "0" : trimmed;
	}

}
