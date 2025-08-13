package cn.isliu.core;

import java.util.Objects;

public class FsTableData {

    private Integer row;
    private String uniqueId;
    private Object data;

    public FsTableData() {
    }

    public FsTableData(Integer row, String uniqueId, Object data) {
        this.row = row;
        this.uniqueId = uniqueId;
        this.data = data;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FsTableData that = (FsTableData) o;
        return Objects.equals(row, that.row) && Objects.equals(uniqueId, that.uniqueId) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, uniqueId, data);
    }

    @Override
    public String toString() {
        return "FsTableData{" +
                "row=" + row +
                ", uniqueId='" + uniqueId + '\'' +
                ", data=" + data +
                '}';
    }
}
