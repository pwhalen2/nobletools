package edu.pitt.dbmi.nlp.noble.coder.processor;

import java.io.File;

public class ReportProcessorTest {
	
	public static void main(String [] args) throws Exception{
		new ReportProcessor().processFile(new File(args[0]));
	}
}
