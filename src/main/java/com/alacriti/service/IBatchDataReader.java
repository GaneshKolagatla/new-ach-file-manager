package com.alacriti.service;

import com.alacriti.util.ACHFile;

public interface IBatchDataReader {
	ACHFile read(String filePath) throws Exception;
}