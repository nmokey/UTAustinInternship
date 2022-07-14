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

public class FileAggregator {
    private List<String[]> currentListByOrigin, currentListByDestination; // each element is a String[] which represents
                                                                          // one line of the csv file.
    private List<String[]> outputList = new ArrayList<String[]>();
    private ArrayList<String> seenOrigins = new ArrayList<>();
    private BoxAPIConnection api;
    private ProgressBar dailyProgress, writingProgress;
    private File currentFile, desktop;
    private String year, month;
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
        AppScreen.updateStatus("See terminal for progress details.");
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
                        currentListByOrigin = null;
                        currentListByDestination = null;
                        dailyProgress = null;
                        fileName = dayItem.getName();
                        AppScreen.updateStatus("Aggregating file " + fileName);
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        currentFile = new File(desktopPath + "/" + fileName); // recognize file locally
                        readCSV(desktopPath + "/" + fileName); // save CSV contents to list
                        currentFile.delete(); // delete local file
                        aggregateData();
                        AppScreen.completeTask();
                        writeCSV(monthItem.getName());
                    }
                    AppScreen.updateStatus("==========Done processing!==========");
                }
            }
        }
    }

    private void aggregateData() {
        dailyProgress = new ProgressBar("Aggregating file " + currentFile.getName(), currentListByOrigin.size());
        currentListByOrigin.remove(0);
        currentListByDestination = new ArrayList<String[]>(currentListByOrigin);
        Collections.sort(currentListByDestination, new Comparator<String[]>() { // Sort by destination
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[2].compareTo(o2[2]);
            }
        });
        String currentOrigin = currentListByOrigin.get(0)[1];
        for (String[] oldRow : currentListByOrigin) {
            String[] newRow = new String[4];
            if (currentOrigin.equals(oldRow[1]) && !oldRow[1].equals(oldRow[2])) { // if still same origin
                newRow[1] = Integer.parseInt(newRow[1]) + Integer.parseInt(oldRow[3]) + ""; // add the origin_count
                
            } else if (!currentOrigin.equals(oldRow[1])) { // if we've moved on to the next origin
                currentOrigin = oldRow[1];
                outputList.add(newRow);
                newRow[0] = oldRow[1];
                newRow[1] = oldRow[3];
                for(int i = findBounds(oldRow)[0]; i< findBounds(oldRow)[1];i++){
                    if(!oldRow[1].equals(oldRow[2])){
                        newRow[2]=Integer.parseInt(newRow[2])+Integer.parseInt(oldRow[3])+""; //add the destination_count
                    }
                }
                newRow[3] = oldRow[0];
            }
            dailyProgress.step();
        }
    }

    private int[] findBounds(String[] destinationRow) {
        int[] bounds = new int[2];
        int index = Collections.binarySearch(currentListByDestination, destinationRow, new Comparator<String[]>(){
            @Override
            public int compare(String[] o1, String[] o2) {
                return o1[2].compareTo(o2[2]);
            }
        });
        while (currentListByDestination.get(index - 1)[2].equals(destinationRow[2])) {
            index--;
        }
        bounds[0] = index;
        while (currentListByDestination.get(index + 1)[2].equals(destinationRow[2])) {
            index++;
        }
        bounds[1] = index;
        return bounds;
    }

    private void addToData(List<String[]> thisData) {
        dailyProgress = new ProgressBar("Processing file " + currentFile.getName(), thisData.size());
        thisData.remove(0); // Get rid of headers to avoid IndexOutOfBounds
        for (String[] row : thisData) {
            int originIndex = Collections.binarySearch(seenOrigins, row[0]);
            if (originIndex > -1) { // if the origin has been seen before
                String[] monthlyRow = outputList.get(originIndex);
                int monthlyDevice = Integer.parseInt(monthlyRow[3]);
                int newDevice = Integer.parseInt(row[3]);
                monthlyRow[3] = monthlyDevice + newDevice + "";
                incrementDestinations(row, monthlyRow);
            } else { // if the origin is a new one
                outputList.add(-originIndex - 1, row);
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
        FileOutputStream stream = new FileOutputStream(desktopPath + "/" + info.getName());
        file.download(stream);
        stream.close();
    }

    private void readCSV(String fileName) throws IOException, CsvException {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            currentListByOrigin = reader.readAll(); // reads CSV into a List<String[]>
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeCSV(String month) throws IOException {
        AppScreen.updateStatus("Writing file month" + month + ".csv");
        CSVWriter writer = new CSVWriter(new FileWriter(desktopPath + "/month" + month + ".csv"));
        writingProgress = new ProgressBar("Writing csv file: ", outputList.size());
        writer.writeNext(
                new String[] { "origin_census_block_group", "origin_count", "destination_count", "device_count" });
        for (String[] row : outputList) { // for each row in the data list:
            writer.writeNext(row);
            writingProgress.step();
        }
        writer.close();
        AppScreen.completeTask();
    }
}