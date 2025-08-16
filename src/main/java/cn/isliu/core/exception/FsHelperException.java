package cn.isliu.core.exception;

import cn.isliu.core.enums.ErrorCode;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 飞书助手异常类
 * 
 * 增强的异常类，支持错误代码、上下文信息、异常链分析和序列化
 * 
 * @author isliu
 */
public class FsHelperException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;
    
    /** 错误代码 */
    private final ErrorCode errorCode;
    
    /** 上下文信息 */
    private final Map<String, Object> context;
    
    /** 异常唯一标识 */
    private final String exceptionId;
    
    /** 异常发生时间 */
    private final LocalDateTime timestamp;
    
    /** 用户友好的错误消息 */
    private final String userFriendlyMessage;

    /**
     * 构造函数 - 仅包含错误代码
     * 
     * @param errorCode 错误代码
     */
    public FsHelperException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null, null);
    }

    /**
     * 构造函数 - 包含错误代码和自定义消息
     * 
     * @param errorCode 错误代码
     * @param message 自定义错误消息
     */
    public FsHelperException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    /**
     * 构造函数 - 包含错误代码、消息和上下文
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param context 上下文信息
     */
    public FsHelperException(ErrorCode errorCode, String message, Map<String, Object> context) {
        this(errorCode, message, context, null);
    }

    /**
     * 构造函数 - 包含错误代码、消息和原因
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param cause 原因异常
     */
    public FsHelperException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    /**
     * 完整构造函数
     * 
     * @param errorCode 错误代码
     * @param message 错误消息
     * @param context 上下文信息
     * @param cause 原因异常
     */
    public FsHelperException(ErrorCode errorCode, String message, Map<String, Object> context, Throwable cause) {
        super(buildDetailedMessage(errorCode, message, context), cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.UNKNOWN_ERROR;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        this.exceptionId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.userFriendlyMessage = generateUserFriendlyMessage(this.errorCode, message);
        
        // 添加基本上下文信息
        this.context.put("exceptionId", this.exceptionId);
        this.context.put("timestamp", this.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        this.context.put("errorCode", this.errorCode.getCode());
        this.context.put("errorCategory", this.errorCode.getCategory().getName());
    }

    /**
     * 兼容性构造函数 - 保持向后兼容
     * 
     * @param message 错误消息
     */
    public FsHelperException(String message) {
        this(ErrorCode.UNKNOWN_ERROR, message);
    }

    /**
     * 兼容性构造函数 - 保持向后兼容
     * 
     * @param message 错误消息
     * @param cause 原因异常
     */
    public FsHelperException(String message, Throwable cause) {
        this(ErrorCode.UNKNOWN_ERROR, message, cause);
    }

    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取上下文信息
     * 
     * @return 上下文信息的副本
     */
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }

    /**
     * 获取异常唯一标识
     * 
     * @return 异常唯一标识
     */
    public String getExceptionId() {
        return exceptionId;
    }

    /**
     * 获取异常发生时间
     * 
     * @return 异常发生时间
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 获取用户友好的错误消息
     * 
     * @return 用户友好的错误消息
     */
    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    /**
     * 添加上下文信息
     * 
     * @param key 键
     * @param value 值
     * @return 当前异常实例（支持链式调用）
     */
    public FsHelperException addContext(String key, Object value) {
        if (key != null && value != null) {
            this.context.put(key, value);
        }
        return this;
    }

    /**
     * 添加多个上下文信息
     * 
     * @param contextMap 上下文信息映射
     * @return 当前异常实例（支持链式调用）
     */
    public FsHelperException addContext(Map<String, Object> contextMap) {
        if (contextMap != null) {
            this.context.putAll(contextMap);
        }
        return this;
    }

    /**
     * 获取指定键的上下文值
     * 
     * @param key 键
     * @return 上下文值，如果不存在返回null
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }

    /**
     * 检查是否包含指定的上下文键
     * 
     * @param key 键
     * @return 如果包含返回true
     */
    public boolean hasContextKey(String key) {
        return context.containsKey(key);
    }

    /**
     * 获取根因异常
     * 
     * @return 根因异常，如果没有返回当前异常
     */
    public Throwable getRootCause() {
        Throwable rootCause = this;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /**
     * 获取异常链信息
     * 
     * @return 异常链描述
     */
    public String getExceptionChain() {
        StringBuilder chain = new StringBuilder();
        Throwable current = this;
        int level = 0;
        
        while (current != null && level < 10) { // 防止无限循环
            if (level > 0) {
                chain.append("\n");
                for (int i = 0; i < level; i++) {
                    chain.append("  ");
                }
                chain.append("Caused by: ");
            }
            
            chain.append(current.getClass().getSimpleName())
                 .append(": ")
                 .append(current.getMessage());
            
            current = current.getCause();
            level++;
        }
        
        return chain.toString();
    }

    /**
     * 检查是否为可重试的异常
     * 
     * @return 如果可重试返回true
     */
    public boolean isRetryable() {
        return errorCode.isRetryable();
    }

    /**
     * 检查是否为致命异常
     * 
     * @return 如果是致命异常返回true
     */
    public boolean isFatal() {
        return errorCode.isFatal();
    }

    /**
     * 检查是否为客户端异常
     * 
     * @return 如果是客户端异常返回true
     */
    public boolean isClientError() {
        return errorCode.isClientError();
    }

    /**
     * 检查是否为服务器异常
     * 
     * @return 如果是服务器异常返回true
     */
    public boolean isServerError() {
        return errorCode.isServerError();
    }

    /**
     * 获取异常的详细信息（用于日志记录）
     * 
     * @return 详细信息字符串
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Exception Details:\n");
        info.append("  ID: ").append(exceptionId).append("\n");
        info.append("  Timestamp: ").append(timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        info.append("  Error Code: ").append(errorCode.getCode()).append("\n");
        info.append("  Category: ").append(errorCode.getCategory().getName()).append("\n");
        info.append("  Message: ").append(getMessage()).append("\n");
        info.append("  User Friendly Message: ").append(userFriendlyMessage).append("\n");
        info.append("  Retryable: ").append(isRetryable()).append("\n");
        info.append("  Fatal: ").append(isFatal()).append("\n");
        
        if (!context.isEmpty()) {
            info.append("  Context:\n");
            context.forEach((key, value) -> 
                info.append("    ").append(key).append(": ").append(value).append("\n"));
        }
        
        if (getCause() != null) {
            info.append("  Exception Chain:\n");
            info.append("    ").append(getExceptionChain().replace("\n", "\n    "));
        }
        
        return info.toString();
    }

    /**
     * 构建详细的错误消息
     * 
     * @param errorCode 错误代码
     * @param message 原始消息
     * @param context 上下文信息
     * @return 详细的错误消息
     */
    private static String buildDetailedMessage(ErrorCode errorCode, String message, Map<String, Object> context) {
        StringBuilder detailedMessage = new StringBuilder();
        
        if (errorCode != null) {
            detailedMessage.append("[").append(errorCode.getCode()).append("] ");
        }
        
        if (message != null && !message.trim().isEmpty()) {
            detailedMessage.append(message);
        } else if (errorCode != null) {
            detailedMessage.append(errorCode.getDefaultMessage());
        } else {
            detailedMessage.append("Unknown error occurred");
        }
        
        if (context != null && !context.isEmpty()) {
            detailedMessage.append(" (Context: ");
            context.entrySet().stream()
                   .filter(entry -> !"exceptionId".equals(entry.getKey()) && 
                                   !"timestamp".equals(entry.getKey()) &&
                                   !"errorCode".equals(entry.getKey()) &&
                                   !"errorCategory".equals(entry.getKey()))
                   .forEach(entry -> detailedMessage.append(entry.getKey())
                                                   .append("=")
                                                   .append(entry.getValue())
                                                   .append(", "));
            
            if (detailedMessage.toString().endsWith(", ")) {
                detailedMessage.setLength(detailedMessage.length() - 2);
            }
            detailedMessage.append(")");
        }
        
        return detailedMessage.toString();
    }

    /**
     * 生成用户友好的错误消息
     * 
     * @param errorCode 错误代码
     * @param originalMessage 原始消息
     * @return 用户友好的错误消息
     */
    private static String generateUserFriendlyMessage(ErrorCode errorCode, String originalMessage) {
        if (errorCode == null) {
            return "系统发生未知错误，请稍后重试或联系技术支持。";
        }
        
        // 获取国际化的用户友好消息
        String friendlyMessage = errorCode.getMessage();
        
        // 根据错误类型提供不同的用户友好消息
        switch (errorCode.getCategory()) {
            case CLIENT:
                return "客户端配置问题：" + friendlyMessage + "。请检查配置后重试。";
            case API:
                return "服务调用失败：" + friendlyMessage + "。请稍后重试或联系技术支持。";
            case CONFIGURATION:
                return "配置错误：" + friendlyMessage + "。请检查配置文件。";
            case SECURITY:
                return "安全验证失败：" + friendlyMessage + "。请检查认证信息。";
            case RESOURCE:
                return "资源不足：" + friendlyMessage + "。请稍后重试。";
            case DATA:
                return "数据处理错误：" + friendlyMessage + "。请检查输入数据。";
            case BUSINESS:
                return "业务规则错误：" + friendlyMessage + "。请检查操作是否符合业务要求。";
            case SYSTEM:
                return "系统错误：" + friendlyMessage + "。请联系技术支持。";
            default:
                return friendlyMessage + "。如问题持续，请联系技术支持。";
        }
    }

    /**
     * 创建构建器
     * 
     * @param errorCode 错误代码
     * @return 异常构建器
     */
    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }

    /**
     * 异常构建器类
     */
    public static class Builder {
        private final ErrorCode errorCode;
        private String message;
        private Map<String, Object> context = new HashMap<>();
        private Throwable cause;

        private Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder context(Map<String, Object> context) {
            if (context != null) {
                this.context.putAll(context);
            }
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public FsHelperException build() {
            return new FsHelperException(errorCode, message, context, cause);
        }
    }
}
