package cn.isliu.core.config;

/**
 * 配置构建器，用于批量配置更新
 */
public class ConfigBuilder {
    
    Integer headLine;
    Integer titleLine;
    Boolean isCover;
    Boolean cellText;
    String foreColor;
    String backColor;
    
    public ConfigBuilder() {
    }
    
    public ConfigBuilder headLine(int headLine) {
        if (headLine < 0) {
            throw new IllegalArgumentException("headLine must be non-negative, got: " + headLine);
        }
        this.headLine = headLine;
        return this;
    }
    
    public ConfigBuilder titleLine(int titleLine) {
        if (titleLine < 0) {
            throw new IllegalArgumentException("titleLine must be non-negative, got: " + titleLine);
        }
        this.titleLine = titleLine;
        return this;
    }
    
    public ConfigBuilder isCover(boolean isCover) {
        this.isCover = isCover;
        return this;
    }
    
    public ConfigBuilder cellText(boolean cellText) {
        this.cellText = cellText;
        return this;
    }
    
    public ConfigBuilder foreColor(String foreColor) {
        if (foreColor == null) {
            throw new IllegalArgumentException("foreColor cannot be null");
        }
        if (!isValidColor(foreColor)) {
            throw new IllegalArgumentException("Invalid foreColor format: " + foreColor);
        }
        this.foreColor = foreColor;
        return this;
    }
    
    public ConfigBuilder backColor(String backColor) {
        if (backColor == null) {
            throw new IllegalArgumentException("backColor cannot be null");
        }
        if (!isValidColor(backColor)) {
            throw new IllegalArgumentException("Invalid backColor format: " + backColor);
        }
        this.backColor = backColor;
        return this;
    }
    
    /**
     * 验证颜色格式
     * @param color 颜色值
     * @return 是否为有效的颜色格式
     */
    private boolean isValidColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return false;
        }
        // 简单的十六进制颜色验证 #RRGGBB
        return color.matches("^#[0-9A-Fa-f]{6}$");
    }
    
    /**
     * 应用配置到ThreadSafeConfig
     */
    public void apply() {
        FsConfig.getInstance().updateConfig(this);
    }
}