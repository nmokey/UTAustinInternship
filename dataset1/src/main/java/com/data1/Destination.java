package com.data1;

public class Destination implements Comparable<String> {
    private String destinationID;
    private int deviceCount;


    public Destination(String destinationID) {
        this.destinationID = destinationID;
        this.deviceCount = 0;
    }

    public Destination(String destinationID, String deviceCount) {
        this.destinationID = destinationID;
        this.deviceCount = Integer.parseInt(deviceCount);
    }

    public void incrementDestinationCount(String newCount) {
        deviceCount += Integer.parseInt(newCount);
    }

    public String getDestinationID() {
        return destinationID;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    @Override
    public int compareTo(String otherID) {
        return this.destinationID.compareTo(otherID);
    }

    @Override
    public String toString(){
        return destinationID;
    }
}
