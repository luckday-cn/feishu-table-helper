package cn.isliu.core.exception;

import cn.isliu.core.enums.ErrorCode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一异常处理器
 * 
 * 提供全局异常处理、异常分类转换、统计监控和恢复建议功能
 * 
 * @author isliu
 */
public class ExceptionHandler {

    private static volatile ExceptionHandler instance;
    private static final Object lock = new Object();

    /** 异常统计计数器 */
    private final Map<ErrorCode, LongAdder> exceptionCounters = new ConcurrentHashMap<>();
    
    /** 异常分类统计 */
    private final Map<ErrorCode.ErrorCategory, LongAdder> categoryCounters = new ConcurrentHashMap<>();
    
    /** 最近异常记录 */
    private final List<ExceptionRecord> recentExceptions = Collections.synchronizedList(new ArrayList<>());
    
    /** 最大记录数量 */
    private static final int MAX_RECENT_EXCEPTIONS = 100;
    
    /** 异常处理监听器 */
    private final List<ExceptionListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * 私有构造函数
     */
    private ExceptionHandler() {
        // 初始化所有错误代码的计数器
        for (ErrorCode errorCode : ErrorCode.values()) {
            exceptionCounters.put(errorCode, new LongAdder());
        }
        
        // 初始化所有分类的计数器
        for (ErrorCode.ErrorCategory category : ErrorCode.ErrorCategory.values()) {
            categoryCounters.put(category, new LongAdder());
        }
    }

    /**
     * 获取单例实例
     * 
     * @return 异常处理器实例
     */
    public static ExceptionHandler getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ExceptionHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 处理异常
     * 
     * @param throwable 原始异常
     * @return 处理后的FsHelperException
     */
    public FsHelperException handleException(Throwable throwable) {
        if (throwable == null) {
            return new FsHelperException(ErrorCode.UNKNOWN_ERROR, "Null exception occurred");
        }

        FsHelperException fsException;
        
        // 如果已经是FsHelperException，直接使用
        if (throwable instanceof FsHelperException) {
            fsException = (FsHelperException) throwable;
        } else {
            // 转换为FsHelperException
            fsException = convertToFsHelperException(throwable);
        }

        // 记录异常统计
        recordException(fsException);
        
        // 通知监听器
        notifyListeners(fsException);
        
        return fsException;
    }

    /**
     * 处理异常并提供上下文
     * 
     * @param throwable 原始异常
     * @param context 上下文信息
     * @return 处理后的FsHelperException
     */
    public FsHelperException handleException(Throwable throwable, Map<String, Object> context) {
        FsHelperException fsException = handleException(throwable);
        
        if (context != null && !context.isEmpty()) {
            fsException.addContext(context);
        }
        
        return fsException;
    }

    /**
     * 处理异常并提供操作上下文
     * 
     * @param throwable 原始异常
     * @param operation 操作名称
     * @param additionalInfo 附加信息
     * @return 处理后的FsHelperException
     */
    public FsHelperException handleException(Throwable throwable, String operation, String additionalInfo) {
        FsHelperException fsException = handleException(throwable);
        
        fsException.addContext("operation", operation);
        if (additionalInfo != null) {
            fsException.addContext("additionalInfo", additionalInfo);
        }
        
        return fsException;
    }

    /**
     * 将普通异常转换为FsHelperException
     * 
     * @param throwable 原始异常
     * @return FsHelperException
     */
    private FsHelperException convertToFsHelperException(Throwable throwable) {
        ErrorCode errorCode = classifyException(throwable);
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
        
        return FsHelperException.builder(errorCode)
            .message(message)
            .context("originalExceptionType", throwable.getClass().getSimpleName())
            .context("originalMessage", throwable.getMessage())
            .cause(throwable)
            .build();
    }

