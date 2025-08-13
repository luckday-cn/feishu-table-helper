package cn.isliu.core;


public class Cell {

    private int row;
    private int col;
    private Object value;
    private Merge merge;

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Merge getMerge() {
        return merge;
    }

    public void setMerge(Merge merge) {
        this.merge = merge;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    @Override
    public String toString() {
        return "Cell{" +
                "row=" + row +
                ", col=" + col +
                ", value=" + value +
                ", merge=" + merge +
                '}';
    }

    public Cell(int row, int col, Object o) {
        this.row = row;
        this.col = col;
        this.value = o;
    }

    public Cell(int row, int col, Object o, Merge merge) {
        this.row = row;
        this.col = col;
        this.value = o;
        this.merge = merge;
    }

    public Cell() {
    }

    public boolean isMerged() {
        return merge != null;
    }
}
