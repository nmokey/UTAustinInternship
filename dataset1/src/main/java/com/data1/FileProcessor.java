package com.data1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

public class FileProcessor {
    private List<String[]> currentList; // each element is a String[] which represents one line of the csv file.
    private ArrayList<CensusBlockGroup> seenOrigins = new ArrayList<>();
    private String[] newDestinations;
    private BoxAPIConnection api;
    private ProgressBar dailyProgress, writingProgress;
    private File currentFile, desktop;
    private String year, month, days, startDate;
    private String desktopPath;

    public FileProcessor(String year, String month, String days, String startDate, String authcode)
            throws IOException, CsvException, InterruptedException {
        this.year = year;
        this.month = month;
        this.days = days;
        this.startDate = startDate;
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
                        currentFile = new File(desktopPath + "/" + fileName); // recognize file locally
                        readCSV(desktopPath + "/" + fileName); // save CSV contents to list
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
        if (seenOrigins.isEmpty()) { // If processing first file
            Collections.sort(thisData, new Comparator<String[]>() { // Sort by origin
                @Override
                public int compare(String[] o1, String[] o2) {
                    return o1[0].compareTo(o2[0]);
                }
            });
            for (String[] row : thisData) { // Add all initial origins to seenOrigins
                CensusBlockGroup thisOrigin = new CensusBlockGroup(row[0], row[3]);
                incrementDestinations(thisOrigin, row);
                seenOrigins.add(thisOrigin);
                dailyProgress.step();
            }
            return;
        }
        for (String[] row : thisData) {
            int originIndex = Collections.binarySearch(seenOrigins, row[0]);
            if (originIndex > -1) { // if the origin has been seen before
                CensusBlockGroup processedOrigin = seenOrigins.get(originIndex);
                incrementDestinations(processedOrigin, row);
            } else { // if the origin is a new one
                CensusBlockGroup newOrigin = new CensusBlockGroup(row[0], row[3]);
                incrementDestinations(newOrigin, row);
                seenOrigins.add(-originIndex - 1, newOrigin);
            }
            //dailyProgress.step();
        }
    }

    private void incrementDestinations(CensusBlockGroup origin, String[] newRow) {
        newDestinations = newRow[13].substring(1, newRow[13].length() - 1).split(",");
        for (String destinationString : newDestinations) {
            origin.addAssociatedCBG(
                    new CensusBlockGroup(destinationString.substring(1, 13), destinationString.substring(15)));
        }
        newDestinations = null;
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(desktopPath + "/" + info.getName());
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
        CSVWriter writer = new CSVWriter(new FileWriter(desktopPath + "/month" + month + ".csv"));
        writingProgress = new ProgressBar("Writing csv file: ", seenOrigins.size());
        writer.writeNext(
                new String[] { "device_count", "origin_census_block_group", "destination", "destination_count" });
        for (CensusBlockGroup origin : seenOrigins) { // for each row in the data list:
            for (CensusBlockGroup destination : origin.getAssociatedCBG()) {
                writer.writeNext(new String[] { origin.getDeviceCount() + "", origin.getCbgID(), destination.getCbgID(),
                        destination.getDeviceCount() + "" });
            }
            writingProgress.step();
        }
        writer.close();
        AppScreen.completeTask();
    }
}