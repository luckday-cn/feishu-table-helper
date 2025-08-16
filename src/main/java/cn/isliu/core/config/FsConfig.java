package cn.isliu.core.config;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线程安全的配置管理器
 * 使用volatile关键字和ReadWriteLock确保线程安全
 */
public class FsConfig {
    
    // 使用volatile确保可见性
    private volatile int headLine = 1;
    private volatile int titleLine = 1;
    private volatile boolean isCover = false;
    private volatile boolean cellText = false;
    private volatile String foreColor = "#000000";
    private volatile String backColor = "#d5d5d5";
    
    // 读写锁保护配置更新操作
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // 配置变更监听器列表
    private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    // 单例实例
    private static volatile FsConfig instance;
    
    private FsConfig() {
    }
    
    /**
     * 获取单例实例
     * @return ThreadSafeConfig实例
     */
    public static FsConfig getInstance() {
        if (instance == null) {
            synchronized (FsConfig.class) {
                if (instance == null) {
                    instance = new FsConfig();
                }
            }
        }
        return instance;
    }
    
    /**
     * 原子性配置更新
     * @param builder 配置构建器
     */
    public void updateConfig(ConfigBuilder builder) {
        // 验证配置
        validateConfig(builder);
        
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            if (builder.headLine != null) {
                this.headLine = builder.headLine;
            }
            if (builder.titleLine != null) {
                this.titleLine = builder.titleLine;
            }
            if (builder.isCover != null) {
                this.isCover = builder.isCover;
            }
            if (builder.cellText != null) {
                this.cellText = builder.cellText;
            }
            if (builder.foreColor != null) {
                this.foreColor = builder.foreColor;
            }
            if (builder.backColor != null) {
                this.backColor = builder.backColor;
            }
        } finally {
            lock.writeLock().unlock();
        }
        
        // 通知配置变更
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    /**
     * 验证配置参数
     * @param builder 配置构建器
     * @throws IllegalArgumentException 如果配置无效
     */
    private void validateConfig(ConfigBuilder builder) {
        if (builder.headLine != null && builder.headLine < 0) {
            throw new IllegalArgumentException("headLine must be non-negative, got: " + builder.headLine);
        }
        if (builder.titleLine != null && builder.titleLine < 0) {
            throw new IllegalArgumentException("titleLine must be non-negative, got: " + builder.titleLine);
        }
        if (builder.foreColor != null && !isValidColor(builder.foreColor)) {
            throw new IllegalArgumentException("Invalid foreColor format: " + builder.foreColor);
        }
        if (builder.backColor != null && !isValidColor(builder.backColor)) {
            throw new IllegalArgumentException("Invalid backColor format: " + builder.backColor);
        }
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
     * 通知配置变更
     * @param oldSnapshot 旧配置快照
     * @param newSnapshot 新配置快照
     */
    private void notifyConfigChange(ConfigSnapshot oldSnapshot, ConfigSnapshot newSnapshot) {
        if (!listeners.isEmpty()) {
            ConfigChangeEvent event = new ConfigChangeEvent(oldSnapshot, newSnapshot);
            for (ConfigChangeListener listener : listeners) {
                try {
                    listener.onConfigChanged(event);
                } catch (Exception e) {
                    // 记录异常但不影响配置更新
                    System.err.println("Error notifying config change listener: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 添加配置变更监听器
     * @param listener 监听器
     */
    public void addConfigChangeListener(ConfigChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除配置变更监听器
     * @param listener 监听器
     */
    public void removeConfigChangeListener(ConfigChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 线程安全的配置读取 - 获取配置快照
     * @return 不可变的配置快照
     */
    public ConfigSnapshot getSnapshot() {
        lock.readLock().lock();
        try {
            return new ConfigSnapshot(headLine, titleLine, isCover, cellText, foreColor, backColor);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // 单独的getter方法，使用volatile保证可见性
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
    
    // 单独的setter方法，使用写锁保护
    public void setHeadLine(int headLine) {
        if (headLine < 0) {
            throw new IllegalArgumentException("headLine must be non-negative, got: " + headLine);
        }
        
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.headLine = headLine;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    public void setTitleLine(int titleLine) {
        if (titleLine < 0) {
            throw new IllegalArgumentException("titleLine must be non-negative, got: " + titleLine);
        }
        
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.titleLine = titleLine;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    public void setIsCover(boolean isCover) {
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.isCover = isCover;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    public void setCellText(boolean cellText) {
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.cellText = cellText;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    public void setForeColor(String foreColor) {
        if (foreColor == null) {
            throw new IllegalArgumentException("foreColor cannot be null");
        }
        if (!isValidColor(foreColor)) {
            throw new IllegalArgumentException("Invalid foreColor format: " + foreColor);
        }
        
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.foreColor = foreColor;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
    
    public void setBackColor(String backColor) {
        if (backColor == null) {
            throw new IllegalArgumentException("backColor cannot be null");
        }
        if (!isValidColor(backColor)) {
            throw new IllegalArgumentException("Invalid backColor format: " + backColor);
        }
        
        ConfigSnapshot oldSnapshot = getSnapshot();
        
        lock.writeLock().lock();
        try {
            this.backColor = backColor;
        } finally {
            lock.writeLock().unlock();
        }
        
        ConfigSnapshot newSnapshot = getSnapshot();
        notifyConfigChange(oldSnapshot, newSnapshot);
    }
}