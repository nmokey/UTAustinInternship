package com.data1;

import java.io.IOException;
import com.opencsv.exceptions.CsvException;
import com.box.*;
import com.box.sdk.BoxAPIConnection;

public class App {
	public static void main(String[] args) throws IOException, CsvException {
		new FileProcess();
		System.out.println("Done!");
		BoxAPIConnection api = new BoxAPIConnection("My access token");
	}
}
