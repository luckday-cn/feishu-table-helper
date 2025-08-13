package cn.isliu.core;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TableData {
    private List<TableRow> rows = new ArrayList<>();

    public List<TableRow> getRows() {
        return rows;
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TableData tableData = (TableData) o;
        return Objects.equals(rows, tableData.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rows);
    }

    public TableData(List<TableRow> rows) {
        this.rows = rows;
    }

    public TableData() {
    }
}
