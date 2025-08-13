package cn.isliu.core;

import com.google.gson.annotations.SerializedName;

public class Merge {
    @SerializedName("start_row_index")
    private int startRowIndex;
    
    @SerializedName("end_row_index")
    private int endRowIndex;
    
    @SerializedName("start_column_index")
    private int startColumnIndex;
    
    @SerializedName("end_column_index")
    private int endColumnIndex;

    public Merge(int startRowIndex, int endRowIndex, int startColumnIndex, int endColumnIndex) {
        this.startRowIndex = startRowIndex;
        this.endRowIndex = endRowIndex;
        this.startColumnIndex = startColumnIndex;
        this.endColumnIndex = endColumnIndex;
    }

    public Merge() {
    }

    public int getStartRowIndex() {
        return startRowIndex;
    }

    public void setStartRowIndex(int startRowIndex) {
        this.startRowIndex = startRowIndex;
    }

    public int getEndRowIndex() {
        return endRowIndex;
    }

    public void setEndRowIndex(int endRowIndex) {
        this.endRowIndex = endRowIndex;
    }

    public int getStartColumnIndex() {
        return startColumnIndex;
    }

    public void setStartColumnIndex(int startColumnIndex) {
        this.startColumnIndex = startColumnIndex;
    }

    public int getEndColumnIndex() {
        return endColumnIndex;
    }

    public void setEndColumnIndex(int endColumnIndex) {
        this.endColumnIndex = endColumnIndex;
    }

    @Override
    public String toString() {
        return "Merge{" +
                "startRowIndex=" + startRowIndex +
                ", endRowIndex=" + endRowIndex +
                ", startColumnIndex=" + startColumnIndex +
                ", endColumnIndex=" + endColumnIndex +
                '}';
    }

    public int getRowSpan() {
        return (endRowIndex - startRowIndex) + 1;
    }

    public int getColSpan() {
        return (endColumnIndex - startColumnIndex) + 1;
    }
}