package cn.isliu.core.enums;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 错误代码枚举
 * 
 * 定义标准化的错误代码，包含错误描述和分类信息，支持国际化错误消息
 * 
 * @author isliu
 */
public enum ErrorCode implements BaseEnum {

    // 客户端相关错误 (FS001-FS099)
    CLIENT_NOT_INITIALIZED("FS001", "Client not initialized", ErrorCategory.CLIENT),
    CLIENT_INITIALIZATION_FAILED("FS002", "Client initialization failed", ErrorCategory.CLIENT),
    CLIENT_CONNECTION_FAILED("FS003", "Client connection failed", ErrorCategory.CLIENT),
    CLIENT_AUTHENTICATION_FAILED("FS004", "Client authentication failed", ErrorCategory.CLIENT),
    CLIENT_TIMEOUT("FS005", "Client operation timeout", ErrorCategory.CLIENT),

    // API调用相关错误 (FS100-FS199)
    API_CALL_FAILED("FS100", "API call failed", ErrorCategory.API),
    API_RATE_LIMIT_EXCEEDED("FS101", "API rate limit exceeded", ErrorCategory.API),
    API_INVALID_REQUEST("FS102", "Invalid API request", ErrorCategory.API),
    API_UNAUTHORIZED("FS103", "API unauthorized access", ErrorCategory.API),
    API_FORBIDDEN("FS104", "API access forbidden", ErrorCategory.API),
    API_NOT_FOUND("FS105", "API resource not found", ErrorCategory.API),
    API_SERVER_ERROR("FS106", "API server error", ErrorCategory.API),
    API_RESPONSE_PARSE_ERROR("FS107", "API response parse error", ErrorCategory.API),

    // 线程安全相关错误 (FS200-FS299)
    THREAD_SAFETY_VIOLATION("FS200", "Thread safety violation", ErrorCategory.CONCURRENCY),
    CONCURRENT_MODIFICATION("FS201", "Concurrent modification detected", ErrorCategory.CONCURRENCY),
    DEADLOCK_DETECTED("FS202", "Deadlock detected", ErrorCategory.CONCURRENCY),
    RACE_CONDITION("FS203", "Race condition occurred", ErrorCategory.CONCURRENCY),

    // 配置相关错误 (FS300-FS399)
    CONFIGURATION_ERROR("FS300", "Configuration error", ErrorCategory.CONFIGURATION),
    INVALID_CONFIGURATION("FS301", "Invalid configuration", ErrorCategory.CONFIGURATION),
    CONFIGURATION_NOT_FOUND("FS302", "Configuration not found", ErrorCategory.CONFIGURATION),
    CONFIGURATION_PARSE_ERROR("FS303", "Configuration parse error", ErrorCategory.CONFIGURATION),
    CONFIGURATION_VALIDATION_FAILED("FS304", "Configuration validation failed", ErrorCategory.CONFIGURATION),

    // 资源相关错误 (FS400-FS499)
    RESOURCE_EXHAUSTED("FS400", "Resource exhausted", ErrorCategory.RESOURCE),
    MEMORY_INSUFFICIENT("FS401", "Insufficient memory", ErrorCategory.RESOURCE),
    CONNECTION_POOL_EXHAUSTED("FS402", "Connection pool exhausted", ErrorCategory.RESOURCE),
    FILE_NOT_FOUND("FS403", "File not found", ErrorCategory.RESOURCE),
    FILE_ACCESS_DENIED("FS404", "File access denied", ErrorCategory.RESOURCE),
    DISK_SPACE_INSUFFICIENT("FS405", "Insufficient disk space", ErrorCategory.RESOURCE),

    // 数据相关错误 (FS500-FS599)
    DATA_VALIDATION_FAILED("FS500", "Data validation failed", ErrorCategory.DATA),
    DATA_CONVERSION_ERROR("FS501", "Data conversion error", ErrorCategory.DATA),
    DATA_INTEGRITY_VIOLATION("FS502", "Data integrity violation", ErrorCategory.DATA),
    DATA_FORMAT_ERROR("FS503", "Data format error", ErrorCategory.DATA),
    DATA_SIZE_EXCEEDED("FS504", "Data size exceeded limit", ErrorCategory.DATA),

    // 安全相关错误 (FS600-FS699)
    SECURITY_VIOLATION("FS600", "Security violation", ErrorCategory.SECURITY),
    INVALID_CREDENTIALS("FS601", "Invalid credentials", ErrorCategory.SECURITY),
    ACCESS_DENIED("FS602", "Access denied", ErrorCategory.SECURITY),
    TOKEN_EXPIRED("FS603", "Token expired", ErrorCategory.SECURITY),
    ENCRYPTION_FAILED("FS604", "Encryption failed", ErrorCategory.SECURITY),
    DECRYPTION_FAILED("FS605", "Decryption failed", ErrorCategory.SECURITY),
    
    // Token管理相关错误 (FS610-FS619)
    TOKEN_MANAGEMENT_ERROR("FS610", "Token management error", ErrorCategory.SECURITY),
    TOKEN_FETCH_FAILED("FS611", "Failed to fetch token from API", ErrorCategory.SECURITY),
    TOKEN_PARSE_ERROR("FS612", "Failed to parse token response", ErrorCategory.SECURITY),
    TOKEN_CACHE_ERROR("FS613", "Token cache operation failed", ErrorCategory.SECURITY),
    TOKEN_REFRESH_FAILED("FS614", "Token refresh operation failed", ErrorCategory.SECURITY),

