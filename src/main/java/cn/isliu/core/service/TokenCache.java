package cn.isliu.core.service;

import cn.isliu.core.pojo.TokenInfo;

/**
 * Token缓存组件
 * 
 * 提供线程安全的TokenInfo存储和访问功能。使用volatile关键字确保在多线程环境中
 * 对token缓存的读写操作具有可见性保证。
 * 
 * 该类设计为轻量级缓存，专门用于存储单个TokenInfo实例。通过volatile字段
 * 确保线程间的可见性，配合外部的同步机制（如读写锁）实现完整的线程安全。
 * 
 * 线程安全机制：
 * - volatile字段确保内存可见性，防止CPU缓存导致的数据不一致
 * - 配合外部ReentrantReadWriteLock实现原子性操作
 * - 所有操作都是原子的引用赋值，避免数据竞争
 * 
 * @author FsHelper
 * @since 1.0
 */
public class TokenCache {

    /**
     * 当前缓存的token信息
     * 
     * 使用volatile关键字确保多线程环境下的可见性：
     * - 当一个线程修改currentToken时，其他线程能立即看到最新值
     * - 防止CPU缓存导致的数据不一致问题
     * - 与外部同步机制配合使用，确保完整的线程安全
     */
    private volatile TokenInfo currentToken;

    /**
     * 获取当前缓存的token信息
     * 
     * @return 当前缓存的TokenInfo实例，如果缓存为空则返回null
     */
    public TokenInfo get() {
        return currentToken;
    }

    /**
     * 设置新的token信息到缓存
     * 
     * 该操作是原子的，由于使用volatile字段，设置操作对所有线程立即可见。
     * 通常在外部写锁保护下调用，确保与其他操作的原子性。
     * 
     * @param tokenInfo 要缓存的TokenInfo实例，可以为null
     */
    public void set(TokenInfo tokenInfo) {
        this.currentToken = tokenInfo;
    }

    /**
     * 清空token缓存
     * 
     * 将缓存设置为null，通常在token获取失败或需要强制刷新时调用。
     * 该操作是原子的，由于使用volatile字段，清空操作对所有线程立即可见。
     */
    public void clear() {
        this.currentToken = null;
    }

    /**
     * 检查缓存是否为空
     * 
     * @return true表示缓存为空，false表示缓存中有token信息
     */
    public boolean isEmpty() {
        return currentToken == null;
    }

    /**
     * 检查缓存中的token是否有效
     * 
     * @return true表示缓存中有token且仍然有效，false表示缓存为空或token已过期
     */
    public boolean hasValidToken() {
        TokenInfo token = currentToken;
        return token != null && token.isValid();
    }

    /**
     * 原子性地比较并设置token
     * 
     * 该方法提供了一种安全的方式来更新token，只有当当前token与期望值相同时才进行更新。
     * 虽然volatile保证了可见性，但这个方法在某些高级并发场景下可能有用。
     * 
     * 注意：这个方法不是真正的CAS操作，因为Java对象引用的比较和设置不是原子的。
     * 在实际使用中，应该依赖外部的同步机制（如写锁）来确保原子性。
     * 
     * @param expected 期望的当前token值
     * @param newToken 要设置的新token值
     * @return true表示更新成功，false表示当前token与期望值不同
     */
    public boolean compareAndSet(TokenInfo expected, TokenInfo newToken) {
        if (currentToken == expected) {
            currentToken = newToken;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        TokenInfo token = currentToken;
        return "TokenCache{" +
                "currentToken=" + (token != null ? token.toString() : "null") +
                '}';
    }
}