package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.FileWriter;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import me.tongfei.progressbar.ProgressBar;

public class FileProcess { // taken from test1, NOT YET ADAPTED FOR BOX API
    private List<String[]> currentList; // each element is a String[] which represents one line of the csv file.
    private List<String[]> data = new ArrayList<String[]>();
    private BoxAPIConnection api;
    private ProgressBar progress;

    // private final String AUTHURL =
    // "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";

    public FileProcess() throws IOException, CsvException, InterruptedException {
        progress = new ProgressBar("Aggregating data:", 731);
        api = authorizeAPI();
        retrieveFiles();
    }

    private BoxAPIConnection authorizeAPI() throws IOException {
        // api = new BoxAPIConnection(
        // "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
        // "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
        // "AUTHCODE" // must replace every time with a new authCode!
        // );
        api = new BoxAPIConnection("ZFRd2MjWP04UxJrYjV1FSqGn8WImsi6U"); // dev token for testing
        return api;
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(info.getName());
        file.download(stream);
        System.out.println("downloaded file!");
        stream.close();
    }

    private void uploadFile(BoxFolder location, String fileName)
            throws IOException, InterruptedException {
        File myFile = new File(fileName);
        FileInputStream stream = new FileInputStream(myFile);
        location.uploadLargeFile(stream, fileName, myFile.length());
        stream.close();
    }

    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    for (BoxItem.Info dayItem : monthFolder) {
                        String fileName = dayItem.getName();
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        File file = new File(fileName); // recognize file locally
                        readCSV(fileName); // save CSV contents to list
                        file.delete(); // delete local file
                        System.out.println("Added file " + fileName);
                        progress.step();
                    }
                    writeCSV(monthItem.getName());
                    uploadFile(monthFolder, "UTAustinInternship/dataset1/month" + monthItem.getName() + ".csv");
                }
            }
        }
    }

    private void readCSV(String fileName) throws IOException, CsvException {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            currentList = reader.readAll(); // reads CSV into a List<String[]>
            addToData(currentList);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Done reading!");
    }

    private void writeCSV(String month) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter("UTAustinInternship/dataset1/month" + month + ".csv"));
        for (String[] row : data) { // for each row in the data list:
            writer.writeNext(row);
        }
        writer.close();
    }

    private void addToData(List<String[]> thisList) {
        thisList.remove(0); // get rid of first row to avoid out-of-bounds errors, who really needs headers
        for (int a = 0; a < thisList.size(); a++) {
            String[] row = thisList.get(a); //row of the raw CSV file
            String[] allDestinations = row[13].substring(1, row[13].length() - 1).split(","); // splitting destinations
            for (int i = 0; i < data.size(); i++) {
                String[] savedRow = data.get(i);
                if (savedRow[1].equals(row[0])) { // origins match
                    savedRow[0] = Integer.parseInt(savedRow[3]) + Integer.parseInt(row[3]) + ""; // increment
                                                                                                 // devicecount
                    for (int dest = 0; dest < allDestinations.length; dest++) {// String destination:allDestinations){
                        String destination = allDestinations[dest];
                        if (savedRow[2].equals(destination.substring(1, 13))) {
                            savedRow[3] = Integer.parseInt(savedRow[3])
                                    + Integer.parseInt(destination.substring(15, 16)) + ""; // increment destination #
                        }
                    }
                } else { // origin not found
                    for (int dest = 0; dest < allDestinations.length; dest++) {// String destination:allDestinations){
                        String destination = allDestinations[dest];
                        data.add(buildRow(row, destination));
                    }
                }
            }
        }
    }

    /**
     * Builds a row of the returned csv document from the old one in the form of a
     * String array.
     * 
     * @param oldRow      a row from the old csv document to be formatted
     * @param destination one entry from the destination column of the row,
     *                    previously separated by commas
     */
    private String[] buildRow(String[] oldRow, String destination) {
        String[] newRow = new String[4];
        newRow[0] = oldRow[3];
        newRow[1] = oldRow[0];
        newRow[2] = destination.substring(1, 13);
        newRow[3] = destination.substring(15, 16);
        return newRow;
    }
}