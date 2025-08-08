package com.alacriti.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

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
public class ACHFileProcessorService {

	private final FileHeaderRepository fileHeaderRepo;
	private final BatchHeaderRepository batchHeaderRepo;
	private final EntryDetailRepository entryDetailRepo;

	@Transactional
	public void processACHFile(File file) throws IOException {
		String fileName = file.getName();
		LocalDateTime now = LocalDateTime.now();

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = reader.readLine()) != null) {
				char recordType = line.charAt(0);

				switch (recordType) {
				case '1':
					FileHeader fh = new FileHeader(null, "1", line.substring(1, 3), line.substring(3, 13),
							line.substring(13, 23), line.substring(23, 29), line.substring(29, 33),
							line.substring(33, 34), line.substring(34, 37), line.substring(37, 39),
							line.substring(39, 40), line.substring(40, 63), line.substring(63, 86),
							line.substring(86, 94), fileName, now);
					fileHeaderRepo.save(fh);
					log.info("Saved FileHeader for {}", fileName);
					break;

				case '5':
					BatchHeader bh = new BatchHeader(null, "5", line.substring(1, 4), line.substring(4, 20),
							line.substring(20, 40), line.substring(40, 50), line.substring(50, 53),
							line.substring(53, 63), line.substring(63, 69), line.substring(69, 75),
							line.substring(75, 78), line.substring(78, 79), line.substring(79, 87),
							line.substring(87, 94), fileName, now);
					batchHeaderRepo.save(bh);
					log.info("Saved BatchHeader for {}", fileName);
					break;

				case '6':
					EntryDetail ed = new EntryDetail(null, "6", line.substring(1, 3), line.substring(3, 11),
							line.substring(11, 12), line.substring(12, 29), line.substring(29, 39),
							line.substring(39, 54), line.substring(54, 76), line.substring(76, 78),
							line.substring(78, 79), line.substring(79, 94), fileName, now);
					entryDetailRepo.save(ed);
					log.info("Saved EntryDetail for {}", fileName);
					break;

				default:
					log.warn("Unhandled record type: {}", recordType);
					break;
				}
			}
		} catch (Exception e) {
			log.error("Error processing ACH file: {}", e.getMessage(), e);
			throw e; // triggers roll back
		}
	}
}
