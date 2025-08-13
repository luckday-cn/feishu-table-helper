package cn.isliu.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableRow {
    private List<Cell> cells = new ArrayList<>();

    public List<Cell> getCells() {
        return cells;
    }

    public void setCells(List<Cell> cells) {
        this.cells = cells;
    }

    public TableRow(List<Cell> cells) {
        this.cells = cells;
    }

    public TableRow() {
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TableRow tableRow = (TableRow) o;
        return Objects.equals(cells, tableRow.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cells);
    }

    @Override
    public String toString() {
        return "TableRow{" +
                "cells=" + cells +
                '}';
    }
}
