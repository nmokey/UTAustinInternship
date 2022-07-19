package com.data1;

public class SociodemographicGroup implements Comparable<String>{
    private String[] dataRow;
    private String cbgID;

    public SociodemographicGroup(String[] row){
        dataRow = java.util.Arrays.copyOfRange(row, 1, row.length);
        cbgID = row[0].substring(9);
    }

    public String[] getData(){
        return dataRow;
    }

    public String getID(){
        return cbgID;
    }

    @Override
    public String toString(){
        return "ID: " +cbgID+"  Data:"+java.util.Arrays.toString(dataRow);
    }
    @Override
    public int compareTo(String o) {
        return this.cbgID.compareTo(o);
    }
}
