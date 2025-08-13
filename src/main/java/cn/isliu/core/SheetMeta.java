package cn.isliu.core;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

public class SheetMeta {
    @SerializedName("sheets")
    private List<Sheet> sheets;

    public List<Sheet> getSheets() {
        return sheets;
    }

    public void setSheets(List<Sheet> sheets) {
        this.sheets = sheets;
    }

    public SheetMeta() {
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SheetMeta sheetMeta = (SheetMeta) o;
        return Objects.equals(sheets, sheetMeta.sheets);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sheets);
    }

    @Override
    public String toString() {
        return "SheetMeta{" +
                "sheets=" + sheets +
                '}';
    }
}