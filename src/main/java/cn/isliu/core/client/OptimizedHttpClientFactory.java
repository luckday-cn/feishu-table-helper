package cn.isliu.core.client;

import cn.isliu.core.logging.FsLogger;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 优化的HTTP客户端工厂
 * 提供连接池优化、超时配置、重试机制和监控功能
 */
public class OptimizedHttpClientFactory {
    
    // 连接池配置常量
    private static final int MAX_IDLE_CONNECTIONS = 10;
    private static final long KEEP_ALIVE_DURATION = 5; // minutes
    private static final int CONNECT_TIMEOUT = 30; // seconds
    private static final int READ_TIMEOUT = 60; // seconds
    private static final int WRITE_TIMEOUT = 60; // seconds
    private static final int CALL_TIMEOUT = 120; // seconds
    
    // 重试配置常量
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY = 1000; // milliseconds
    
    /**
     * 创建优化的HTTP客户端
     * 
     * @return 配置优化的OkHttpClient实例
     */
    public static OkHttpClient createOptimizedClient() {
        return createOptimizedClient(new ClientConfig());
    }
    
    /**
     * 使用自定义配置创建优化的HTTP客户端
     * 
     * @param config 客户端配置
     * @return 配置优化的OkHttpClient实例
     */
    public static OkHttpClient createOptimizedClient(ClientConfig config) {
        // 创建优化的连接池
        ConnectionPool connectionPool = new ConnectionPool(
            config.maxIdleConnections,
            config.keepAliveDuration,
            TimeUnit.MINUTES
        );
        
        // 创建HTTP客户端构建器
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(config.connectTimeout, TimeUnit.SECONDS)
            .readTimeout(config.readTimeout, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeout, TimeUnit.SECONDS)
            .callTimeout(config.callTimeout, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true);
        
        // 添加拦截器链
        addInterceptors(builder, config);
        
        return builder.build();
    }
    
