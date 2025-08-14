package cn.isliu.core;

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
}