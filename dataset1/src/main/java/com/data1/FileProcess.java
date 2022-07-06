package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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

public class FileProcess {
    private List<String[]> currentList; // each element is a String[] which represents one line of the csv file.
    private List<String[]> monthlyData = new ArrayList<String[]>();
    private ArrayList<String> seenOrigins = new ArrayList<>();
    private BoxAPIConnection api;
    private ProgressBar overallProgress, dailyProgress;
    private File currentFile;

    // private final String AUTHURL =
    // "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";

    public FileProcess() throws IOException, CsvException, InterruptedException {
        overallProgress = new ProgressBar("Aggregating data:", 731);
        api = authorizeAPI();
        retrieveFiles();
    }

    private BoxAPIConnection authorizeAPI() throws IOException {
        api = new BoxAPIConnection(
                "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
                "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
                "AUTHCODE" // must replace every time with a new authCode!
        );
        // api = new BoxAPIConnection("ozyqJRa06MkD6xJgk9DcX5GFuSRKEuPv"); // dev token for testing
        return api;
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
    }

    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    int daycounter = 0;
                    for (BoxItem.Info dayItem : monthFolder) {
                        if (!monthItem.getName().equals("04")) {
                            System.out.println(monthItem.getName());
                            break;
                        }
                        if (daycounter > 6) {
                            continue;
                        }
                        String fileName = dayItem.getName();
                        BoxFile dayFile = (BoxFile) dayItem.getResource(); // recognize boxfile
                        downloadFile(dayFile); // download CSV from box
                        currentFile = new File(fileName); // recognize file locally
                        readCSV(fileName); // save CSV contents to list
                        currentFile.delete(); // delete local file
                        daycounter++;
                        // overallProgress.step();
                    }
                    if (daycounter == 7 && monthItem.getName().equals("4")) {
                        writeCSV(monthItem.getName());
                        uploadFile(monthFolder, "UTAustinInternship/dataset1/month" + monthItem.getName() + ".csv");
                    }
                }
            }
        }
    }

    private void readCSV(String fileName) throws IOException, CsvException {
        try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            currentList = reader.readAll(); // reads CSV into a List<String[]>
            addToData(currentList);
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void addToData(List<String[]> thisData) {
        if (monthlyData.isEmpty()) {
            monthlyData = thisData;
            Collections.sort(monthlyData, new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {
                    return o1[0].compareTo(o2[0]);
                }
            });
            for (String[] row : monthlyData) {
                seenOrigins.add(row[0]);
            }
            return;
        }
        Collections.sort(seenOrigins);
        thisData.remove(0); // get rid of headers
        dailyProgress = new ProgressBar("Processing file " + currentFile.getName(), thisData.size());
        for (String[] row : thisData) {
            if (seenOrigins.contains(row[0])) { // if the origin has been seen before
                String[] monthlyRow = monthlyData.get(Collections.binarySearch(seenOrigins, row[0]));
                // System.out.println("matched an origin!");
                int monthlyDevice = Integer.parseInt(monthlyRow[3]);
                int newDevice = Integer.parseInt(row[3]);
                monthlyRow[3] = monthlyDevice + newDevice + "";
                incrementDestinations(row, monthlyRow);
            } else { // if the origin is a new one
                int pos = Collections.binarySearch(seenOrigins, row[0]);
                monthlyData.add(-pos - 1, row);
                seenOrigins.add(-pos - 1, row[0]);
                // System.out.println("added a new row!");
            }
            dailyProgress.step();
        }
    }

    private void incrementDestinations(String[] dailyRow, String[] monthlyRow) {
        String[] dailyDestinations = dailyRow[13].substring(1, dailyRow[13].length() - 1).split(",");
        String[] monthlyDestinations = monthlyRow[13].substring(1, monthlyRow[13].length() - 1).split(",");
        for (String newDest : dailyDestinations) {
            int destinationCounter = monthlyDestinations.length;
            for (int i = 0; i < monthlyDestinations.length; i++) {
                String monthlyDest = monthlyDestinations[i];
                if (newDest.substring(1, 13).equals(monthlyDest.substring(1, 13))) {
                    int combinedPass = Integer.parseInt(newDest.split(":")[1])
                            + Integer.parseInt(monthlyDest.split(":")[1]);
                    monthlyDest = monthlyDest.substring(0, monthlyDest.length() - 1) + combinedPass;
                    break; // move onto next newDest
                }
                destinationCounter--;
            }
            if (destinationCounter == 0) {
                ArrayList<String> temp = new ArrayList<String>(Arrays.asList(monthlyDestinations));
                temp.add(newDest);
                monthlyDestinations = temp.toArray(monthlyDestinations);
            }
        }
        monthlyRow[13] = "{";
        for (String destination : monthlyDestinations) {
            monthlyRow[13] += destination + ",";
        }
        monthlyRow[13] = monthlyRow[13].substring(0, monthlyRow[13].length() - 1) + "}";
        // System.out.println("incremented destination!");
    }

    private void writeCSV(String month) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter("UTAustinInternship/dataset1/month" + month + ".csv"));
        for (String[] row : monthlyData) { // for each row in the data list:
            String[] destinations = row[13].substring(1, row[13].length() - 1).split(",");
            for (String destination : destinations) {
                writer.writeNext(buildRow(row, destination));
            }
        }
        writer.close();
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