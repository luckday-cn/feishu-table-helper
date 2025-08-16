package cn.isliu.core.logging;

import cn.isliu.core.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 统一日志管理器
 * 提供结构化日志记录、敏感信息脱敏、性能监控等功能
 * 
 * @author liu
 * @since 0.0.2
 */
public class FsLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(FsLogger.class);
    
    // 敏感信息脱敏模式
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
        "(appSecret|token|password|key|secret)=[^&\\s]*", 
        Pattern.CASE_INSENSITIVE
    );
    
    // 性能监控指标
    private static final Map<String, AtomicLong> performanceMetrics = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    
    // 日志级别控制
    private static volatile LogLevel minLogLevel = LogLevel.INFO;
    
    // 日志采样配置
    private static volatile int samplingRate = 1; // 1表示不采样，10表示每10条记录1条
    private static final AtomicLong logCounter = new AtomicLong(0);
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        TRACE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4);
        
        private final int level;
        
        LogLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * 记录API调用日志
     * 
     * @param operation API操作名称
     * @param params 请求参数
     * @param duration 执行时长(毫秒)
     */
    public static void apiCall(String operation, String params, long duration) {
        if (!shouldLog(LogLevel.DEBUG)) {
            return;
        }
        
        try {
            // 设置MDC上下文
            MDC.put("operation", operation);
            MDC.put("duration", String.valueOf(duration));
            
            // 更新性能指标
            updatePerformanceMetrics(operation, duration);
            
            // 记录日志
            if (logger.isDebugEnabled()) {
                logger.debug("API调用 - 操作: {} | 参数: {} | 耗时: {}ms", 
                    operation, sanitizeParams(params), duration);
            }
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 记录API调用日志（带上下文信息）
     * 
     * @param operation API操作名称
     * @param params 请求参数
     * @param duration 执行时长(毫秒)
     * @param context 上下文信息
     */
    public static void apiCall(String operation, String params, long duration, Map<String, Object> context) {
        if (!shouldLog(LogLevel.DEBUG)) {
            return;
        }
        
        try {
            // 设置MDC上下文
            MDC.put("operation", operation);
            MDC.put("duration", String.valueOf(duration));
            
            // 添加自定义上下文
            if (context != null) {
                context.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
            }
            
            // 更新性能指标
            updatePerformanceMetrics(operation, duration);
            
            // 记录日志
            if (logger.isDebugEnabled()) {
                logger.debug("API调用 - 操作: {} | 参数: {} | 耗时: {}ms | 上下文: {}", 
                    operation, sanitizeParams(params), duration, formatContext(context));
            }
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 记录错误日志
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param context 上下文信息
     * @param cause 异常原因
     */
    public static void error(ErrorCode errorCode, String message, String context, Throwable cause) {
        if (!shouldLog(LogLevel.ERROR)) {
            return;
        }
        
        try {
            // 设置MDC上下文
            MDC.put("errorCode", errorCode.getCode());
            MDC.put("errorType", errorCode.name());
            MDC.put("context", context);
            
            // 记录日志
            logger.error("错误 [{}]: {} | 上下文: {}", errorCode.getCode(), message, context, cause);
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 记录错误日志（简化版本）
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     */
    public static void error(ErrorCode errorCode, String message) {
        error(errorCode, message, null, null);
    }
    
    /**
     * 记录信息日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void info(String message, Object... args) {
        if (!shouldLog(LogLevel.INFO)) {
            return;
        }
        
        logger.info(sanitizeMessage(message), sanitizeArgs(args));
    }
    
    /**
     * 记录警告日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void warn(String message, Object... args) {
        if (!shouldLog(LogLevel.WARN)) {
            return;
        }
        
        logger.warn(sanitizeMessage(message), sanitizeArgs(args));
    }
    
    /**
     * 记录调试日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void debug(String message, Object... args) {
        if (!shouldLog(LogLevel.DEBUG)) {
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(sanitizeMessage(message), sanitizeArgs(args));
        }
    }
    
    /**
     * 记录跟踪日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void trace(String message, Object... args) {
        if (!shouldLog(LogLevel.TRACE)) {
            return;
        }
        
        if (logger.isTraceEnabled()) {
            logger.trace(sanitizeMessage(message), sanitizeArgs(args));
        }
    }
    
    /**
     * 记录性能指标
     * 
     * @param operation 操作名称
     * @param duration 执行时长
     * @param success 是否成功
     */
    public static void logPerformance(String operation, long duration, boolean success) {
        if (!shouldLog(LogLevel.INFO)) {
            return;
        }
        
        try {
            // 设置MDC上下文
            MDC.put("operation", operation);
            MDC.put("duration", String.valueOf(duration));
            MDC.put("success", String.valueOf(success));
            
            // 更新性能指标
            updatePerformanceMetrics(operation, duration);
            
            // 记录日志
            logger.info("性能指标 - 操作: {} | 耗时: {}ms | 状态: {}", 
                operation, duration, success ? "成功" : "失败");
            
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 获取性能指标
     * 
     * @return 性能指标映射
     */
    public static Map<String, Long> getPerformanceMetrics() {
        Map<String, Long> metrics = new ConcurrentHashMap<>();
        performanceMetrics.forEach((key, value) -> metrics.put(key, value.get()));
        return metrics;
    }
    
    /**
     * 获取操作计数
     * 
     * @return 操作计数映射
     */
    public static Map<String, Long> getOperationCounts() {
        Map<String, Long> counts = new ConcurrentHashMap<>();
        operationCounts.forEach((key, value) -> counts.put(key, value.get()));
        return counts;
    }
    
    /**
     * 重置性能指标
     */
    public static void resetMetrics() {
        performanceMetrics.clear();
        operationCounts.clear();
    }
    
    /**
     * 设置最小日志级别
     * 
     * @param level 日志级别
     */
    public static void setMinLogLevel(LogLevel level) {
        minLogLevel = level;
    }
    
    /**
     * 设置日志采样率
     * 
     * @param rate 采样率（1表示不采样，10表示每10条记录1条）
     */
    public static void setSamplingRate(int rate) {
        if (rate < 1) {
            throw new IllegalArgumentException("采样率必须大于等于1");
        }
        samplingRate = rate;
    }
    
    /**
     * 敏感信息脱敏
     * 
     * @param params 参数字符串
     * @return 脱敏后的参数字符串
     */
    private static String sanitizeParams(String params) {
        if (params == null || params.isEmpty()) {
            return params;
        }
        
        return SENSITIVE_PATTERN.matcher(params).replaceAll("$1=***");
    }
    
    /**
     * 消息脱敏
     * 
     * @param message 消息
     * @return 脱敏后的消息
     */
    private static String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        return SENSITIVE_PATTERN.matcher(message).replaceAll("$1=***");
    }
    
    /**
     * 参数脱敏
     * 
     * @param args 参数数组
     * @return 脱敏后的参数数组
     */
    private static Object[] sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        
        Object[] sanitizedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                sanitizedArgs[i] = sanitizeParams((String) args[i]);
            } else {
                sanitizedArgs[i] = args[i];
            }
        }
        return sanitizedArgs;
    }
    
    /**
     * 格式化上下文信息
     * 
     * @param context 上下文映射
     * @return 格式化后的上下文字符串
     */
    private static String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        context.forEach((key, value) -> {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(key).append("=").append(value);
        });
        sb.append("}");
        
        return sb.toString();
    }
    
    /**
     * 更新性能指标
     * 
     * @param operation 操作名称
     * @param duration 执行时长
     */
    private static void updatePerformanceMetrics(String operation, long duration) {
        // 更新总耗时
        performanceMetrics.computeIfAbsent(operation + "_total_duration", k -> new AtomicLong(0))
                          .addAndGet(duration);
        
        // 更新最大耗时
        performanceMetrics.computeIfAbsent(operation + "_max_duration", k -> new AtomicLong(0))
                          .updateAndGet(current -> Math.max(current, duration));
        
        // 更新操作计数
        operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0))
                       .incrementAndGet();
    }
    
    /**
     * 判断是否应该记录日志
     * 
     * @param level 日志级别
     * @return 是否应该记录
     */
    private static boolean shouldLog(LogLevel level) {
        // 检查日志级别
        if (level.getLevel() < minLogLevel.getLevel()) {
            return false;
        }
        
        // 检查采样率
        if (samplingRate > 1) {
            long count = logCounter.incrementAndGet();
            return count % samplingRate == 0;
        }
        
        return true;
    }
}