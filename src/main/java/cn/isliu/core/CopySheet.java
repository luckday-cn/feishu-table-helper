package cn.isliu.core;


import java.util.Objects;

public class CopySheet {

    private Properties properties;

    public CopySheet() {
    }

    public CopySheet(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CopySheet copySheet = (CopySheet) o;
        return Objects.equals(properties, copySheet.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }

    @Override
    public String toString() {
        return "CopySheet{" +
                "properties=" + properties +
                '}';
    }
}
