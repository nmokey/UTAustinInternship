package com.fileprocess;

import java.io.IOException;
import com.opencsv.exceptions.CsvException;

public class App {
	public static void main(String[] args) throws IOException, CsvException {
		new FileProcess();
		System.out.println("Done!");
	}
}