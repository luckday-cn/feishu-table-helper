package cn.isliu.core;


import java.util.Objects;

public class Properties {

    private String sheetId;
    private String title;
    private int index;

    public Properties() {
    }

    public Properties(String sheetId, String title, int index) {
        this.sheetId = sheetId;
        this.title = title;
        this.index = index;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Properties that = (Properties) o;
        return index == that.index && Objects.equals(sheetId, that.sheetId) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sheetId, title, index);
    }

    @Override
    public String toString() {
        return "Properties{" +
                "sheetId='" + sheetId + '\'' +
                ", title='" + title + '\'' +
                ", index=" + index +
                '}';
    }
}
