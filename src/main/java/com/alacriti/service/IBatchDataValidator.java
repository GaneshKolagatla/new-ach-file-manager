package com.alacriti.service;

import com.alacriti.exceptions.ACHValidationException;
import com.alacriti.util.ACHFile;

public interface IBatchDataValidator {
	void validate(ACHFile file) throws ACHValidationException;
}