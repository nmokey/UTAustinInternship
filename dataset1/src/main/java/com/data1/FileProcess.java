package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import me.tongfei.progressbar.ProgressBar;

public class FileProcess {
    private List<String[]> currentList; // each element is a String[] which represents one line of the csv file.
    private List<String[]> monthlyData = new ArrayList<String[]>();
    private ArrayList<String> seenOrigins = new ArrayList<>();
    private BoxAPIConnection api;
    private ProgressBar dailyProgress, writingProgress;
    private File currentFile;
    private final int YEAR = 2019, MONTH = 01, DAYS = 31, START_DATE = 1;
    // private final String AUTHURL =
    // "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";

    public FileProcess() throws IOException, CsvException, InterruptedException {
        api = authorizeAPI();
        retrieveFiles();
    }

    private BoxAPIConnection authorizeAPI() throws IOException {
        try {
            String authcode = new String(System.console().readPassword("ENTER AUTHCODE: "));
            api = new BoxAPIConnection(
                    "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
                    "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
                    authcode);
            // api = new BoxAPIConnection("8Ho3wtVuqZ7ObZZnFWzvF07zGhdoiS3W"); // for
            // testing
            return api;
        } catch (Exception e) {
            System.out.println("\nInvalid authcode!");
            return authorizeAPI();
        }
    }

    /*
     * Currently this method is customized to only aggregate certain periods of
     * time.
     * To change which days are aggregated, adjust the final instance variables
     * MONTH, DAY, and YEAR.
     */
    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    if (!yearItem.getName().equals(YEAR + "")) {
                        break;
                    }
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    int daycounter = 0;
                    for (BoxItem.Info dayItem : monthFolder) {
                        if (!monthItem.getName().equals(MONTH + "")) {
                            break;
                        }
                        if (daycounter > DAYS - 1) {
                            continue;
                        }
                        currentList = null;
                        dailyProgress = null;
                        String fileName = dayItem.getName();
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        currentFile = new File(fileName); // recognize file locally
                        readCSV(fileName); // save CSV contents to list
                        currentFile.delete(); // delete local file
                        addToData(currentList);
                        daycounter++;
                    }
                    if (daycounter == DAYS && monthItem.getName().equals(MONTH + "")) {
                        writeCSV(monthItem.getName());
                        uploadFile(monthFolder, "UTAustinInternship/dataset1/month" + monthItem.getName() + ".csv");
                    }
                }
            }
        }
    }

    private void addToData(List<String[]> thisData) {
        dailyProgress = new ProgressBar("Processing file " + currentFile.getName(), thisData.size());
        thisData.remove(0); // Get rid of headers to avoid IndexOutOfBounds
        if (monthlyData.isEmpty()) {
            monthlyData = thisData;
            Collections.sort(monthlyData, new Comparator<String[]>() { // Sort monthlyData by origin
                @Override
                public int compare(String[] o1, String[] o2) {
                    return o1[0].compareTo(o2[0]);
                }
            });
            for (String[] row : monthlyData) { // Add all initial origins to seenOrigins
                seenOrigins.add(row[0]);
                dailyProgress.step();
            }
            return;
        }
        for (String[] row : thisData) {
            int originIndex = Collections.binarySearch(seenOrigins, row[0]);
            if (originIndex > -1) { // if the origin has been seen before
                String[] monthlyRow = monthlyData.get(originIndex);
                int monthlyDevice = Integer.parseInt(monthlyRow[3]);
                int newDevice = Integer.parseInt(row[3]);
                monthlyRow[3] = monthlyDevice + newDevice + "";
                incrementDestinations(row, monthlyRow);
            } else { // if the origin is a new one
                monthlyData.add(-originIndex - 1, row);
                seenOrigins.add(-originIndex - 1, row[0]);
            }
            dailyProgress.step();
        }
    }

    private void incrementDestinations(String[] dailyRow, String[] monthlyRow) {
        ArrayList<String> monthlyDestinations = new ArrayList<String>(
                Arrays.asList(monthlyRow[13].substring(1, monthlyRow[13].length() - 1).split(",")));
        String[] dailyDestinations = dailyRow[13].substring(1, dailyRow[13].length() - 1).split(",");
        for (String newDest : dailyDestinations) {
            int destinationCounter = monthlyDestinations.size();
            for (int i = 0; i < monthlyDestinations.size(); i++) {
                String monthlyDest = monthlyDestinations.get(i);
                if (newDest.substring(1, 13).equals(monthlyDest.substring(1, 13))) {
                    int combinedPass = Integer.parseInt(newDest.split(":")[1])
                            + Integer.parseInt(monthlyDest.split(":")[1]);
                    monthlyDest = monthlyDest.substring(0, monthlyDest.length() - 1) + combinedPass;
                    break; // move onto next newDest
                }
                destinationCounter--;
            }
            if (destinationCounter == 0) {
                monthlyDestinations.add(newDest);
            }
        }
        monthlyRow[13] = "{";
        for (String destination : monthlyDestinations) {
            monthlyRow[13] += destination + ",";
        }
        monthlyRow[13] = monthlyRow[13].substring(0, monthlyRow[13].length() - 1) + "}";
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(info.getName());
        file.download(stream);
        stream.close();
    }

    private void uploadFile(BoxFolder location, String fileName)
            throws IOException, InterruptedException {
        File myFile = new File(fileName);
        FileInputStream stream = new FileInputStream(myFile);
        location.uploadLargeFile(stream, fileName, myFile.length());
        stream.close();
        System.out.println("Done uploading!");
    }

    private void readCSV(String fileName) throws IOException, CsvException {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            currentList = reader.readAll(); // reads CSV into a List<String[]>
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeCSV(String month) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter("UTAustinInternship/dataset1/month" + month + ".csv"));
        writingProgress = new ProgressBar("Writing csv file: ", monthlyData.size());
        writer.writeNext(new String[] { "device_count", "origin_census_block_group", "destination", "destination_count" });
        for (String[] row : monthlyData) { // for each row in the data list:
            String[] destinations = row[13].substring(1, row[13].length() - 1).split(",");
            for (String destination : destinations) {
                writer.writeNext(buildRow(row, destination));
            }
            writingProgress.step();
        }
        writer.close();
    }

    private String[] buildRow(String[] oldRow, String destination) {
        String[] newRow = new String[4];
        newRow[0] = oldRow[3];
        newRow[1] = oldRow[0];
        newRow[2] = destination.substring(1, 13);
        newRow[3] = destination.substring(15);
        return newRow;
    }
}