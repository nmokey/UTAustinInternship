package com.data1;

import java.io.IOException;
import com.opencsv.exceptions.CsvException;

public class App {
	public static void main(String[] args) throws IOException, CsvException, InterruptedException {
		// new FileOrganizer();
		// System.out.println("Done organizing!");
		new FileProcess();
		System.out.println("Done processing!");
	}
}