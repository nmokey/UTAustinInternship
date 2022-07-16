package com.data1;

import java.util.ArrayList;
import java.util.Collections;

public class CensusBlockGroup implements Comparable<String> {
    private String cbgID;
    private int deviceCount;
    private ArrayList<CensusBlockGroup> associatedCBG; // Sorted arraylist of CBGs associated with this CBG, such as
                                                       // destinations associated with an origin

    public CensusBlockGroup(String cbgString) {
        this.cbgID = cbgString;
        this.deviceCount = 0;
        this.associatedCBG = new ArrayList<>();
    }

    public CensusBlockGroup(String cbgString, String deviceCount) {
        this.cbgID = cbgString;
        this.deviceCount = Integer.parseInt(deviceCount);
        this.associatedCBG = new ArrayList<>();
    }

    public int incrementDeviceCount(String newCount) {
        deviceCount += Integer.parseInt(newCount);
        return deviceCount;
    }

    public void addAssociatedCBG(CensusBlockGroup newCBG) {
        int index = Collections.binarySearch(associatedCBG, newCBG.cbgID);
        if (index > -1) {
            CensusBlockGroup cbg = associatedCBG.get(index);
            cbg.incrementDeviceCount(newCBG.deviceCount + "");
        } else {
            associatedCBG.add(-index - 1, newCBG);
        }
    }

    public ArrayList<CensusBlockGroup> getAssociatedCBG() {
        return associatedCBG;
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
