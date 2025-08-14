package cn.isliu.core.pojo;

import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;

/**
 * 字段属性类
 * 
 * 封装实体类字段的属性信息，包括字段名和对应的表格属性注解信息
 */
public class FieldProperty {

    /**
     * 字段名
     */
    private String field;
    
    /**
     * 表格属性注解
     */
    private TableProperty tableProperty;

    /**
     * 无参构造函数
     */
    public FieldProperty() {
    }

    /**
     * 构造函数
     * 
     * @param field 字段名
     * @param tableProperty 表格属性注解
     */
    public FieldProperty(String field, TableProperty tableProperty) {
        this.field = field;
        this.tableProperty = tableProperty;
    }

    /**
     * 获取字段名
     * 
     * @return 字段名字符串
     */
    public String getField() {
        return field;
    }

    /**
     * 设置字段名
     * 
     * @param field 字段名字符串
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * 获取表格属性注解
     * 
     * @return 表格属性注解对象
     */
    public TableProperty getTableProperty() {
        return tableProperty;
    }

    /**
     * 设置表格属性注解
     * 
     * @param tableProperty 表格属性注解对象
     */
    public void setTableProperty(TableProperty tableProperty) {
        this.tableProperty = tableProperty;
    }

    /**
     * 获取字段名（注解中的field属性）
     * 
     * @return 字段名字符串
     */
    public String getFieldField() {
        return tableProperty.field();
    }

    /**
     * 获取列名（注解中的value属性）
     * 
     * @return 列名字符串
     */
    public String getFieldName() {
        return tableProperty.value();
    }

    /**
     * 获取字段类型
     * 
     * @return 字段类型枚举
     */
    public TypeEnum getFieldType() {
        return tableProperty.type();
    }

    /**
     * 获取字段格式化处理类
     * 
     * @return 字段值处理类Class对象
     */
    public Class<? extends FieldValueProcess> getFieldFormat() {
        return tableProperty.fieldFormatClass();
    }

    /**
     * 获取字段枚举类
     * 
     * @return 枚举类Class对象
     */
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