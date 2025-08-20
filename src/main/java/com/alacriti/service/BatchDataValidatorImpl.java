package com.alacriti.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.alacriti.exceptions.ACHValidationException;
import com.alacriti.exceptions.InvalidRoutingNumberException;
import com.alacriti.exceptions.InvalidSECCodeException;
import com.alacriti.exceptions.InvalidTransactionCodeException;
import com.alacriti.util.ACHFile;
import com.alacriti.util.Batch;
import com.alacriti.util.BatchHeader;
import com.alacriti.util.EntryDetail;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Data
@Slf4j
public class BatchDataValidatorImpl implements IBatchDataValidator {
	private static final Set<String> VALID_TRANSACTION_CODES = Set.of("22", "23", "24", "27", "28", "29", "32", "33",
			"34", "37", "38", "39");
	private static final Set<String> VALID_SEC_CODES = Set.of("PPD", "CCD", "CTX", "WEB", "TEL", "POP", "ARC", "BOC",
			"RCK");

	@Override
	public void validate(ACHFile file) throws ACHValidationException {
		log.info("🔍 Starting ACH file validation...");

		if (file == null) {
			log.error("Validation failed: ACHFile is null");
			throw new ACHValidationException("ACHFile is null");
		}
		if (file.getFileHeader() == null) {
			log.error("Validation failed: File header missing");
			throw new ACHValidationException("File header missing");
		}
		if (!"1".equals(file.getFileHeader().getRecordTypeCode())) {
			log.error("Invalid FileHeader recordTypeCode: {}", file.getFileHeader().getRecordTypeCode());
			throw new ACHValidationException("Bad fileHeader recordType", "FILE_HEADER", "recordTypeCode", 1);
		}
		if (!"01".equals(file.getFileHeader().getPriorityCode())) {
			log.error("Invalid FileHeader priorityCode: {}", file.getFileHeader().getPriorityCode());
			throw new ACHValidationException("Bad priority code", "FILE_HEADER", "priorityCode", 1);
		}

		log.info("✅ File header validation passed");

		if (file.getBatches() == null || file.getBatches().isEmpty()) {
			log.error("Validation failed: No batches found in ACH file");
			throw new ACHValidationException("No batches found in ACH file");
		}

		log.info("Found {} batches to validate", file.getBatches().size());

		int batchNum = 1;
		for (Batch batch : file.getBatches()) {
			BatchHeader header = batch.getBatchHeader();
			log.debug("Validating batch {} with SEC code: {}", batchNum, header.getStandardEntryClassCode());

			validateBatchHeader(header, batchNum);
			log.info("Batch {} header validation passed", batchNum);

			int entryNum = 1;
			for (EntryDetail entry : batch.getEntryDetails()) {
				log.debug("Validating entry {} of batch {}", entryNum, batchNum);
				validateEntryDetail(entry, entryNum);
				entryNum++;
			}
			log.info("Batch {} entries validation passed ({} entries)", batchNum, batch.getEntryDetails().size());
			batchNum++;
		}

		if (file.getFileControl() == null) {
			log.error("Validation failed: File control missing");
			throw new ACHValidationException("File control missing");
		}
		if (!"9".equals(file.getFileControl().getRecordTypeCode())) {
			log.error("Invalid FileControl recordTypeCode: {}", file.getFileControl().getRecordTypeCode());
			throw new ACHValidationException("Bad fileControl recordType", "FILE_CONTROL", "recordTypeCode",
					file.getBatches().size() + 2);
		}

		log.info("✅ File control validation passed");
		log.info("🎉 All validations completed successfully!");
	}

	private void validateBatchHeader(BatchHeader header, int batchNum) throws ACHValidationException {
		if (!"5".equals(header.getRecordTypeCode())) {
			log.error("Invalid BatchHeader recordTypeCode in batch {}: {}", batchNum, header.getRecordTypeCode());
			throw new ACHValidationException("Bad batchHeader recordType", "BATCH_HEADER", "recordTypeCode",
					batchNum + 1);
		}
		if (!VALID_SEC_CODES.contains(header.getStandardEntryClassCode())) {
			log.error("Invalid SEC code in batch {}: {}", batchNum, header.getStandardEntryClassCode());
			throw new InvalidSECCodeException("Invalid SEC code: " + header.getStandardEntryClassCode(), batchNum + 1);
		}
		if (header.getOriginatingDFI() == null || header.getOriginatingDFI().length() != 8) {
			log.error("Invalid originating DFI in batch {}: {}", batchNum, header.getOriginatingDFI());
			throw new InvalidRoutingNumberException("Invalid originating DFI: " + header.getOriginatingDFI(),
					batchNum + 1);
		}

		log.debug("Batch {} header is valid", batchNum);
	}

	private void validateEntryDetail(EntryDetail entry, int entryNum) throws ACHValidationException {
		if (!"6".equals(entry.getRecordTypeCode())) {
			log.error("Invalid EntryDetail recordTypeCode at entry {}: {}", entryNum, entry.getRecordTypeCode());
			throw new ACHValidationException("Bad entry recordType", "ENTRY_DETAIL", "recordTypeCode", entryNum);
		}
		if (!VALID_TRANSACTION_CODES.contains(entry.getTransactionCode())) {
			log.error("Invalid transaction code at entry {}: {}", entryNum, entry.getTransactionCode());
			throw new InvalidTransactionCodeException("Invalid transaction code: " + entry.getTransactionCode(),
					entryNum);
		}
		if (entry.getReceivingDFIIdentification() == null || entry.getReceivingDFIIdentification().length() != 8) {
			log.error("Invalid receiving DFI at entry {}: {}", entryNum, entry.getReceivingDFIIdentification());
			throw new InvalidRoutingNumberException("Invalid receiving DFI: " + entry.getReceivingDFIIdentification(),
					entryNum);
		}

		log.debug("Entry {} validation passed", entryNum);
	}
}
