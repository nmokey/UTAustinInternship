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

public class FileOrganizer {
    public FileOrganizer() throws IOException, InterruptedException {
        //rearrangeFiles(api);
        BoxAPIConnection api = authorizeAPI();
        unzipFiles(api); // must generate a new access token/developer token each time to create the API
                         // connection!
    }

    private BoxAPIConnection authorizeAPI() throws IOException{
        // Reader reader = new FileReader("UTAustinInternship/dataset1/config.json");
        // String userID = "19746625595";
        // BoxConfig config = BoxConfig.readFrom(reader);
        // BoxDeveloperEditionAPIConnection api = new BoxDeveloperEditionAPIConnection.getUserConnection(userID, config, null);
        String authorizationUrl = "https://account.box.com/api/oauth2/authorize?client_id=g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv&redirect_uri=https://google.com&response_type=code";
        BoxAPIConnection client = new BoxAPIConnection(
            "g9lmqv1kb5gw8zzsz8g0ftkd1wzj1hzv",
            "SECRET",
            "AUTHCODE" //must replace every time with a new authCode!
          );
        client.refresh();
        return client;
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
                            }
                        }
                    }
                    System.out.println("Rearranged " + monthItem.getName() + "/" + yearItem.getName()); // Status update
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
                        if(fileName.substring(fileName.length()-2).equals("gz")){ //if the item hasn't been uncompressed
                            BoxFile compressedDataFile = ((BoxFile.Info) dayItem).getResource();
                            downloadFile(compressedDataFile); // download the .gz file
                            File oldFile = new File(fileName); // recognize local .gz file
                            decompress(fileName); // decompress the .gz file locally
                            File newFile = new File(fileName.substring(0, 32)); // recognize local .csv file
                            uploadFile(monthFolder, fileName.substring(0, 32), api); // upload new .csv file
                            oldFile.delete(); // delete local .gz file
                            newFile.delete(); // delete local .csv file
                            compressedDataFile.delete(); // delete old .gz file from Box
                        }
                        System.out.println("Processed file: " + fileName);
                    }
                }
            }
        }
    }

    private void uploadFile(BoxFolder location, String fileName, BoxAPIConnection api)
            throws IOException, InterruptedException {
        // FileInputStream stream = new FileInputStream(fileName);
        // BoxFile.Info newFileInfo = location.uploadFile(stream, fileName);
        // stream.close();
        File myFile = new File(fileName);
        FileInputStream stream = new FileInputStream(myFile);
        BoxFile.Info fileInfo = location.uploadLargeFile(stream, fileName.substring(0, 32), myFile.length());
    }

    private void downloadFile(BoxFile file) throws IOException {
        BoxFile.Info info = file.getInfo();
        FileOutputStream stream = new FileOutputStream(info.getName());
        file.download(stream);
        stream.close();
        System.out.println("Done downloading!");
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
            System.out.println("Done decompressing!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}