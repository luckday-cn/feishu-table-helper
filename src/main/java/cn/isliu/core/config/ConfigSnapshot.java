package cn.isliu.core.config;

/**
 * 不可变的配置快照
 * 用于线程安全的配置读取
 */
public class ConfigSnapshot {
    
    private final int headLine;
    private final int titleLine;
    private final boolean isCover;
    private final boolean cellText;
    private final String foreColor;
    private final String backColor;
    private final long timestamp;
    
    /**
     * 创建配置快照
     * @param headLine 头部行数
     * @param titleLine 标题行数
     * @param isCover 是否覆盖
     * @param cellText 是否单元格文本
     * @param foreColor 前景色
     * @param backColor 背景色
     */
    public ConfigSnapshot(int headLine, int titleLine, boolean isCover, 
                         boolean cellText, String foreColor, String backColor) {
        this.headLine = headLine;
        this.titleLine = titleLine;
        this.isCover = isCover;
        this.cellText = cellText;
        this.foreColor = foreColor;
        this.backColor = backColor;
        this.timestamp = System.currentTimeMillis();
    }
    
    public int getHeadLine() {
        return headLine;
    }
    
    public int getTitleLine() {
        return titleLine;
    }
    
    public boolean isCover() {
        return isCover;
    }
    
    public boolean isCellText() {
        return cellText;
    }
    
    public String getForeColor() {
        return foreColor;
    }
    
    public String getBackColor() {
        return backColor;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "ConfigSnapshot{" +
                "headLine=" + headLine +
                ", titleLine=" + titleLine +
                ", isCover=" + isCover +
                ", cellText=" + cellText +
                ", foreColor='" + foreColor + '\'' +
                ", backColor='" + backColor + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}