package cn.isliu.core.enums;


public enum TypeEnum {

    SINGLE_SELECT("SINGLE_SELECT", "单选"),
    MULTI_SELECT("MULTI_SELECT", "多选"),
    TEXT("TEXT", "文本"),
    NUMBER("NUMBER", "数字"),
    DATE("DATE", "日期"),
    TEXT_FILE("TEXT_FILE", "文本文件"),
    MULTI_TEXT("MULTI_TEXT", "多个文本（逗号分割）"),
    TEXT_URL("TEXT_URL", "文本链接")

    ;

    private final String code;
    private final String desc;

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    TypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TypeEnum getByCode(String code) {
        for (TypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