    // 业务逻辑相关错误 (FS700-FS799)
    BUSINESS_LOGIC_ERROR("FS700", "Business logic error", ErrorCategory.BUSINESS),
    INVALID_OPERATION("FS701", "Invalid operation", ErrorCategory.BUSINESS),
    OPERATION_NOT_SUPPORTED("FS702", "Operation not supported", ErrorCategory.BUSINESS),
    PRECONDITION_FAILED("FS703", "Precondition failed", ErrorCategory.BUSINESS),
    WORKFLOW_ERROR("FS704", "Workflow error", ErrorCategory.BUSINESS),

    // 系统相关错误 (FS800-FS899)
    SYSTEM_ERROR("FS800", "System error", ErrorCategory.SYSTEM),
    SERVICE_UNAVAILABLE("FS801", "Service unavailable", ErrorCategory.SYSTEM),
    MAINTENANCE_MODE("FS802", "System in maintenance mode", ErrorCategory.SYSTEM),
    VERSION_INCOMPATIBLE("FS803", "Version incompatible", ErrorCategory.SYSTEM),

    // 未知错误 (FS999)
    UNKNOWN_ERROR("FS999", "Unknown error", ErrorCategory.UNKNOWN);

    private final String code;
    private final String defaultMessage;
    private final ErrorCategory category;

    /**
     * 构造函数
     * 
     * @param code 错误代码
     * @param defaultMessage 默认错误消息
     * @param category 错误分类
     */
    ErrorCode(String code, String defaultMessage, ErrorCategory category) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.category = category;
    }

    /**
     * 获取错误代码
     * 
     * @return 错误代码
     */
    @Override
    public String getCode() {
        return code;
    }

    /**
     * 获取默认描述
     * 
     * @return 默认描述
     */
    @Override
    public String getDesc() {
        return defaultMessage;
    }

    /**
     * 获取错误分类
     * 
     * @return 错误分类
     */
    public ErrorCategory getCategory() {
        return category;
    }

    /**
     * 获取默认错误消息
     * 
     * @return 默认错误消息
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * 获取国际化错误消息
     * 
     * @param locale 语言环境
     * @return 国际化错误消息
     */
    public String getMessage(Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages.errors", locale);
            return bundle.getString(this.code);
        } catch (Exception e) {
            // 如果获取国际化消息失败，返回默认消息
            return defaultMessage;
        }
    }

    /**
     * 获取当前语言环境的错误消息
     * 
     * @return 当前语言环境的错误消息
     */
    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    /**
     * 获取格式化的错误消息
     * 
     * @param locale 语言环境
     * @param args 格式化参数
     * @return 格式化的错误消息
     */
    public String getFormattedMessage(Locale locale, Object... args) {
        String message = getMessage(locale);
        if (args != null && args.length > 0) {
            return String.format(message, args);
        }
        return message;
    }

    /**
     * 获取当前语言环境的格式化错误消息
     * 
     * @param args 格式化参数
     * @return 格式化的错误消息
     */
    public String getFormattedMessage(Object... args) {
        return getFormattedMessage(Locale.getDefault(), args);
    }

    /**
     * 根据错误代码获取枚举值
     * 
     * @param code 错误代码
     * @return 对应的枚举值，未找到返回UNKNOWN_ERROR
     */
    public static ErrorCode getByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN_ERROR;
        }
        
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 根据分类获取所有错误代码
     * 
     * @param category 错误分类
     * @return 该分类下的所有错误代码
     */
    public static ErrorCode[] getByCategory(ErrorCategory category) {
        return java.util.Arrays.stream(values())
                .filter(errorCode -> errorCode.getCategory() == category)
                .toArray(ErrorCode[]::new);
    }

    /**
     * 检查是否为客户端错误
     * 
     * @return 如果是客户端错误返回true
     */
    public boolean isClientError() {
        return category == ErrorCategory.CLIENT;
    }

    /**
     * 检查是否为服务器错误
     * 
     * @return 如果是服务器错误返回true
     */
    public boolean isServerError() {
        return category == ErrorCategory.API || category == ErrorCategory.SYSTEM;
    }

    /**
     * 检查是否为可重试的错误
     * 
     * @return 如果是可重试的错误返回true
     */
    public boolean isRetryable() {
        switch (this) {
            case API_RATE_LIMIT_EXCEEDED:
            case CLIENT_TIMEOUT:
            case API_SERVER_ERROR:
            case SERVICE_UNAVAILABLE:
            case CONNECTION_POOL_EXHAUSTED:
            case TOKEN_FETCH_FAILED:
            case TOKEN_REFRESH_FAILED:
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查是否为致命错误
     * 
     * @return 如果是致命错误返回true
     */
    public boolean isFatal() {
        switch (this) {
            case CLIENT_NOT_INITIALIZED:
            case CLIENT_INITIALIZATION_FAILED:
            case CONFIGURATION_ERROR:
            case SECURITY_VIOLATION:
            case SYSTEM_ERROR:
                return true;
            default:
                return false;
        }
    }

    /**
     * 错误分类枚举
     */
    public enum ErrorCategory {
        /** 客户端错误 */
        CLIENT("Client"),
        /** API错误 */
        API("API"),
        /** 并发错误 */
        CONCURRENCY("Concurrency"),
        /** 配置错误 */
        CONFIGURATION("Configuration"),
        /** 资源错误 */
        RESOURCE("Resource"),
        /** 数据错误 */
        DATA("Data"),
        /** 安全错误 */
        SECURITY("Security"),
        /** 业务逻辑错误 */
        BUSINESS("Business"),
        /** 系统错误 */
        SYSTEM("System"),
        /** 未知错误 */
        UNKNOWN("Unknown");

        private final String name;

        ErrorCategory(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}