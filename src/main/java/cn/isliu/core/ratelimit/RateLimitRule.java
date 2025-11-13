package cn.isliu.core.ratelimit;

import java.time.Duration;

/**
 * 频控规则定义
 *
 * <p>描述单个飞书 API 操作的频控窗口、令牌数以及串行约束等信息。</p>
 */
public class RateLimitRule {

    private final ApiOperation operation;
    private final Duration window;
    private final int permits;
    private final boolean requireDocumentLock;
    private final boolean allow429Retry;

    private RateLimitRule(Builder builder) {
        this.operation = builder.operation;
        this.window = builder.window;
        this.permits = builder.permits;
        this.requireDocumentLock = builder.requireDocumentLock;
        this.allow429Retry = builder.allow429Retry;
    }

    public ApiOperation getOperation() {
        return operation;
    }

    public Duration getWindow() {
        return window;
    }

    public int getPermits() {
        return permits;
    }

    public boolean isRequireDocumentLock() {
        return requireDocumentLock;
    }

    public boolean isAllow429Retry() {
        return allow429Retry;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ApiOperation operation;
        private Duration window = Duration.ofSeconds(1);
        private int permits = 1;
        private boolean requireDocumentLock;
        private boolean allow429Retry = true;

        public Builder operation(ApiOperation operation) {
            this.operation = operation;
            return this;
        }

        public Builder window(Duration window) {
            if (window != null && !window.isZero() && !window.isNegative()) {
                this.window = window;
            }
            return this;
        }

        public Builder permits(int permits) {
            if (permits > 0) {
                this.permits = permits;
            }
            return this;
        }

        public Builder requireDocumentLock(boolean requireDocumentLock) {
            this.requireDocumentLock = requireDocumentLock;
            return this;
        }

        public Builder allow429Retry(boolean allow429Retry) {
            this.allow429Retry = allow429Retry;
            return this;
        }

        public RateLimitRule build() {
            if (operation == null) {
                throw new IllegalArgumentException("operation must not be null");
            }
            return new RateLimitRule(this);
        }
    }
}

