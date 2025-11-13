package cn.isliu.core.ratelimit;

import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.exception.FsHelperException;
import cn.isliu.core.logging.FsLogger;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 飞书 API 调用执行器
 *
 * <p>统一处理限流、429 重试、串行锁等逻辑。</p>
 */
public class FeishuApiExecutor {

    private static final int MAX_RETRY = 3;
    private static final long BASE_BACKOFF_MILLIS = 200L;

    private final FeishuRateLimiterManager limiterManager;
    private final DocumentLockRegistry documentLockRegistry;

    public FeishuApiExecutor(FeishuRateLimiterManager limiterManager,
                             DocumentLockRegistry documentLockRegistry) {
        this.limiterManager = limiterManager;
        this.documentLockRegistry = documentLockRegistry;
    }

    public <T> T execute(String tenantKey,
                         ApiOperation operation,
                         String spreadsheetToken,
                         CheckedCallable<T> action) throws Exception {

        RateLimitRule rule = operation != null ? operation.getRule() : ApiOperation.GENERIC_OPERATION.getRule();
        ApiOperation op = operation != null ? operation : ApiOperation.GENERIC_OPERATION;

        limiterManager.getLimiter(tenantKey, rule).acquire();

        ReentrantLock lock = null;
        if (rule.isRequireDocumentLock()) {
            lock = documentLockRegistry.acquireLock(spreadsheetToken);
            if (lock != null) {
                lock.lock();
            }
        }

        try {
            return executeWithRetry(tenantKey, op, rule, action);
        } finally {
            if (lock != null) {
                lock.unlock();
                documentLockRegistry.releaseLock(spreadsheetToken, lock);
            }
        }
    }

    private <T> T executeWithRetry(String tenantKey,
                                   ApiOperation operation,
                                   RateLimitRule rule,
                                   CheckedCallable<T> action) throws Exception {
        int attempt = 0;
        long backoff = BASE_BACKOFF_MILLIS;

        while (true) {
            attempt++;
            try {
                return action.call();
            } catch (FsHelperException ex) {
                if (rule.isAllow429Retry() && isRateLimitException(ex) && attempt <= MAX_RETRY) {
                    long waitMillis = resolveWaitMillis(ex, backoff, attempt);
                    FsLogger.warn("【飞书表格】触发限流，operation:{}，attempt:{}，等待{}ms",
                            operation.name(), attempt, waitMillis);
                    sleepQuietly(waitMillis);
                    adjustLimiter(tenantKey, operation, waitMillis);
                    continue;
                }
                throw ex;
            } catch (IOException io) {
                throw io;
            } catch (Exception ex) {
                if (ex instanceof RuntimeException) {
                    throw ex;
                }
                throw new FsHelperException(ErrorCode.API_CALL_FAILED, "飞书 API 调用异常", null, ex);
            }
        }
    }

    private boolean isRateLimitException(FsHelperException ex) {
        if (ex == null) {
            return false;
        }
        Object status = ex.getContextValue("httpStatus");
        if (status instanceof Number && ((Number) status).intValue() == 429) {
            return true;
        }
        if (status instanceof String) {
            try {
                if (Integer.parseInt((String) status) == 429) {
                    return true;
                }
            } catch (NumberFormatException ignore) {
                // ignore
            }
        }
        return ex.getMessage() != null && ex.getMessage().contains("429");
    }

    private long resolveWaitMillis(FsHelperException ex, long baseBackoff, int attempt) {
        Object reset = ex.getContextValue("x-ogw-ratelimit-reset");
        if (reset instanceof Number) {
            return Duration.ofSeconds(((Number) reset).longValue()).toMillis();
        }
        if (reset instanceof String) {
            try {
                long seconds = Long.parseLong((String) reset);
                if (seconds > 0) {
                    return Duration.ofSeconds(seconds).toMillis();
                }
            } catch (NumberFormatException ignore) {
                // ignore
            }
        }
        long exponential = (long) (baseBackoff * Math.pow(2, Math.max(0, attempt - 1)));
        return Math.min(TimeUnit.SECONDS.toMillis(10), exponential);
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw FsHelperException.builder(ErrorCode.API_CALL_FAILED)
                    .message("限流等待被中断")
                    .cause(e)
                    .build();
        }
    }

    private void adjustLimiter(String tenantKey, ApiOperation operation, long waitMillis) {
        if (waitMillis <= 0) {
            return;
        }
        double permitsPerSecond = 1000.0d / waitMillis;
        limiterManager.adjustRate(tenantKey, operation, permitsPerSecond);
    }

    @FunctionalInterface
    public interface CheckedCallable<T> extends Callable<T> {
        @Override
        T call() throws Exception;
    }
}

