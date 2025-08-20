package cn.isliu.core;

import java.util.Map;
import java.util.Objects;

/**
 * 实体类基类
 * 
 * 所有需要与飞书表格进行映射的实体类都应该继承此类，
 * 以便提供统一的唯一标识符管理功能。
 */
public abstract class BaseEntity {

    /**
     * 唯一标识符，用于标识表格中的行数据
     */
    public String uniqueId;
    /**
     * 行号，用于标识表格中的行位置
     */
    private Integer row;
    /**
     * 行数据，用于存储与表格行相关的信息
     */
    private Map<String, Object> rowData;

    /**
     * 获取唯一标识符
     * 
     * @return 唯一标识符字符串
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * 设置唯一标识符
     * 
     * @param uniqueId 唯一标识符字符串
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Map<String, Object> getRowData() {
        return rowData;
    }

    public void setRowData(Map<String, Object> rowData) {
        this.rowData = rowData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(uniqueId, that.uniqueId) && Objects.equals(row, that.row) && Objects.equals(rowData, that.rowData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, row, rowData);
    }
}