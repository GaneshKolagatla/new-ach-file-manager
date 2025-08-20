package com.alacriti.util;

import java.util.List;

import lombok.Data;

@Data
public class ACHFile {
	public FileHeader fileHeader;
	public List<Batch> batches;
	public FileControl fileControl;
}