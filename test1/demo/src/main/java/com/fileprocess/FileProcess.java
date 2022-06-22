package com.fileprocess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.FileWriter;

public class FileProcess{
    private List<String[]> list; //each element is a String[] which represents one line of the csv file.
    //private String s;

    public FileProcess() throws IOException, CsvException{
        readCSV();
        writeCSV();
    }

    private void readCSV() throws IOException, CsvException{
        try {
            CSVReader reader = new CSVReader(new FileReader("UTAustinInternship/test1/demo/data/2020-08-01-social-distancing.csv"));
            list = reader.readAll();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeCSV() throws IOException{
        CSVWriter writer = new CSVWriter(new FileWriter("UTAustinInternship/test1/demo/data/output.csv"));
        for(String[] row:list){
            String[] allDestinations = row[13].split(","); //splitting destination string into each destination
            for(String destination:allDestinations){
                writer.writeNext(buildRow(row, destination));
            }
        }
        writer.close();
    }

    private String[] buildRow(String[] oldRow, String destination){
        String[] newRow = new String[5];
        newRow[0] = oldRow[0];
        newRow[1] = destination.substring(1, 13);
        newRow[2] = destination.substring(15, 16);
        newRow[3] = oldRow[1];
        newRow[4] = oldRow[2];
        return newRow;
    }
}