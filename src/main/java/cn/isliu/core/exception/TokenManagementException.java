package cn.isliu.core.exception;

import cn.isliu.core.enums.ErrorCode;
import java.util.Map;

/**
 * Token management exception class
 * 
 * Specialized exception for handling various error scenarios during tenant_access_token management,
 * including token fetch failures, parsing errors, cache operation failures, etc.
 * 
 * @author isliu
 */
public class TokenManagementException extends FsHelperException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor - uses default TOKEN_MANAGEMENT_ERROR error code
     * 
     * @param message error message
     */
    public TokenManagementException(String message) {
        super(ErrorCode.TOKEN_MANAGEMENT_ERROR, message);
    }

    /**
     * Constructor - uses default TOKEN_MANAGEMENT_ERROR error code with cause
     * 
     * @param message error message
     * @param cause root cause exception
     */
    public TokenManagementException(String message, Throwable cause) {
        super(ErrorCode.TOKEN_MANAGEMENT_ERROR, message, cause);
    }

    /**
     * Constructor - specifies specific error code
     * 
     * @param errorCode specific token management error code
     * @param message error message
     */
    public TokenManagementException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructor - specifies specific error code with cause
     * 
     * @param errorCode specific token management error code
     * @param message error message
     * @param cause root cause exception
     */
    public TokenManagementException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * Constructor - includes context information
     * 
     * @param errorCode specific token management error code
     * @param message error message
     * @param context context information
     */
    public TokenManagementException(ErrorCode errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }

    /**
     * Constructor - full parameters
     * 
     * @param errorCode specific token management error code
     * @param message error message
     * @param context context information
     * @param cause root cause exception
     */
    public TokenManagementException(ErrorCode errorCode, String message, Map<String, Object> context, Throwable cause) {
        super(errorCode, message, context, cause);
    }

    // Static factory methods for creating specific types of token management exceptions

    /**
     * Create token fetch failed exception
     * 
     * @param message error message
     * @param cause root cause exception
     * @return TokenManagementException instance
     */
    public static TokenManagementException tokenFetchFailed(String message, Throwable cause) {
        return new TokenManagementException(ErrorCode.TOKEN_FETCH_FAILED, message, cause);
    }

    /**
     * Create token parse error exception
     * 
     * @param message error message
     * @param responseBody response body content for debugging
     * @return TokenManagementException instance
     */
    public static TokenManagementException tokenParseError(String message, String responseBody) {
        TokenManagementException exception = new TokenManagementException(ErrorCode.TOKEN_PARSE_ERROR, message);
        if (responseBody != null) {
            exception.addContext("responseBody", responseBody);
        }
        return exception;
    }

    /**
     * Create token cache operation failed exception
     * 
     * @param message error message
     * @param cause root cause exception
     * @return TokenManagementException instance
     */
    public static TokenManagementException tokenCacheError(String message, Throwable cause) {
        return new TokenManagementException(ErrorCode.TOKEN_CACHE_ERROR, message, cause);
    }

    /**
     * Create token refresh failed exception
     * 
     * @param message error message
     * @param cause root cause exception
     * @return TokenManagementException instance
     */
    public static TokenManagementException tokenRefreshFailed(String message, Throwable cause) {
        return new TokenManagementException(ErrorCode.TOKEN_REFRESH_FAILED, message, cause);
    }

    /**
     * Create token fetch failed exception with API response info
     * 
     * @param message error message
     * @param apiCode API returned error code
     * @param apiMessage API returned error message
     * @return TokenManagementException instance
     */
    public static TokenManagementException tokenFetchFailedWithApiInfo(String message, int apiCode, String apiMessage) {
        TokenManagementException exception = new TokenManagementException(ErrorCode.TOKEN_FETCH_FAILED, message);
        exception.addContext("apiCode", apiCode);
        exception.addContext("apiMessage", apiMessage);
        return exception;
    }

    /**
     * Create token management exception with retry information
     * 
     * @param errorCode error code
     * @param message error message
     * @param retryCount retry count
     * @param maxRetries maximum retry count
     * @param cause root cause exception
     * @return TokenManagementException instance
     */
    public static TokenManagementException withRetryInfo(ErrorCode errorCode, String message, 
                                                        int retryCount, int maxRetries, Throwable cause) {
        TokenManagementException exception = new TokenManagementException(errorCode, message, cause);
        exception.addContext("retryCount", retryCount);
        exception.addContext("maxRetries", maxRetries);
        exception.addContext("retriesExhausted", retryCount >= maxRetries);
        return exception;
    }

    /**
     * Check if this is a network-related token management exception
     * 
     * @return true if network-related exception
     */
    public boolean isNetworkRelated() {
        Throwable rootCause = getRootCause();
        return rootCause instanceof java.net.SocketTimeoutException ||
               rootCause instanceof java.net.ConnectException ||
               rootCause instanceof java.net.UnknownHostException ||
               rootCause instanceof java.io.IOException;
    }

    /**
     * Check if this is an API response-related exception
     * 
     * @return true if API response-related exception
     */
    public boolean isApiResponseRelated() {
        ErrorCode code = getErrorCode();
        return code == ErrorCode.TOKEN_PARSE_ERROR || 
               code == ErrorCode.TOKEN_FETCH_FAILED;
    }

    /**
     * Check if this is a cache-related exception
     * 
     * @return true if cache-related exception
     */
    public boolean isCacheRelated() {
        return getErrorCode() == ErrorCode.TOKEN_CACHE_ERROR;
    }

    /**
     * Get suggested retry delay time in milliseconds
     * 
     * @param retryCount current retry count
     * @return suggested delay time
     */
    public long getSuggestedRetryDelay(int retryCount) {
        if (!isRetryable()) {
            return 0;
        }

        // Exponential backoff strategy, base delay 1 second, max delay 30 seconds
        long baseDelay = 1000; // 1 second
        long maxDelay = 30000; // 30 seconds
        long delay = Math.min(baseDelay * (1L << retryCount), maxDelay);
        
        // Add random jitter to avoid thundering herd effect
        double jitter = 0.1; // 10% jitter
        long jitterAmount = (long) (delay * jitter * Math.random());
        
        return delay + jitterAmount;
    }

    /**
     * Get user-friendly error message
     * 
     * @return user-friendly error message
     */
    @Override
    public String getUserFriendlyMessage() {
        ErrorCode code = getErrorCode();
        switch (code) {
            case TOKEN_FETCH_FAILED:
                return "Failed to fetch access token, please check network connection and application configuration.";
            case TOKEN_PARSE_ERROR:
                return "Failed to parse access token response, please contact technical support.";
            case TOKEN_CACHE_ERROR:
                return "Token cache operation failed, system will re-fetch token.";
            case TOKEN_REFRESH_FAILED:
                return "Failed to refresh access token, please try again later.";
            default:
                return "Error occurred during token management, please try again later or contact technical support.";
        }
    }
}