package cn.isliu.core.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 服务管理器，用于统一管理FeishuClient中的各种服务实例
 * 
 * @param <T> 服务类型
 */
class ServiceManager<T> {
    private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final FeishuClient client;

    /**
     * 构造函数
     * 
     * @param client FeishuClient实例
     */
    ServiceManager(FeishuClient client) {
        this.client = client;
    }

    /**
     * 获取指定类型的服务实例
     * 
     * @param serviceClass 服务类
     * @param supplier 服务实例提供者
     * @param <T> 服务类型
     * @return 服务实例
     */
    @SuppressWarnings("unchecked")
    <T> T getService(Class<T> serviceClass, Supplier<T> supplier) {
        return (T) services.computeIfAbsent(serviceClass, k -> supplier.get());
    }
}