package cn.isliu.core.client;

/**
 * 线程安全的飞书客户端管理器
 * 使用双重检查锁定单例模式确保线程安全
 */
public class FsClient {
    
    private static volatile FsClient instance;
    private volatile FeishuClient client;
    private final Object lock = new Object();
    
    // 私有构造函数防止外部实例化
    private FsClient() {
    }
    
    /**
     * 获取单例实例 - 使用双重检查锁定模式
     * @return FeishuClientManager实例
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
        FeishuClient currentClient = client;
        if (currentClient == null) {
            throw new IllegalStateException("FeishuClient not initialized. Please call initializeClient first.");
        }
        return currentClient;
    }
    
    /**
     * 线程安全的客户端初始化
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
        
        if (client == null) {
            synchronized (lock) {
                if (client == null) {
                    client = FeishuClient.newBuilder(appId, appSecret).build();
                }
            }
        }
        return client;
    }
    
    /**
     * 设置客户端实例（用于外部已构建的客户端）
     * @param feishuClient 外部构建的FeishuClient实例
     */
    public void setClient(FeishuClient feishuClient) {
        if (feishuClient == null) {
            throw new IllegalArgumentException("FeishuClient cannot be null");
        }
        
        synchronized (lock) {
            this.client = feishuClient;
        }
    }
    
    /**
     * 检查客户端是否已初始化
     * @return true如果客户端已初始化，否则false
     */
    public boolean isInitialized() {
        return client != null;
    }
    
    /**
     * 重置客户端（主要用于测试）
     */
    public synchronized void resetForTesting() {
        client = null;
    }
}