    /**
     * 添加拦截器链
     * 
     * @param builder OkHttp客户端构建器
     * @param config 客户端配置
     */
    private static void addInterceptors(OkHttpClient.Builder builder, ClientConfig config) {
        // 添加重试拦截器
        if (config.enableRetry) {
            builder.addInterceptor(new RetryInterceptor(config.maxRetryAttempts, config.initialRetryDelay));
        }
        
        // 添加监控拦截器
        if (config.enableMonitoring) {
            builder.addInterceptor(new MonitoringInterceptor());
        }
        
        // 添加日志拦截器
        if (config.enableLogging) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                FsLogger.debug("HTTP: " + message));
            loggingInterceptor.setLevel(config.loggingLevel);
            builder.addInterceptor(loggingInterceptor);
        }
        
        // 添加用户代理拦截器
        builder.addInterceptor(new UserAgentInterceptor());
    }
    
    /**
     * 重试拦截器
     * 实现指数退避重试策略
     */
    public static class RetryInterceptor implements Interceptor {
        private final int maxRetryAttempts;
        private final long initialRetryDelay;
        
        public RetryInterceptor(int maxRetryAttempts, long initialRetryDelay) {
            this.maxRetryAttempts = maxRetryAttempts;
            this.initialRetryDelay = initialRetryDelay;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;
            
            for (int attempt = 0; attempt <= maxRetryAttempts; attempt++) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    
                    response = chain.proceed(request);
                    
                    // 如果响应成功或不可重试，直接返回
                    if (response.isSuccessful() || !isRetryableResponse(response)) {
                        return response;
                    }
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < maxRetryAttempts) {
                        long delay = calculateRetryDelay(attempt);
                        FsLogger.warn("HTTP request failed, retrying in {}ms. Attempt: {}/{}", 
                            delay, attempt + 1, maxRetryAttempts);
                        
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", e);
                        }
                    }
                    
                } catch (IOException e) {
                    lastException = e;
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < maxRetryAttempts && isRetryableException(e)) {
                        long delay = calculateRetryDelay(attempt);
                        FsLogger.warn("HTTP request failed with exception, retrying in {}ms. Attempt: {}/{} - {}", 
                            delay, attempt + 1, maxRetryAttempts, e.getMessage());
                        
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", ie);
                        }
                    } else {
                        throw e;
                    }
                }
            }
            
            // 如果所有重试都失败了
            if (response != null && !response.isSuccessful()) {
                return response;
            }
            
            if (lastException != null) {
                throw lastException;
            }
            
            throw new IOException("All retry attempts failed");
        }
        
        /**
         * 计算重试延迟时间（指数退避）
         * 
         * @param attempt 当前重试次数
         * @return 延迟时间（毫秒）
         */
        private long calculateRetryDelay(int attempt) {
            return initialRetryDelay * (1L << attempt); // 指数退避：1s, 2s, 4s, 8s...
        }
        
        /**
         * 判断响应是否可重试
         * 
         * @param response HTTP响应
         * @return 是否可重试
         */
        private boolean isRetryableResponse(Response response) {
            int code = response.code();
            // 5xx服务器错误和429限流错误可重试
            return code >= 500 || code == 429;
        }
        
        /**
         * 判断异常是否可重试
         * 
         * @param exception 异常
         * @return 是否可重试
         */
        private boolean isRetryableException(IOException exception) {
            // 连接超时、读取超时等网络异常可重试
            return exception instanceof java.net.SocketTimeoutException ||
                   exception instanceof java.net.ConnectException ||
                   exception instanceof java.net.UnknownHostException;
        }
    }
    
    /**
     * 监控拦截器
     * 收集请求性能指标和连接状态
     */
    public static class MonitoringInterceptor implements Interceptor {
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            
            try {
                Response response = chain.proceed(request);
                long duration = System.currentTimeMillis() - startTime;
                
                // 记录成功请求的性能指标
                FsLogger.apiCall(
                    request.method() + " " + request.url().encodedPath(),
                    request.url().query(),
                    duration
                );
                
                return response;
                
            } catch (IOException e) {
                long duration = System.currentTimeMillis() - startTime;
                
                // 记录失败请求
                FsLogger.warn("HTTP request failed: {} {} in {}ms - {}", 
                    request.method(), request.url().encodedPath(), duration, e.getMessage());
                
                throw e;
            }
        }
    }
    
    /**
     * 用户代理拦截器
     * 添加统一的User-Agent头
     */
    public static class UserAgentInterceptor implements Interceptor {
        private static final String USER_AGENT = "FeishuTableHelper/0.0.2 (Java)";
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", USER_AGENT)
                .build();
            
            return chain.proceed(requestWithUserAgent);
        }
    }
    
    /**
     * HTTP客户端配置类
     */
    public static class ClientConfig {
        // 连接池配置
        public int maxIdleConnections = MAX_IDLE_CONNECTIONS;
        public long keepAliveDuration = KEEP_ALIVE_DURATION;
        
        // 超时配置
        public int connectTimeout = CONNECT_TIMEOUT;
        public int readTimeout = READ_TIMEOUT;
        public int writeTimeout = WRITE_TIMEOUT;
        public int callTimeout = CALL_TIMEOUT;
        
        // 重试配置
        public boolean enableRetry = true;
        public int maxRetryAttempts = MAX_RETRY_ATTEMPTS;
        public long initialRetryDelay = INITIAL_RETRY_DELAY;
        
        // 监控配置
        public boolean enableMonitoring = true;
        
        // 日志配置
        public boolean enableLogging = false; // 默认关闭详细日志
        public HttpLoggingInterceptor.Level loggingLevel = HttpLoggingInterceptor.Level.BASIC;
        
        /**
         * 创建默认配置
         * 
         * @return 默认配置实例
         */
        public static ClientConfig defaultConfig() {
            return new ClientConfig();
        }
        
        /**
         * 创建生产环境配置
         * 
         * @return 生产环境配置实例
         */
        public static ClientConfig productionConfig() {
            ClientConfig config = new ClientConfig();
            config.enableLogging = false;
            config.maxRetryAttempts = 2; // 生产环境减少重试次数
            return config;
        }
        
        /**
         * 创建开发环境配置
         * 
         * @return 开发环境配置实例
         */
        public static ClientConfig developmentConfig() {
            ClientConfig config = new ClientConfig();
            config.enableLogging = true;
            config.loggingLevel = HttpLoggingInterceptor.Level.BODY;
            return config;
        }
        
        // 流式配置方法
        public ClientConfig maxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
            return this;
        }
        
        public ClientConfig keepAliveDuration(long keepAliveDuration) {
            this.keepAliveDuration = keepAliveDuration;
            return this;
        }
        
        public ClientConfig connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        
        public ClientConfig readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }
        
        public ClientConfig writeTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
            return this;
        }
        
        public ClientConfig callTimeout(int callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }
        
        public ClientConfig enableRetry(boolean enableRetry) {
            this.enableRetry = enableRetry;
            return this;
        }
        
        public ClientConfig maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }
        
        public ClientConfig initialRetryDelay(long initialRetryDelay) {
            this.initialRetryDelay = initialRetryDelay;
            return this;
        }
        
        public ClientConfig enableMonitoring(boolean enableMonitoring) {
            this.enableMonitoring = enableMonitoring;
            return this;
        }
        
        public ClientConfig enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }
        
        public ClientConfig loggingLevel(HttpLoggingInterceptor.Level loggingLevel) {
            this.loggingLevel = loggingLevel;
            return this;
        }
    }
}