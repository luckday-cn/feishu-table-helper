package cn.isliu.core;

import java.util.List;
import java.util.Objects;

public class ValueRange {

    private String majorDimension;
    private String range;
    private int revision;
    private List<List<Object>> values;

    public ValueRange() {
    }

    public ValueRange(String majorDimension, String range, int revision, List<List<Object>> values) {
        this.majorDimension = majorDimension;
        this.range = range;
        this.revision = revision;
        this.values = values;
    }

    public String getMajorDimension() {
        return majorDimension;
    }

    public void setMajorDimension(String majorDimension) {
        this.majorDimension = majorDimension;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public List<List<Object>> getValues() {
        return values;
    }

    public void setValues(List<List<Object>> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ValueRange that = (ValueRange) o;
        return revision == that.revision && Objects.equals(majorDimension, that.majorDimension) && Objects.equals(range, that.range) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(majorDimension, range, revision, values);
    }

    @Override
    public String toString() {
        return "ValueRange{" +
                "majorDimension='" + majorDimension + '\'' +
                ", range='" + range + '\'' +
                ", revision=" + revision +
                ", values=" + values +
                '}';
    }
}
