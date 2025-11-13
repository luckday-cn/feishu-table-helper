package cn.isliu.core.ratelimit;

import com.google.common.util.concurrent.RateLimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飞书频控管理器
 *
 * <p>基于租户/应用维度缓存 {@link RateLimiter}，实现线程安全的限流。</p>
 */
public class FeishuRateLimiterManager {

    private final Map<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    public RateLimiter getLimiter(String tenantKey, RateLimitRule rule) {
        String cacheKey = tenantKey + ":" + rule.getOperation().name();
        return limiterCache.computeIfAbsent(cacheKey, key -> createLimiter(rule));
    }

    public void adjustRate(String tenantKey, ApiOperation operation, double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            return;
        }
        String cacheKey = tenantKey + ":" + operation.name();
        RateLimiter limiter = limiterCache.get(cacheKey);
        if (limiter != null) {
            limiter.setRate(Math.max(0.1d, permitsPerSecond));
        }
    }

    private RateLimiter createLimiter(RateLimitRule rule) {
        double permitsPerSecond = calculatePermitsPerSecond(rule);
        return RateLimiter.create(permitsPerSecond);
    }

    private double calculatePermitsPerSecond(RateLimitRule rule) {
        double seconds = rule.getWindow().toMillis() / 1000.0d;
        if (seconds <= 0) {
            seconds = 1;
        }
        return Math.max(0.1d, rule.getPermits() / seconds);
    }
}

