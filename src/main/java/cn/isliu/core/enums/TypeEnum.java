package cn.isliu.core.enums;


/**
 * 字段类型枚举
 * 
 * 定义飞书表格支持的字段类型，用于在实体类注解中指定字段的数据类型
 */
public enum TypeEnum {

    /**
     * 单选类型
     */
    SINGLE_SELECT("SINGLE_SELECT", "单选"),
    
    /**
     * 多选类型
     */
    MULTI_SELECT("MULTI_SELECT", "多选"),
    
    /**
     * 文本类型
     */
    TEXT("TEXT", "文本"),
    
    /**
     * 数字类型
     */
    NUMBER("NUMBER", "数字"),
    
    /**
     * 日期类型
     */
    DATE("DATE", "日期"),
    
    /**
     * 文本文件类型
     */
    TEXT_FILE("TEXT_FILE", "文本文件"),
    
    /**
     * 多个文本（逗号分割）类型
     */
    MULTI_TEXT("MULTI_TEXT", "多个文本（逗号分割）"),
    
    /**
     * 文本链接类型
     */
    TEXT_URL("TEXT_URL", "文本链接");

    private final String code;
    private final String desc;

    /**
     * 获取类型编码
     * 
     * @return 类型编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取类型描述
     * 
     * @return 类型描述字符串
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 构造函数
     * 
     * @param code 类型编码
     * @param desc 类型描述
     */
    TypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举值
     * 
     * @param code 类型编码
     * @return 对应的枚举值，未找到返回null
     */
    public static TypeEnum getByCode(String code) {
        for (TypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}