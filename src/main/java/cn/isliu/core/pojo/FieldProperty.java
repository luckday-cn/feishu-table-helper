package cn.isliu.core.pojo;

import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;

public class FieldProperty {

    private String field;
    private TableProperty tableProperty;

    public FieldProperty() {
    }

    public FieldProperty(String field, TableProperty tableProperty) {
        this.field = field;
        this.tableProperty = tableProperty;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public TableProperty getTableProperty() {
        return tableProperty;
    }

    public void setTableProperty(TableProperty tableProperty) {
        this.tableProperty = tableProperty;
    }

    public String getFieldField() {
        return tableProperty.field();
    }

    public String getFieldName() {
        return tableProperty.value();
    }

    public TypeEnum getFieldType() {
        return tableProperty.type();
    }

    public Class<? extends FieldValueProcess> getFieldFormat() {
        return tableProperty.fieldFormatClass();
    }

    public Class<? extends BaseEnum> getFieldEnum() {
        return tableProperty.enumClass();
    }

    @Override
    public String toString() {
        return "FieldProperty{" +
                "field='" + field + '\'' +
                ", tableProperty=" + tableProperty +
                '}';
    }

}
