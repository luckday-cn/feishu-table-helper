package cn.isliu.core.config;

/**
 * 配置变更事件
 */
public class ConfigChangeEvent {
    
    private final ConfigSnapshot oldSnapshot;
    private final ConfigSnapshot newSnapshot;
    private final long timestamp;
    
    /**
     * 创建配置变更事件
     * @param oldSnapshot 旧配置快照
     * @param newSnapshot 新配置快照
     */
    public ConfigChangeEvent(ConfigSnapshot oldSnapshot, ConfigSnapshot newSnapshot) {
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取旧配置快照
     * @return 旧配置快照
     */
    public ConfigSnapshot getOldSnapshot() {
        return oldSnapshot;
    }
    
    /**
     * 获取新配置快照
     * @return 新配置快照
     */
    public ConfigSnapshot getNewSnapshot() {
        return newSnapshot;
    }
    
    /**
     * 获取事件时间戳
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 检查指定字段是否发生变更
     * @param fieldName 字段名
     * @return 是否发生变更
     */
    public boolean hasChanged(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "headline":
                return oldSnapshot.getHeadLine() != newSnapshot.getHeadLine();
            case "titleline":
                return oldSnapshot.getTitleLine() != newSnapshot.getTitleLine();
            case "iscover":
                return oldSnapshot.isCover() != newSnapshot.isCover();
            case "celltext":
                return oldSnapshot.isCellText() != newSnapshot.isCellText();
            case "forecolor":
                return !oldSnapshot.getForeColor().equals(newSnapshot.getForeColor());
            case "backcolor":
                return !oldSnapshot.getBackColor().equals(newSnapshot.getBackColor());
            default:
                return false;
        }
    }
    
    @Override
    public String toString() {
        return "ConfigChangeEvent{" +
                "oldSnapshot=" + oldSnapshot +
                ", newSnapshot=" + newSnapshot +
                ", timestamp=" + timestamp +
                '}';
    }
}