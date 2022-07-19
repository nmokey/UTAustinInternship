package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import me.tongfei.progressbar.ProgressBar;

public class FileAggregator {
    private String[] currentRow;
    private ArrayList<CensusBlockGroup> destinations;
    private static List<SociodemographicGroup> sociodemographicData;
    private CSVReader processedDataReader, sociodemographicReader;
    private List<String[]> outputList;
    private BoxAPIConnection api;
    private File currentFile, outputFile, desktop;
    private String year, month, currentOrigin;
    private String desktopPath, fileName;
    private ProgressBar readingProgressBar;

    public FileAggregator(String year, String month, BoxAPIConnection api)
            throws InterruptedException, IOException, CsvException {
        this.year = year;
        this.month = month;
        desktop = new File(System.getProperty("user.home"), "/Desktop");
        desktopPath = desktop.getAbsolutePath();
        this.api = api;
        readSociodemographicData();
        retrieveFiles();
    }

    /**
     * @throws IOException
     * @throws CsvException
     * @throws InterruptedException
     * @since 1.1.0
     */
    private void retrieveFiles() throws IOException, CsvException, InterruptedException {
        AppScreen.updateStatus("==========Aggregating data of month " + month + "==========");
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
                        processedDataReader = new CSVReader(new FileReader(desktopPath + "/" + fileName));
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
                        AppScreen.updateStatus("==========Done aggregating!==========");
                    }
                }
            }
        }
    }

    private void readSociodemographicData() throws IOException, CsvValidationException {
        if (sociodemographicData == null) {
            AppScreen.updateStatus("Reading sociodemographic data");
            sociodemographicData = new ArrayList<SociodemographicGroup>();
            readingProgressBar = new ProgressBar("Reading sociodemographic data: ", 217740);
            sociodemographicReader = new CSVReader(
                    new FileReader("UTAustinInternship/dataset1/src/main/resources/ACS_summary.csv"));
            sociodemographicReader.skip(1);
            String[] dataRow = sociodemographicReader.readNext();
            while (dataRow != null) {
                sociodemographicData.add(new SociodemographicGroup(dataRow));
                dataRow = sociodemographicReader.readNext();
                readingProgressBar.step();
            }
            sociodemographicReader.close();
            AppScreen.completeTask();
        }
    }

    /**
     * @throws IOException
     * @throws CsvException
     * @since 1.1.0
     */
    private void aggregateData() throws IOException, CsvException {
        String[] newRow = new String[4];
        processedDataReader.skip(1);
        currentRow = processedDataReader.readNext();
        readingProgressBar = new ProgressBar("Aggregating origin information: ", 220000);
        while (currentRow != null) { // TODO: this loop runs extremely fast... why doesn't processing? research
                                     // question.
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
            currentRow = processedDataReader.readNext();
            readingProgressBar.step();
        }
        processedDataReader.close();
        readingProgressBar = new ProgressBar("Adding destination_count and sociodemographic data: ", outputList.size());
        for (int i = 0; i < outputList.size(); i++) {
            String[] outputRow = outputList.get(i);
            if (outputRow[0] != null) {
                int destinationIndex = Collections.binarySearch(destinations, outputRow[0]);
                if (destinationIndex > -1) {
                    CensusBlockGroup currentDest = destinations.get(destinationIndex);
                    outputRow[2] = currentDest.getDeviceCount() + "";
                } else {
                    outputRow[2] = "0";
                }
            }
            addSociodemographicData(outputRow, i);
            readingProgressBar.step();
        }
    }

    /**
     * @param row
     * @throws IOException
     * @throws CsvException
     * @since 1.1.0
     */
    private void addDestination(String[] row) throws IOException, CsvException {
        int destinationIndex = Collections.binarySearch(destinations, row[2]);
        if (destinationIndex > -1) {
            destinations.get(destinationIndex).incrementDeviceCount(row[3]);
        } else {
            if (row[2].chars().allMatch(Character::isDigit) && row[3].chars().allMatch(Character::isDigit)) {
                destinations.add(-destinationIndex - 1, new CensusBlockGroup(row[2], row[3]));
            }
        }
    }

    /**
     * @param row
     * @since 1.2.0
     */
    private void addSociodemographicData(String[] row, int i) {
        if(row[0]==null){
            return;
        }
        int originIndex = Collections.binarySearch(sociodemographicData, row[0]);
        if (originIndex > -1) {
            SociodemographicGroup group = sociodemographicData.get(originIndex);
            row = ArrayUtils.addAll(row, group.getData());
            outputList.set(i, row);
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
        AppScreen.completeTask();
    }

    private void writeCSV() throws IOException {
        CSVWriter writer = new CSVWriter(
                new FileWriter(desktopPath + "/" + fileName.substring(0, fileName.length() - 4) + "_aggregated.csv"));
        writer.writeNext(
                new String[] { "origin_census_block_group", "origin_count", "destination_count", "device_count",
                        "median_age", "medianHHincome", "rate_POPworker", "n_pop", "rate_WorkerDriveAlone",
                        "rate_WorkerBus", "rate_WorkerSubway", "rate_WorkerTrain", "rate_WorkerWalk",
                        "rate_POPwhitealone", "rate_POPafrican", "rate_POPnative", "rate_POPasian", "rate_POPmale",
                        "rate_HHwithChild" });
        writer.writeAll(outputList);
        writer.close();
    }
}
