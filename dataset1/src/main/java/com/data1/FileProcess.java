package com.data1;

import java.io.File;
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
    private String year, month, days, startDate;

    public FileProcess(String year, String month, String days, String startDate, String authcode)
            throws IOException, CsvException, InterruptedException {
        this.year = year;
        this.month = month;
        this.days = days;
        this.startDate = startDate;
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
        AppScreen.updateStatus("See terminal for progress details.");
        AppScreen.updateStatus("==========Processing files==========");
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
                    int currentDay = 0;
                    for (BoxItem.Info dayItem : monthFolder) {
                        currentDay++;
                        if (!monthItem.getName().equals(month)) { // check for correct month
                            break;
                        }
                        if (currentDay < Integer.parseInt(startDate)) { // check for correct date range
                            continue;
                        }
                        if (currentDay > Integer.parseInt(days) + Integer.parseInt(startDate) - 1) {
                            break;
                        }
                        currentList = null;
                        dailyProgress = null;
                        String fileName = dayItem.getName();
                        AppScreen.updateStatus("Processing file " + fileName);
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        currentFile = new File(fileName); // recognize file locally
                        readCSV(fileName); // save CSV contents to list
                        currentFile.delete(); // delete local file
                        addToData(currentList);
                        AppScreen.completeTask();
                    }
                    if (currentDay == Integer.parseInt(days) + Integer.parseInt(startDate)
                            && monthItem.getName().equals(month + "")) {
                        writeCSV(monthItem.getName());
                        AppScreen.updateStatus("==========Done processing!==========");
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
        AppScreen.updateStatus("Writing file month" + month + ".csv");
        CSVWriter writer = new CSVWriter(new FileWriter("UTAustinInternship/dataset1/month" + month + ".csv"));
        writingProgress = new ProgressBar("Writing csv file: ", monthlyData.size());
        writer.writeNext(
                new String[] { "device_count", "origin_census_block_group", "destination", "destination_count" });
        for (String[] row : monthlyData) { // for each row in the data list:
            String[] destinations = row[13].substring(1, row[13].length() - 1).split(",");
            for (String destination : destinations) {
                writer.writeNext(buildRow(row, destination));
            }
            writingProgress.step();
        }
        writer.close();
        AppScreen.completeTask();
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