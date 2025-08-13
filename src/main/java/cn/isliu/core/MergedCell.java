package cn.isliu.core;

import java.util.Objects;

public class MergedCell extends Cell {

    private int rowSpan;
    private int colSpan;

    public MergedCell() {
        super(0, 0, null);
    }

    public MergedCell(int rowSpan, int colSpan) {
        super(rowSpan, colSpan, null);
        this.rowSpan = rowSpan;
        this.colSpan = colSpan;
    }

    public int getRowSpan() {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan) {
        this.rowSpan = rowSpan;
    }

    public int getColSpan() {
        return colSpan;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = colSpan;
    }

    @Override
    public String toString() {
        return "MergedCell{" +
                "rowSpan=" + rowSpan +
                ", colSpan=" + colSpan +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MergedCell that = (MergedCell) o;
        return rowSpan == that.rowSpan && colSpan == that.colSpan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowSpan, colSpan);
    }
}
