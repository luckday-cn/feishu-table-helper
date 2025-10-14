package cn.isliu.core.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全的飞书客户端管理器
 * 使用ThreadLocal为每个线程维护独立的客户端实例
 */
public class FsClient implements AutoCloseable {

    private static volatile FsClient instance;
    private final ThreadLocal<FeishuClient> clientHolder = new ThreadLocal<>();
    private final Map<String, FeishuClient> clientMap = new ConcurrentHashMap<>();

    // 私有构造函数防止外部实例化
    private FsClient() {
    }

    /**
     * 获取单例实例 - 使用双重检查锁定模式
     * @return FsClient实例
     */
    public static FsClient getInstance() {
        if (instance == null) {
            synchronized (FsClient.class) {
                if (instance == null) {
                    instance = new FsClient();
                }
            }
        }
        return instance;
    }

    /**
     * 线程安全的客户端获取
     * @return FeishuClient实例
     * @throws IllegalStateException 如果客户端未初始化
     */
    public FeishuClient getClient() {
        FeishuClient currentClient = clientHolder.get();
        if (currentClient == null) {
            throw new IllegalStateException("FeishuClient not initialized. Please call initializeClient first.");
        }
        return currentClient;
    }

    /**
     * 线程安全的客户端初始化
     * 每个线程调用此方法会创建并维护自己的客户端实例
     * @param appId 飞书应用ID
     * @param appSecret 飞书应用密钥
     * @return 初始化的FeishuClient实例
     */
    public FeishuClient initializeClient(String appId, String appSecret) {
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("appId cannot be null or empty");
        }
        if (appSecret == null || appSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("appSecret cannot be null or empty");
        }
        if (clientMap.containsKey(appId + "_" + appSecret)) {
            FeishuClient feishuClient = clientMap.get(appId + "_" + appSecret);
            clientHolder.set(feishuClient);
            return feishuClient;
        } else {
            FeishuClient client = FeishuClient.newBuilder(appId, appSecret).build();
            clientMap.put(appId + "_" + appSecret, client);
            clientHolder.set(client);
            return client;
        }
    }

    /**
     * 设置客户端实例（用于外部已构建的客户端）
     * 每个线程调用此方法会设置自己的客户端实例
     * @param feishuClient 外部构建的FeishuClient实例
     */
    public void setClient(FeishuClient feishuClient) {
        if (feishuClient == null) {
            throw new IllegalArgumentException("FeishuClient cannot be null");
        }

        clientHolder.set(feishuClient);
    }

    /**
     * 检查当前线程的客户端是否已初始化
     * @return true如果当前线程客户端已初始化，否则false
     */
    public boolean isInitialized() {
        return clientHolder.get() != null;
    }

    /**
     * 清除当前线程的客户端实例（主要用于资源清理）
     */
    public void clearClient() {
        clientHolder.remove();
    }

    /**
     * 重置客户端（主要用于测试）
     */
    public synchronized void resetForTesting() {
        clientHolder.remove();
    }

    /**
     * 实现AutoCloseable接口，用于try-with-resources语句
     * 清理当前线程的客户端实例
     */
    @Override
    public void close() {
        clearClient();
    }
}