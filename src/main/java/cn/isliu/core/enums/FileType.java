package cn.isliu.core.enums;

public enum FileType {
    IMAGE("image"),
    FILE("file"),

    UNKNOWN("unknown");

    private String type;

    FileType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static FileType getType(String type) {
        for (FileType fileType : FileType.values()) {
            if (fileType.getType().equals(type)) {
                return fileType;
            }
        }
        return UNKNOWN;
    }
}
