package cn.isliu.core.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文档级别锁注册表
 *
 * <p>对于“单个文档只能串行调用”的操作，通过文档 token 获取同一把锁。</p>
 */
public class DocumentLockRegistry {

    private final ConcurrentMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public ReentrantLock acquireLock(String spreadsheetToken) {
        if (spreadsheetToken == null || spreadsheetToken.isEmpty()) {
            return null;
        }
        return lockMap.computeIfAbsent(spreadsheetToken, key -> new ReentrantLock(true));
    }

    public void releaseLock(String spreadsheetToken, ReentrantLock lock) {
        if (spreadsheetToken == null || lock == null) {
            return;
        }
        if (!lock.isLocked()) {
            lockMap.remove(spreadsheetToken, lock);
        }
    }
}

