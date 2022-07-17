package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

public class FileAggregator {
    private String[] currentRow;
    private ArrayList<CensusBlockGroup> destinations;
    private CSVReader reader;
    private List<String[]> outputList;
    private BoxAPIConnection api;
    private File currentFile, outputFile, desktop;
    private String year, month, currentOrigin;
    private String desktopPath, fileName;

    public FileAggregator(String year, String month, String authcode)
            throws IOException, CsvException, InterruptedException {
        this.year = year;
        this.month = month;
        desktop = new File(System.getProperty("user.home"), "/Desktop");
        desktopPath = desktop.getAbsolutePath();
        api = authorizeAPI(authcode);
        retrieveFiles();
    }

    private BoxAPIConnection authorizeAPI(String authcode) throws IOException {
        AppScreen.updateStatus("Establishing API connection");
        api = new BoxAPIConnection(
                "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
                "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
                authcode);
        AppScreen.completeTask();
        // api = new BoxAPIConnection("DEVTOKEN"); // for testing
        return api;
    }

    /*
     * Currently this method is customized to only aggregate certain periods of
     * time.
     * To change which days are aggregated, adjust the instance variables
     * year, month, days, and startDate.
     */
    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        AppScreen.updateStatus("==========Aggregating Data of month " + month + "==========");
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    if (!yearItem.getName().equals(year)) { // check for correct year
                        break;
                    }
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    for (BoxItem.Info dayItem : monthFolder) {
                        if (!monthItem.getName().equals(month)) { // check for correct month
                            break;
                        }
                        if (!dayItem.getName().substring(0, 5).equals(year + "_")) { // aggregate only processed files
                            continue;
                        }
                        outputList = new ArrayList<String[]>();
                        destinations = new ArrayList<CensusBlockGroup>();
                        fileName = dayItem.getName();
                        AppScreen.updateStatus("Aggregating file " + fileName);
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        currentFile = new File(desktopPath + "/" + fileName); // recognize file locally
                        reader = new CSVReader(new FileReader(desktopPath + "/" + fileName));
                        currentFile.delete(); // delete local file
                        aggregateData();
                        writeCSV();
                        outputFile = new File(
                                desktopPath + "/" + fileName.substring(0, fileName.length() - 4) + "_aggregated.csv");
                        uploadFile(monthFolder,
                                desktopPath + "/" + fileName.substring(0, fileName.length() - 4) + "_aggregated.csv");
                        outputFile.delete();
                    }
                    if (monthItem.getName().equals(month)) {
                        AppScreen.updateStatus("==========Done processing!==========");
                    }
                }
            }
        }
    }

    private void aggregateData() throws CsvValidationException, IOException {
        String[] newRow = new String[4];
        reader.skip(1);
        currentRow = reader.readNext();
        while (currentRow != null) {
            if (currentOrigin == null) { // it's the very first row
                currentOrigin = currentRow[1];
                newRow[0] = currentRow[1];
                newRow[1] = 0 + "";
                newRow[3] = currentRow[0];
            }
            if (currentOrigin.equals(currentRow[1]) && !currentRow[1].equals(currentRow[2])) { // if still same origin
                newRow[1] = Integer.parseInt(newRow[1]) + Integer.parseInt(currentRow[3]) + ""; // increment
                                                                                                // origin_count
                addDestination(currentRow);
            } else if (!currentOrigin.equals(currentRow[1])) { // if we've moved on to the next origin
                outputList.add(newRow);
                currentOrigin = currentRow[1];
                newRow = new String[4];
                newRow[0] = currentRow[1];
                newRow[1] = currentRow[3];
                addDestination(currentRow);
                newRow[3] = currentRow[0];
            }
            currentRow = reader.readNext();
        }
        reader.close();
        for (int i = 0; i < outputList.size(); i++) {
            String[] row = outputList.get(i);
            if (row[0] == null || destinations == null) {
                System.out.println(Arrays.toString(row) + i);
            } else {
                int index = Collections.binarySearch(destinations, row[0]);
                if (index > -1) {
                    CensusBlockGroup currentDest = destinations.get(index);
                    row[2] = currentDest.getDeviceCount() + "";
                } else {
                    row[2] = "0";
                }
            }
        }
    }

    private void addDestination(String[] row) {
        int index = Collections.binarySearch(destinations, row[2]);
        if (index > -1) {
            destinations.get(index).incrementDeviceCount(row[3]);
        } else {
            if(row[2].chars().allMatch(Character::isDigit)&&row[3].chars().allMatch(Character::isDigit)){
                destinations.add(-index - 1, new CensusBlockGroup(row[2], row[3]));
            }
        }
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(desktopPath + "/" + info.getName());
        file.download(stream);
        stream.close();
    }

    private void uploadFile(BoxFolder location, String fileToUpload)
            throws IOException, InterruptedException {
        File myFile = new File(fileToUpload);
        FileInputStream stream = new FileInputStream(myFile);
        location.uploadFile(stream, fileName.substring(0, fileName.length() - 4) + "_aggregated.csv");
        stream.close();
    }

    private void writeCSV() throws IOException {
        CSVWriter writer = new CSVWriter(
                new FileWriter(desktopPath + "/" + fileName.substring(0, fileName.length() - 4) + "_aggregated.csv"));
        writer.writeNext(
                new String[] { "origin_census_block_group", "origin_count", "destination_count", "device_count" });
        writer.writeAll(outputList);
        writer.close();
        AppScreen.completeTask();
    }
}