    /**
     * 异常分类逻辑
     * 
     * @param throwable 异常
     * @return 对应的错误代码
     */
    private ErrorCode classifyException(Throwable throwable) {
        if (throwable == null) {
            return ErrorCode.UNKNOWN_ERROR;
        }

        String className = throwable.getClass().getSimpleName().toLowerCase();
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";

        // 网络和连接相关异常
        if (className.contains("connect") || className.contains("socket") || className.contains("timeout")) {
            if (message.contains("timeout")) {
                return ErrorCode.CLIENT_TIMEOUT;
            }
            return ErrorCode.CLIENT_CONNECTION_FAILED;
        }

        // 认证和授权异常
        if (className.contains("auth") || className.contains("credential") || className.contains("permission") ||
            message.contains("auth") || message.contains("credential") || message.contains("permission")) {
            if (message.contains("expired") || message.contains("token")) {
                return ErrorCode.TOKEN_EXPIRED;
            }
            if (message.contains("unauthorized") || message.contains("401")) {
                return ErrorCode.API_UNAUTHORIZED;
            }
            if (message.contains("forbidden") || message.contains("403")) {
                return ErrorCode.API_FORBIDDEN;
            }
            return ErrorCode.INVALID_CREDENTIALS;
        }

        // HTTP相关异常
        if (className.contains("http") || message.contains("http")) {
            if (message.contains("404") || message.contains("not found")) {
                return ErrorCode.API_NOT_FOUND;
            }
            if (message.contains("429") || message.contains("rate limit")) {
                return ErrorCode.API_RATE_LIMIT_EXCEEDED;
            }
            if (message.contains("500") || message.contains("502") || message.contains("503")) {
                return ErrorCode.API_SERVER_ERROR;
            }
            if (message.contains("400") || message.contains("bad request")) {
                return ErrorCode.API_INVALID_REQUEST;
            }
            return ErrorCode.API_CALL_FAILED;
        }

        // 并发相关异常 - 需要优先检查，因为ConcurrentModificationException包含"modification"
        if (className.equals("concurrentmodificationexception")) {
            return ErrorCode.CONCURRENT_MODIFICATION;
        }
        if (className.contains("concurrent") || className.contains("thread") || className.contains("lock")) {
            return ErrorCode.THREAD_SAFETY_VIOLATION;
        }

        // 数据相关异常
        if (className.contains("parse") || className.contains("json") || className.contains("xml")) {
            return ErrorCode.API_RESPONSE_PARSE_ERROR;
        }

        if (className.contains("validation") || className.contains("illegal") || className.contains("invalid")) {
            return ErrorCode.DATA_VALIDATION_FAILED;
        }

        if (className.contains("format") || className.contains("number") || className.contains("date")) {
            return ErrorCode.DATA_FORMAT_ERROR;
        }

        // 资源相关异常
        if (className.contains("memory") || message.contains("out of memory")) {
            return ErrorCode.MEMORY_INSUFFICIENT;
        }

        if (className.contains("file") || className.equals("ioexception") || className.startsWith("io")) {
            if (message.contains("not found") || message.contains("no such file")) {
                return ErrorCode.FILE_NOT_FOUND;
            }
            if (message.contains("access denied") || message.contains("permission")) {
                return ErrorCode.FILE_ACCESS_DENIED;
            }
            return ErrorCode.RESOURCE_EXHAUSTED;
        }

        // 配置相关异常
        if (className.contains("config") || className.contains("property") || className.contains("setting")) {
            return ErrorCode.CONFIGURATION_ERROR;
        }

        // 业务逻辑异常
        if (className.contains("state") || className.contains("operation")) {
            return ErrorCode.INVALID_OPERATION;
        }

        // 系统异常 - 更精确的匹配
        if (className.equals("runtimeexception") || className.contains("system")) {
            return ErrorCode.SYSTEM_ERROR;
        }

        // 默认未知错误
        return ErrorCode.UNKNOWN_ERROR;
    }

    /**
     * 记录异常统计
     * 
     * @param exception 异常
     */
    private void recordException(FsHelperException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ErrorCode.ErrorCategory category = errorCode.getCategory();

        // 增加计数
        exceptionCounters.get(errorCode).increment();
        categoryCounters.get(category).increment();

        // 记录最近异常
        ExceptionRecord record = new ExceptionRecord(
            exception.getExceptionId(),
            errorCode,
            exception.getMessage(),
            exception.getUserFriendlyMessage(),
            LocalDateTime.now(),
            exception.getContext()
        );

        synchronized (recentExceptions) {
            recentExceptions.add(record);
            // 保持最大记录数量
            if (recentExceptions.size() > MAX_RECENT_EXCEPTIONS) {
                recentExceptions.remove(0);
            }
        }
    }

    /**
     * 通知异常监听器
     * 
     * @param exception 异常
     */
    private void notifyListeners(FsHelperException exception) {
        for (ExceptionListener listener : listeners) {
            try {
                listener.onException(exception);
            } catch (Exception e) {
                // 监听器异常不应影响主流程，只记录日志
                System.err.println("Exception listener failed: " + e.getMessage());
            }
        }
    }

