package cn.isliu.core;

import java.util.Objects;

public class AddSheet {

    private Properties properties;

    public AddSheet() {
    }

    public AddSheet(Properties properties) {
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
        AddSheet addSheet = (AddSheet) o;
        return Objects.equals(properties, addSheet.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }

    @Override
    public String toString() {
        return "AddSheet{" +
                "properties=" + properties +
                '}';
    }
}
