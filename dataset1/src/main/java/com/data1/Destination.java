package com.data1;

public class Destination implements Comparable<Destination> {
    private int destinationID, deviceCount;

    public Destination(String destinationID) {
        this.destinationID = Integer.parseInt(destinationID);
        this.deviceCount = 0;
    }

    public Destination(String destinationID, String deviceCount) {
        this.destinationID = Integer.parseInt(destinationID);
        this.deviceCount = Integer.parseInt(deviceCount);
    }

    public void incrementDestinationCount(int newCount) {
        deviceCount += newCount;
    }

    public int getDestinationID() {
        return destinationID;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    @Override
    public int compareTo(Destination other) {
        return this.destinationID - other.destinationID;
    }
}
