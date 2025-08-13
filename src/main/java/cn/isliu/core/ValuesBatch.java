package cn.isliu.core;

import java.util.List;
import java.util.Objects;

public class ValuesBatch {
    private int revision;
    private String spreadsheetToken;
    private int totalCells;
    private List<ValueRange> valueRanges;

    public ValuesBatch() {
    }

    public ValuesBatch(int revision, String spreadsheetToken, int totalCells, List<ValueRange> valueRanges) {
        this.revision = revision;
        this.spreadsheetToken = spreadsheetToken;
        this.totalCells = totalCells;
        this.valueRanges = valueRanges;
    }


    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getSpreadsheetToken() {
        return spreadsheetToken;
    }

    public void setSpreadsheetToken(String spreadsheetToken) {
        this.spreadsheetToken = spreadsheetToken;
    }

    public int getTotalCells() {
        return totalCells;
    }

    public void setTotalCells(int totalCells) {
        this.totalCells = totalCells;
    }

    public List<ValueRange> getValueRanges() {
        return valueRanges;
    }

    public void setValueRanges(List<ValueRange> valueRanges) {
        this.valueRanges = valueRanges;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ValuesBatch that = (ValuesBatch) o;
        return revision == that.revision && totalCells == that.totalCells && Objects.equals(spreadsheetToken, that.spreadsheetToken) && Objects.equals(valueRanges, that.valueRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, spreadsheetToken, totalCells, valueRanges);
    }

    @Override
    public String toString() {
        return "ValuesBatch{" +
                "revision=" + revision +
                ", spreadsheetToken='" + spreadsheetToken + '\'' +
                ", totalCells=" + totalCells +
                ", valueRanges=" + valueRanges +
                '}';
    }
}