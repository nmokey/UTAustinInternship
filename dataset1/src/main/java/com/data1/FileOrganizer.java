package com.data1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import me.tongfei.progressbar.ProgressBar;

public class FileOrganizer {
    private BoxAPIConnection api;
    private ProgressBar rearrangeProgress, unzipProgress;

    public FileOrganizer(String authcode) throws IOException, InterruptedException {
        api = authorizeAPI(authcode);
        rearrangeProgress = new ProgressBar("Rearranging files:", 731);
        rearrangeFiles(api);
        unzipProgress = new ProgressBar("Unzipping files:", 731);
        unzipFiles(api);
        System.out.println("done orgainzing files!");
    }

    private BoxAPIConnection authorizeAPI(String authcode) throws IOException {
        api = new BoxAPIConnection(
                "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
                "nhg2Qi0VeZX767uhWySRt7KywKu0uKgm",
                authcode);
        // api = new BoxAPIConnection("DEVTOKEN"); // for testing
        return api;

    }

    private void rearrangeFiles(BoxAPIConnection api) {
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    for (BoxItem.Info dayItem : monthFolder) {
                        if (dayItem instanceof BoxFolder.Info) { // If the month hasn't already been rearranged:
                            BoxFolder dayFolder = ((BoxFolder.Info) dayItem).getResource();
                            for (BoxItem.Info dayInfo : dayFolder) {
                                BoxFile dayFile = ((BoxFile.Info) dayInfo).getResource();
                                dayFile.move(monthFolder); // Move the .csv to its parent folder
                                dayFolder.delete(true); // Delete the day folder
                                rearrangeProgress.step();
                            }
                        }
                    }
                }
            }
        }
    }

    private void unzipFiles(BoxAPIConnection api) throws IOException, InterruptedException {
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info dataItem : rootFolder) {
            BoxFolder dataFolder = ((BoxFolder.Info) dataItem).getResource();
            for (BoxItem.Info yearItem : dataFolder) {
                BoxFolder yearFolder = ((BoxFolder.Info) yearItem).getResource();
                for (BoxItem.Info monthItem : yearFolder) {
                    BoxFolder monthFolder = ((BoxFolder.Info) monthItem).getResource();
                    for (BoxItem.Info dayItem : monthFolder) {
                        String fileName = dayItem.getName();
                        if (fileName.substring(fileName.length() - 2).equals("gz")) { // if item hasn't been
                                                                                      // uncompressed
                            BoxFile compressedDataFile = (BoxFile) dayItem.getResource();
                            downloadFile(compressedDataFile); // download the .gz file
                            File oldFile = new File(fileName); // recognize local .gz file
                            decompress(fileName); // decompress the .gz file locally
                            File newFile = new File(fileName.substring(0, 32)); // recognize local .csv file
                            uploadFile(monthFolder, fileName.substring(0, 32)); // upload new .csv file
                            oldFile.delete(); // delete local .gz file
                            newFile.delete(); // delete local .csv file
                            compressedDataFile.delete(); // delete old .gz file from Box
                        }
                        unzipProgress.step();
                    }
                }
            }
        }
    }

    private void uploadFile(BoxFolder location, String fileName)
            throws IOException, InterruptedException {
        File myFile = new File(fileName);
        FileInputStream stream = new FileInputStream(myFile);
        location.uploadLargeFile(stream, fileName.substring(0, 32), myFile.length());
        stream.close();
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(info.getName());
        file.download(stream);
        stream.close();
    }

    private void decompress(String name) {
        String sourceFile = name;
        String targetFile = name.substring(0, 32);
        try (
                FileInputStream fis = new FileInputStream(sourceFile);
                GZIPInputStream gzis = new GZIPInputStream(fis);
                FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fis.close();
            gzis.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}