package cn.isliu.core;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class GridProperties {
    @SerializedName("frozen_row_count")
    private int frozenRowCount;
    
    @SerializedName("frozen_column_count")
    private int frozenColumnCount;
    
    @SerializedName("row_count")
    private int rowCount;
    
    @SerializedName("column_count")
    private int columnCount;

    public GridProperties() {
    }
    public GridProperties(int frozenRowCount, int frozenColumnCount, int rowCount, int columnCount) {
        this.frozenRowCount = frozenRowCount;
        this.frozenColumnCount = frozenColumnCount;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }
    public int getFrozenRowCount() {
        return frozenRowCount;
    }
    public int getFrozenColumnCount() {
        return frozenColumnCount;
    }
    public int getRowCount() {
        return rowCount;
    }
    public int getColumnCount() {
        return columnCount;
    }
    public void setFrozenRowCount(int frozenRowCount) {
        this.frozenRowCount = frozenRowCount;
    }
    public void setFrozenColumnCount(int frozenColumnCount) {
        this.frozenColumnCount = frozenColumnCount;
    }
    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }
    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GridProperties gridProperties = (GridProperties) o;
        return frozenRowCount == gridProperties.frozenRowCount && frozenColumnCount == gridProperties.frozenColumnCount && rowCount == gridProperties.rowCount && columnCount == gridProperties.columnCount;
    }
    @Override
    public int hashCode() {
        return Objects.hash(frozenRowCount, frozenColumnCount, rowCount, columnCount);
    }
    @Override
    public String toString() {
        return "GridProperties{" +
                "frozenRowCount=" + frozenRowCount +
                ", frozenColumnCount=" + frozenColumnCount +
                ", rowCount=" + rowCount +
                ", columnCount=" + columnCount +
                '}';
    }
}