package com.alacriti.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.alacriti.util.ACHFile;
import com.alacriti.util.Batch;
import com.alacriti.util.BatchControl;
import com.alacriti.util.BatchHeader;
import com.alacriti.util.EntryDetail;
import com.alacriti.util.FileControl;
import com.alacriti.util.FileHeader;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@Slf4j
public class BatchDataReaderImpl implements IBatchDataReader {

	public ACHFile read(String filePath) throws Exception {
		List<String> lines = Files.readAllLines(Paths.get(filePath));
		ACHFile achFile = new ACHFile();
		List<Batch> batches = new ArrayList<>();
		Batch currentBatch = null;

		for (String line : lines) {
			if (line.length() < 94) {
				log.warn("Skipping invalid line (length < 94): {}", line);
				continue;
			}

			char type = line.charAt(0);
			switch (type) {
			case '1':
				achFile.setFileHeader(parseFileHeader(line));
				log.info("File header parsed successfully");
				break;

			case '5':
				currentBatch = new Batch();
				try {
					currentBatch.setBatchHeader(parseBatchHeader(line));
					currentBatch.setEntryDetails(new ArrayList<>());
					log.info("✅ Created batch with SEC code: {}",
							currentBatch.getBatchHeader().getStandardEntryClassCode());
				} catch (Exception e) {
					log.error("❌ Error parsing batch header: {}", e.getMessage(), e);
					// Create a minimal batch header so the batch isn't lost
					BatchHeader errorHeader = new BatchHeader();
					errorHeader.setStandardEntryClassCode("INVALID");
					errorHeader.setRecordTypeCode("5");
					currentBatch.setBatchHeader(errorHeader);
					currentBatch.setEntryDetails(new ArrayList<>());
				}
				break;

			case '6':
				if (currentBatch != null) {
					currentBatch.getEntryDetails().add(parseEntryDetail(line));
					log.debug("Added entry detail to current batch");
				} else {
					log.warn("Entry detail found without active batch: {}", line);
				}
				break;

			case '8':
				if (currentBatch != null) {
					currentBatch.setBatchControl(parseBatchControl(line));
					batches.add(currentBatch);
					log.info("✅ Added batch to file - Total batches: {}", batches.size());
					currentBatch = null;
				} else {
					log.warn("Batch control found without active batch: {}", line);
				}
				break;

			case '9':
				achFile.setFileControl(parseFileControl(line));
				log.info("✅ File control parsed successfully");
				break;

			default:
				log.warn("Unknown record type '{}' in line: {}", type, line);
			}
		}

		achFile.setBatches(batches);
		log.info("📊 Total batches parsed: {}", batches.size());
		return achFile;
	}

	private FileHeader parseFileHeader(String l) {
		FileHeader h = new FileHeader();
		h.setRecordTypeCode(l.substring(0, 1));
		h.setPriorityCode(l.substring(1, 3));
		h.setImmediateDestination(l.substring(3, 13));
		h.setImmediateOrigin(l.substring(13, 23));
		h.setFileCreationDate(l.substring(23, 29));
		h.setFileCreationTime(l.substring(29, 33));
		h.setFileIdModifier(l.substring(33, 34));
		h.setRecordSize(l.substring(34, 37));
		h.setBlockingFactor(l.substring(37, 39));
		h.setFormatCode(l.substring(39, 40));
		h.setImmediateDestinationName(l.substring(40, 63).trim());
		h.setImmediateOriginName(l.substring(63, 86).trim());
		h.setReferenceCode(l.substring(86, 94));
		return h;
	}

	private BatchHeader parseBatchHeader(String l) {
		log.debug("Parsing batch header line: {}", l);

		BatchHeader h = new BatchHeader();
		h.setRecordTypeCode(l.substring(0, 1));
		h.setServiceClassCode(l.substring(1, 4));
		h.setCompanyName(l.substring(4, 20).trim());
		h.setCompanyDiscretionaryData(l.substring(20, 40).trim());
		h.setCompanyIdentification(l.substring(40, 50).trim());

		String secCode = l.substring(50, 53).trim();
		log.debug("Parsed SEC code: '{}'", secCode);
		h.setStandardEntryClassCode(secCode);

		h.setCompanyEntryDescription(l.substring(53, 63).trim());
		h.setCompanyDescriptiveDate(l.substring(63, 69));
		h.setEffectiveEntryDate(l.substring(69, 75));
		h.setSettlementDate(l.substring(75, 78));
		h.setOriginatorStatusCode(l.substring(78, 79));
		h.setOriginatingDFI(l.substring(79, 87));
		h.setBatchNumber(l.substring(87, 94));

		log.debug("Batch header created successfully with SEC: {}", h.getStandardEntryClassCode());
		return h;
	}

	private EntryDetail parseEntryDetail(String l) {
		EntryDetail e = new EntryDetail();
		e.setRecordTypeCode(l.substring(0, 1));
		e.setTransactionCode(l.substring(1, 3));
		e.setReceivingDFIIdentification(l.substring(3, 11));
		e.setCheckDigit(l.substring(11, 12));
		e.setDfiAccountNumber(l.substring(12, 29).trim());
		e.setAmount(l.substring(29, 39));
		e.setIndividualIdentificationNumber(l.substring(39, 54).trim());
		e.setIndividualName(l.substring(54, 76).trim());
		e.setDiscretionaryData(l.substring(76, 78));
		e.setAddendaRecordIndicator(l.substring(78, 79));
		e.setTraceNumber(l.substring(79, 94));
		return e;
	}

	private BatchControl parseBatchControl(String l) {
		BatchControl c = new BatchControl();
		c.setRecordTypeCode(l.substring(0, 1));
		c.setServiceClassCode(l.substring(1, 4));
		c.setEntryAddendaCount(l.substring(4, 10));
		c.setEntryHash(l.substring(10, 20));
		c.setTotalDebitEntryDollarAmount(l.substring(20, 32));
		c.setTotalCreditEntryDollarAmount(l.substring(32, 44));
		c.setCompanyIdentification(l.substring(44, 54));
		c.setMessageAuthenticationCode(l.substring(54, 73));
		c.setReserved(l.substring(73, 79));
		c.setOriginatingDFI(l.substring(79, 87));
		c.setBatchNumber(l.substring(87, 94));
		return c;
	}

	private FileControl parseFileControl(String l) {
		FileControl c = new FileControl();
		c.setRecordTypeCode(l.substring(0, 1));
		c.setBatchCount(l.substring(1, 7));
		c.setBlockCount(l.substring(7, 13));
		c.setEntryAddendaCount(l.substring(13, 21));
		c.setEntryHash(l.substring(21, 31));
		c.setTotalDebitEntryDollarAmount(l.substring(31, 43));
		c.setTotalCreditEntryDollarAmount(l.substring(43, 55));
		c.setReserved(l.substring(55, 94));
		return c;
	}
}
