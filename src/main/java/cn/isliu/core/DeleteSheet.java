package cn.isliu.core;

import java.util.Objects;

public class DeleteSheet {

    private boolean result;
    private String sheetId;

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public DeleteSheet() {
    }

    public DeleteSheet(boolean result, String sheetId) {
        this.result = result;
        this.sheetId = sheetId;
    }

    @Override
    public String toString() {
        return "DeleteSheet{" +
                "result=" + result +
                ", sheetId='" + sheetId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DeleteSheet deleteSheet = (DeleteSheet) o;
        return result == deleteSheet.result && Objects.equals(sheetId, deleteSheet.sheetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, sheetId);
    }
}
