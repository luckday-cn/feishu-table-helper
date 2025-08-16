package cn.isliu.core.config;

/**
 * 配置变更监听器接口
 */
public interface ConfigChangeListener {
    
    /**
     * 配置变更时的回调方法
     * @param event 配置变更事件
     */
    void onConfigChanged(ConfigChangeEvent event);
}