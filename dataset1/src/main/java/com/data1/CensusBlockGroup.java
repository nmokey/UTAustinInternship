package com.data1;

public class CensusBlockGroup implements Comparable<String> {
    private String cbgID;
    private int deviceCount;

    /**
     * @param cbgString
     * @param deviceCount
     * @since 1.1.0
     */
    public CensusBlockGroup(String cbgString, String deviceCount) {
        this.cbgID = cbgString;
        this.deviceCount = Integer.parseInt(deviceCount);
    }

    public int incrementDeviceCount(String newCount) {
        deviceCount += Integer.parseInt(newCount);
        return deviceCount;
    }


    public String getCbgID() {
        return cbgID;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    @Override
    public int compareTo(String otherID) {
        return this.cbgID.compareTo(otherID);
    }

    @Override
    public String toString() {
        return cbgID;
    }
}
