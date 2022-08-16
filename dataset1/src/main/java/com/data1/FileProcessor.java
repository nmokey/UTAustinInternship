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

public class FileProcessor {
    private List<String[]> currentList; // each element is a String[] which represents one line of the csv file.
    private List<String[]> processedData = new ArrayList<String[]>();
    private ArrayList<String> seenOrigins = new ArrayList<>();
    private BoxAPIConnection api;
    private ProgressBar dailyProgress, writingProgress;
    private File currentFile, desktop;
    private String year, month, days, startDate;
    private String desktopPath;
    private Boolean dataInRoot;

    public FileProcessor(String year, String month, String days, String startDate, BoxAPIConnection api,
            Boolean dataInRoot)
            throws IOException, CsvException, InterruptedException {
        this.year = year;
        this.month = month;
        this.days = days;
        this.startDate = startDate;
        this.dataInRoot = dataInRoot;
        this.api = api;
        desktop = new File(System.getProperty("user.home"), "/Desktop");
        desktopPath = desktop.getAbsolutePath();
        retrieveFiles();
    }

    /*
     * Currently this method is customized to only aggregate certain periods of
     * time.
     * To change which days are aggregated, adjust the instance variables
     * year, month, days, and startDate.
     */
    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        AppScreen.updateStatus("==========Processing files==========");
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        if (!dataInRoot) {
            for (BoxItem.Info layer1 : rootFolder) {
                if (layer1.getName().equals("Urban Information Lab")) {
                    BoxFolder layer1Folder = ((BoxFolder.Info) layer1).getResource();
                    for (BoxItem.Info layer2 : layer1Folder) {
                        if (layer2.getName().equals("COVID19_research")) {
                            BoxFolder layer2Folder = ((BoxFolder.Info) layer2).getResource();
                            for (BoxItem.Info layer3 : layer2Folder) {
                                if (layer3.getName().equals("SafeGraph")) {
                                    BoxFolder layer3Folder = ((BoxFolder.Info) layer3).getResource();
                                    for (BoxItem.Info layer4 : layer3Folder) {
                                        if (layer4.getName().equals("SDM_daily_v2")) {
                                            rootFolder = ((BoxFolder.Info) layer4).getResource();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (BoxItem.Info dataItem : rootFolder) {
            if (!dataItem.getName().equals("daily-social-distancing-v2")) {
                break;
            }
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
        if (processedData.isEmpty()) { // if first file
            processedData = thisData;
            Collections.sort(processedData, new Comparator<String[]>() { // Sort processedData by origin
                @Override
                public int compare(String[] o1, String[] o2) {
                    return o1[0].compareTo(o2[0]);
                }
            });
            for (String[] row : processedData) { // Add all initial origins to seenOrigins
                seenOrigins.add(row[0]);
                dailyProgress.step();
            }
            return;
        }
        for (String[] row : thisData) {
            int originIndex = Collections.binarySearch(seenOrigins, row[0]);
            if (originIndex > -1) { // if the origin has been seen before
                String[] processedRow = processedData.get(originIndex); // processedRow is row containing this origin
                int seenDevice = Integer.parseInt(processedRow[3]);
                int newDevice = Integer.parseInt(row[3]);
                processedRow[3] = seenDevice + newDevice + ""; // increment device_count
                incrementDestinations(row, processedRow);
            } else { // if the origin is a new one
                processedData.add(-originIndex - 1, row);
                seenOrigins.add(-originIndex - 1, row[0]);
            }
            dailyProgress.step();
        }
    }

    private void incrementDestinations(String[] newRow, String[] processedRow) {
        ArrayList<String> seenDestinations = new ArrayList<String>(
                Arrays.asList(processedRow[13].substring(1, processedRow[13].length() - 1).split(",")));
        String[] dailyDestinations = newRow[13].substring(1, newRow[13].length() - 1).split(",");
        for (String newDest : dailyDestinations) {
            int destinationCounter = seenDestinations.size();
            for (int i = 0; i < seenDestinations.size(); i++) {
                String seenDest = seenDestinations.get(i);
                if (newDest.substring(1, 13).equals(seenDest.substring(1, 13))) {
                    int incremented = Integer.parseInt(newDest.substring(15))
                            + Integer.parseInt(seenDest.substring(15));
                    seenDest = seenDest.split(":")[0] + ":" + incremented;
                    seenDestinations.set(i, seenDest); // REMOVING THIS LINE BREAKS THE PROCESSOR. DON'T BREAK AGAIN!!
                    break; // move onto next newDest
                }
                destinationCounter--;
            }
            if (destinationCounter == 0) {
                seenDestinations.add(newDest);
            }
        }
        processedRow[13] = "{";
        for (String destination : seenDestinations) {
            processedRow[13] += destination + ",";
        }
        processedRow[13] = processedRow[13].substring(0, processedRow[13].length() - 1) + "}";
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

    private String formatDay(String day) {
        return Integer.parseInt(day) < 10 ? "0" + day : day;
    }

    private void writeCSV(String month) throws IOException {
        AppScreen.updateStatus("Writing file month" + year + "_" + month + formatDay(startDate) + "-" + month
                + formatDay(Integer.parseInt(days) + Integer.parseInt(startDate) - 1 + "") + ".csv");
        CSVWriter writer = new CSVWriter(
                new FileWriter(desktopPath + "/" + year + "_" + month + formatDay(startDate) + "-" + month
                        + formatDay(Integer.parseInt(days) + Integer.parseInt(startDate) - 1 + "") + ".csv"));
        writingProgress = new ProgressBar("Writing csv file: ", processedData.size());
        writer.writeNext(
                new String[] { "device_count", "origin_census_block_group", "destination", "destination_count" });
        for (String[] row : processedData) { // for each row in the data list:
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
        newRow[3] = destination.split(":")[1];
        return newRow;
    }
}