    /**
     * 获取异常统计信息
     * 
     * @return 异常统计信息
     */
    public ExceptionStatistics getStatistics() {
        Map<ErrorCode, Long> errorCodeCounts = new ConcurrentHashMap<>();
        Map<ErrorCode.ErrorCategory, Long> categoryCounts = new ConcurrentHashMap<>();

        for (Map.Entry<ErrorCode, LongAdder> entry : exceptionCounters.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                errorCodeCounts.put(entry.getKey(), count);
            }
        }

        for (Map.Entry<ErrorCode.ErrorCategory, LongAdder> entry : categoryCounters.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                categoryCounts.put(entry.getKey(), count);
            }
        }

        return new ExceptionStatistics(errorCodeCounts, categoryCounts, new ArrayList<>(recentExceptions));
    }

    /**
     * 获取恢复建议
     * 
     * @param exception 异常
     * @return 恢复建议
     */
    public RecoveryAdvice getRecoveryAdvice(FsHelperException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        
        RecoveryAdvice.Builder builder = RecoveryAdvice.builder()
            .errorCode(errorCode)
            .isRetryable(errorCode.isRetryable())
            .isFatal(errorCode.isFatal());

        // 根据错误类型提供具体建议
        switch (errorCode.getCategory()) {
            case CLIENT:
                return builder
                    .immediateAction("检查客户端配置和初始化参数")
                    .longTermAction("确保客户端正确初始化并配置有效的认证信息")
                    .preventiveAction("在使用客户端前进行初始化检查")
                    .build();

            case API:
                if (errorCode.isRetryable()) {
                    return builder
                        .immediateAction("等待一段时间后重试")
                        .longTermAction("实现指数退避重试策略")
                        .preventiveAction("监控API调用频率，避免超过限制")
                        .build();
                } else {
                    return builder
                        .immediateAction("检查API请求参数和格式")
                        .longTermAction("验证API权限和认证信息")
                        .preventiveAction("在发送请求前验证参数完整性")
                        .build();
                }

            case CONFIGURATION:
                return builder
                    .immediateAction("检查配置文件的格式和内容")
                    .longTermAction("建立配置验证机制")
                    .preventiveAction("使用配置模板和验证规则")
                    .build();

            case SECURITY:
                return builder
                    .immediateAction("检查认证凭据是否有效")
                    .longTermAction("实现凭据自动刷新机制")
                    .preventiveAction("定期更新和验证安全凭据")
                    .build();

            case RESOURCE:
                return builder
                    .immediateAction("释放不必要的资源")
                    .longTermAction("优化资源使用策略")
                    .preventiveAction("实现资源监控和预警机制")
                    .build();

            case DATA:
                return builder
                    .immediateAction("验证输入数据的格式和内容")
                    .longTermAction("加强数据验证和清理机制")
                    .preventiveAction("在处理前进行数据格式检查")
                    .build();

            case CONCURRENCY:
                return builder
                    .immediateAction("检查并发访问的同步机制")
                    .longTermAction("重新设计线程安全的数据结构")
                    .preventiveAction("使用线程安全的组件和模式")
                    .build();

            case BUSINESS:
                return builder
                    .immediateAction("检查业务规则和前置条件")
                    .longTermAction("完善业务逻辑验证")
                    .preventiveAction("在操作前验证业务规则")
                    .build();

            case SYSTEM:
                return builder
                    .immediateAction("检查系统状态和资源可用性")
                    .longTermAction("实现系统监控和告警")
                    .preventiveAction("定期进行系统健康检查")
                    .build();

            default:
                return builder
                    .immediateAction("查看详细错误信息和日志")
                    .longTermAction("联系技术支持获取帮助")
                    .preventiveAction("加强错误处理和日志记录")
                    .build();
        }
    }

    /**
     * 添加异常监听器
     * 
     * @param listener 监听器
     */
    public void addListener(ExceptionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 移除异常监听器
     * 
     * @param listener 监听器
     */
    public void removeListener(ExceptionListener listener) {
        listeners.remove(listener);
    }

    /**
     * 清除统计信息
     */
    public void clearStatistics() {
        exceptionCounters.values().forEach(LongAdder::reset);
        categoryCounters.values().forEach(LongAdder::reset);
        recentExceptions.clear();
    }

    /**
     * 异常记录
     */
    public static class ExceptionRecord {
        private final String exceptionId;
        private final ErrorCode errorCode;
        private final String message;
        private final String userFriendlyMessage;
        private final LocalDateTime timestamp;
        private final Map<String, Object> context;

        public ExceptionRecord(String exceptionId, ErrorCode errorCode, String message, 
                             String userFriendlyMessage, LocalDateTime timestamp, Map<String, Object> context) {
            this.exceptionId = exceptionId;
            this.errorCode = errorCode;
            this.message = message;
            this.userFriendlyMessage = userFriendlyMessage;
            this.timestamp = timestamp;
            this.context = new ConcurrentHashMap<>(context != null ? context : new ConcurrentHashMap<>());
        }

        public String getExceptionId() { return exceptionId; }
        public ErrorCode getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getUserFriendlyMessage() { return userFriendlyMessage; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getContext() { return new ConcurrentHashMap<>(context); }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s (%s)", 
                timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                errorCode.getCode(), 
                message, 
                exceptionId);
        }
    }

    /**
     * 异常统计信息
     */
    public static class ExceptionStatistics {
        private final Map<ErrorCode, Long> errorCodeCounts;
        private final Map<ErrorCode.ErrorCategory, Long> categoryCounts;
        private final List<ExceptionRecord> recentExceptions;

        public ExceptionStatistics(Map<ErrorCode, Long> errorCodeCounts, 
                                 Map<ErrorCode.ErrorCategory, Long> categoryCounts,
                                 List<ExceptionRecord> recentExceptions) {
            this.errorCodeCounts = new ConcurrentHashMap<>(errorCodeCounts);
            this.categoryCounts = new ConcurrentHashMap<>(categoryCounts);
            this.recentExceptions = new ArrayList<>(recentExceptions);
        }

        public Map<ErrorCode, Long> getErrorCodeCounts() { return new ConcurrentHashMap<>(errorCodeCounts); }
        public Map<ErrorCode.ErrorCategory, Long> getCategoryCounts() { return new ConcurrentHashMap<>(categoryCounts); }
        public List<ExceptionRecord> getRecentExceptions() { return new ArrayList<>(recentExceptions); }

        public long getTotalExceptions() {
            return errorCodeCounts.values().stream().mapToLong(Long::longValue).sum();
        }

        public ErrorCode getMostFrequentError() {
            return errorCodeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }

        public ErrorCode.ErrorCategory getMostFrequentCategory() {
            return categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
    }

    /**
     * 恢复建议
     */
    public static class RecoveryAdvice {
        private final ErrorCode errorCode;
        private final boolean isRetryable;
        private final boolean isFatal;
        private final String immediateAction;
        private final String longTermAction;
        private final String preventiveAction;

        private RecoveryAdvice(Builder builder) {
            this.errorCode = builder.errorCode;
            this.isRetryable = builder.isRetryable;
            this.isFatal = builder.isFatal;
            this.immediateAction = builder.immediateAction;
            this.longTermAction = builder.longTermAction;
            this.preventiveAction = builder.preventiveAction;
        }

        public ErrorCode getErrorCode() { return errorCode; }
        public boolean isRetryable() { return isRetryable; }
        public boolean isFatal() { return isFatal; }
        public String getImmediateAction() { return immediateAction; }
        public String getLongTermAction() { return longTermAction; }
        public String getPreventiveAction() { return preventiveAction; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ErrorCode errorCode;
            private boolean isRetryable;
            private boolean isFatal;
            private String immediateAction;
            private String longTermAction;
            private String preventiveAction;

            public Builder errorCode(ErrorCode errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder isRetryable(boolean isRetryable) {
                this.isRetryable = isRetryable;
                return this;
            }

            public Builder isFatal(boolean isFatal) {
                this.isFatal = isFatal;
                return this;
            }

            public Builder immediateAction(String immediateAction) {
                this.immediateAction = immediateAction;
                return this;
            }

            public Builder longTermAction(String longTermAction) {
                this.longTermAction = longTermAction;
                return this;
            }

            public Builder preventiveAction(String preventiveAction) {
                this.preventiveAction = preventiveAction;
                return this;
            }

            public RecoveryAdvice build() {
                return new RecoveryAdvice(this);
            }
        }
    }

    /**
     * 异常监听器接口
     */
    public interface ExceptionListener {
        /**
         * 异常发生时的回调
         * 
         * @param exception 异常
         */
        void onException(FsHelperException exception);
    }
}