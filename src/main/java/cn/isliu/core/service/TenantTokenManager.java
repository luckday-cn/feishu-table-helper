package cn.isliu.core.service;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.exception.FsHelperException;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.pojo.TokenInfo;
import cn.isliu.core.pojo.TokenResponse;
import com.google.gson.Gson;
import com.lark.oapi.core.utils.Jsons;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 飞书租户访问令牌管理器
 * 
 * 负责管理tenant_access_token的获取、缓存、过期检测和自动刷新。
 * 实现线程安全的token管理，避免频繁的API调用，提高系统性能。
 * 
 * 核心功能：
 * - 智能缓存：缓存有效的token，避免重复获取
 * - 过期检测：自动检测token是否即将过期（剩余时间<30分钟）
 * - 自动刷新：在token即将过期时自动获取新token
 * - 线程安全：使用读写锁确保并发访问的安全性
 * - 异常处理：完善的错误处理和重试机制
 * 
 * @author FsHelper
 * @since 1.0
 */
public class TenantTokenManager {

    /** 飞书API基础URL */
    private static final String BASE_URL = "https://open.feishu.cn/open-apis";
    
    /** 获取租户访问令牌的API端点 */
    private static final String TOKEN_ENDPOINT = "/auth/v3/tenant_access_token/internal";
    
    /** JSON媒体类型 */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /** Token缓存组件 */
    private final TokenCache tokenCache;
    
    /** 读写锁，确保线程安全 */
    private final ReentrantReadWriteLock lock;
    
    /** 飞书客户端 */
    private final FeishuClient feishuClient;
    
    /** HTTP客户端 */
    private final OkHttpClient httpClient;
    
    /** JSON序列化工具 */
    private final Gson gson;

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端实例，不能为null
     * @throws IllegalArgumentException 如果feishuClient为null
     */
    public TenantTokenManager(FeishuClient feishuClient) {
        if (feishuClient == null) {
            throw new IllegalArgumentException("FeishuClient cannot be null");
        }
        
        this.feishuClient = feishuClient;
        this.httpClient = feishuClient.getHttpClient();
        this.gson = Jsons.DEFAULT;
        this.tokenCache = new TokenCache();
        this.lock = new ReentrantReadWriteLock();
        
        FsLogger.debug("TenantTokenManager initialized for app_id: {}", feishuClient.getAppId());
    }

    /**
     * 获取租户访问令牌 直接调用飞书API获取
     *
     * tenant_access_token 的最大有效期是 2 小时。
     *
     * 剩余有效期小于 30 分钟时，调用本接口会返回一个新的 tenant_access_token，这会同时存在两个有效的 tenant_access_token。
     * 剩余有效期大于等于 30 分钟时，调用本接口会返回原有的 tenant_access_token。
     *
     * @return 获取到的租户访问令牌字符串
     * @throws IOException 如果获取令牌时发生错误
     */
    public String getTenantAccessToken() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", feishuClient.getAppId());
        params.put("app_secret", feishuClient.getAppSecret());

        RequestBody body = RequestBody.create(gson.toJson(params), JSON_MEDIA_TYPE);

        Request request =
                new Request.Builder().url(BASE_URL + "/auth/v3/tenant_access_token/internal").post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status: " + response.code() +
                        ", message: " + response.message());
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            String responseBody = response.body().string();
            FsLogger.debug("Token API response received - status: {}, body_length: {} chars",
                    response.code(), responseBody.length());

            // 解析响应
            TokenResponse tokenResponse;
            try {
                tokenResponse = gson.fromJson(responseBody, TokenResponse.class);
            } catch (Exception e) {
                throw new IOException("Failed to parse token response JSON: " + responseBody, e);
            }

            // 检查API响应状态
            if (tokenResponse.getCode() != 0) {
                throw new IOException("API returned error code: " + tokenResponse.getCode() +
                        ", message: " + tokenResponse.getMsg());
            }

            // 验证响应数据
            if (tokenResponse.getTenant_access_token() == null ||
                    tokenResponse.getTenant_access_token().trim().isEmpty()) {
                throw new IOException("Invalid token response: tenant_access_token is null or empty");
            }

            if (tokenResponse.getExpire() <= 0) {
                throw new IOException("Invalid token response: expire value is invalid: " +
                        tokenResponse.getExpire());
            }

            FsLogger.debug("New token created successfully - expires in: {} seconds, token: {}",
                    tokenResponse.getExpire(),
                    tokenResponse.getTenant_access_token());


            return tokenResponse.getTenant_access_token();

        }
    }

    /**
     * 获取有效的租户访问令牌
     * 
     * 该方法实现智能的token管理逻辑和完整的线程安全机制：
     * 1. 首先使用读锁检查缓存中的token是否有效且未即将过期
     * 2. 如果缓存的token有效且不会很快过期，直接返回
     * 3. 如果token无效或即将过期，使用写锁获取新token
     * 4. 在写锁中再次检查缓存（双重检查锁定模式），避免重复获取
     * 5. 调用飞书API获取新token并更新缓存
     * 
     * 线程安全保证：
     * - 使用ReentrantReadWriteLock确保并发访问安全
     * - 实现双重检查锁定模式避免重复token获取
     * - 多个线程同时请求时，只有一个线程执行token获取操作
     * - 其他线程等待获取完成后使用新token
     * 
     * @return 有效的租户访问令牌字符串
     * @throws FsHelperException 当token获取失败时抛出
     */
    public String getCachedTenantAccessToken() throws FsHelperException {
        // 第一次检查：使用读锁检查缓存
        // 这允许多个线程同时读取缓存，提高并发性能
        lock.readLock().lock();
        try {
            TokenInfo cachedToken = tokenCache.get();
            if (cachedToken != null && cachedToken.isValid() && !cachedToken.isExpiringSoon()) {
                FsLogger.trace("Token cache hit - remaining: {} seconds", cachedToken.getRemainingSeconds());
                logTokenDetails(cachedToken, "cache_hit");
                return cachedToken.getToken();
            }
        } finally {
            lock.readLock().unlock();
        }

        // 需要获取新token，使用写锁确保只有一个线程执行获取操作
        lock.writeLock().lock();
        try {
            // 双重检查锁定模式：再次检查缓存
            // 这是关键的并发控制机制，防止多个线程重复获取token
            TokenInfo cachedToken = tokenCache.get();
            if (cachedToken != null && cachedToken.isValid() && !cachedToken.isExpiringSoon()) {
                FsLogger.debug("Token was refreshed by another thread, using updated token - remaining: {} seconds", 
                              cachedToken.getRemainingSeconds());
                return cachedToken.getToken();
            }

            // 记录token获取原因，便于调试并发问题
            String reason = cachedToken == null ? "no cached token" : 
                           !cachedToken.isValid() ? "token expired" : "token expiring soon";
            
            long startTime = System.currentTimeMillis();
            FsLogger.info("Token refresh triggered - reason: {}, thread: {}", 
                         reason, Thread.currentThread().getName());
            
            // 获取新token并原子性更新缓存
            TokenInfo newToken = fetchNewToken();
            tokenCache.set(newToken);
            
            long duration = System.currentTimeMillis() - startTime;
            FsLogger.info("Token refresh completed successfully - expires in: {} seconds, duration: {}ms, thread: {}", 
                         newToken.getRemainingSeconds(), duration, Thread.currentThread().getName());
            
            // 记录详细token信息
            logTokenDetails(newToken, "token_refreshed");
            
            // 记录性能指标
            FsLogger.logPerformance("tenant_token_fetch", duration, true);
            
            return newToken.getToken();
            
        } catch (IOException e) {
            // 记录错误日志
            String context = String.format("app_id=%s, endpoint=%s, thread=%s", 
                                         feishuClient.getAppId(), BASE_URL + TOKEN_ENDPOINT, 
                                         Thread.currentThread().getName());
            FsLogger.error(ErrorCode.TOKEN_FETCH_FAILED, 
                          "Failed to fetch tenant access token from Feishu API", context, e);
            
            // 清空缓存，强制下次重新获取
            // 这个操作在写锁保护下是原子的
            tokenCache.clear();
            throw FsHelperException.builder(ErrorCode.TOKEN_FETCH_FAILED)
                    .message("Failed to fetch tenant access token from Feishu API")
                    .context("app_id", feishuClient.getAppId())
                    .context("endpoint", BASE_URL + TOKEN_ENDPOINT)
                    .context("thread", Thread.currentThread().getName())
                    .cause(e)
                    .build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从飞书API获取新的访问令牌
     * 
     * 该方法负责：
     * 1. 构建API请求参数（app_id和app_secret）
     * 2. 发送HTTP POST请求到飞书API
     * 3. 解析API响应并提取token信息
     * 4. 创建TokenInfo实例并返回
     * 
     * @return 新的TokenInfo实例
     * @throws IOException 当网络请求失败或响应解析失败时抛出
     */
    private TokenInfo fetchNewToken() throws IOException {
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("app_id", feishuClient.getAppId());
        params.put("app_secret", feishuClient.getAppSecret());

        RequestBody body = RequestBody.create(gson.toJson(params), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(BASE_URL + TOKEN_ENDPOINT)
                .post(body)
                .build();

        FsLogger.debug("Sending token request to: {}", BASE_URL + TOKEN_ENDPOINT);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status: " + response.code() + 
                                    ", message: " + response.message());
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            String responseBody = response.body().string();
            FsLogger.debug("Token API response received - status: {}, body_length: {} chars", 
                          response.code(), responseBody.length());

            // 解析响应
            TokenResponse tokenResponse;
            try {
                tokenResponse = gson.fromJson(responseBody, TokenResponse.class);
            } catch (Exception e) {
                throw new IOException("Failed to parse token response JSON: " + responseBody, e);
            }

            // 检查API响应状态
            if (tokenResponse.getCode() != 0) {
                throw new IOException("API returned error code: " + tokenResponse.getCode() + 
                                    ", message: " + tokenResponse.getMsg());
            }

            // 验证响应数据
            if (tokenResponse.getTenant_access_token() == null || 
                tokenResponse.getTenant_access_token().trim().isEmpty()) {
                throw new IOException("Invalid token response: tenant_access_token is null or empty");
            }

            if (tokenResponse.getExpire() <= 0) {
                throw new IOException("Invalid token response: expire value is invalid: " + 
                                    tokenResponse.getExpire());
            }

            // 创建TokenInfo实例
            TokenInfo tokenInfo = TokenInfo.create(tokenResponse.getTenant_access_token(), tokenResponse.getExpire());
            
            FsLogger.debug("New token created successfully - expires in: {} seconds, token_prefix: {}...", 
                          tokenInfo.getRemainingSeconds(), 
                          tokenInfo.getToken().substring(0, Math.min(10, tokenInfo.getToken().length())));
            
            return tokenInfo;
        }
    }

    /**
     * 获取当前缓存的token信息（用于调试和监控）
     * 
     * @return 当前缓存的TokenInfo，如果缓存为空返回null
     */
    public TokenInfo getCurrentTokenInfo() {
        lock.readLock().lock();
        try {
            return tokenCache.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 强制清空token缓存
     * 
     * 该方法会清空当前缓存的token，强制下次调用getTenantAccessToken()时重新获取。
     * 通常在以下情况下使用：
     * - 检测到token无效时
     * - 需要强制刷新token时
     * - 系统重置或清理时
     */
    public void clearTokenCache() {
        lock.writeLock().lock();
        try {
            tokenCache.clear();
            FsLogger.info("Token cache cleared manually");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查当前是否有有效的缓存token
     * 
     * @return true表示有有效的缓存token，false表示缓存为空或token已过期
     */
    public boolean hasValidCachedToken() {
        lock.readLock().lock();
        try {
            return tokenCache.hasValidToken();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 强制刷新token（用于主动刷新场景）
     * 
     * 该方法会忽略当前缓存状态，强制获取新的token。
     * 使用写锁确保与其他操作的线程安全。
     * 
     * @return 新获取的租户访问令牌字符串
     * @throws FsHelperException 当token获取失败时抛出
     */
    public String forceRefreshToken() throws FsHelperException {
        lock.writeLock().lock();
        try {
            long startTime = System.currentTimeMillis();
            FsLogger.info("Force refresh initiated - thread: {}", Thread.currentThread().getName());
            
            TokenInfo newToken = fetchNewToken();
            tokenCache.set(newToken);
            
            long duration = System.currentTimeMillis() - startTime;
            FsLogger.info("Force refresh completed - expires in: {} seconds, duration: {}ms, thread: {}", 
                         newToken.getRemainingSeconds(), duration, Thread.currentThread().getName());
            
            // 记录性能指标
            FsLogger.logPerformance("tenant_token_force_refresh", duration, true);
            
            return newToken.getToken();
            
        } catch (IOException e) {
            String context = String.format("app_id=%s, thread=%s", 
                                         feishuClient.getAppId(), Thread.currentThread().getName());
            FsLogger.error(ErrorCode.TOKEN_FETCH_FAILED, 
                          "Failed to force refresh tenant access token", context, e);
            
            tokenCache.clear();
            throw FsHelperException.builder(ErrorCode.TOKEN_FETCH_FAILED)
                    .message("Failed to force refresh tenant access token from Feishu API")
                    .context("app_id", feishuClient.getAppId())
                    .context("thread", Thread.currentThread().getName())
                    .cause(e)
                    .build();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查是否有其他线程正在获取token
     * 
     * @return true表示有线程持有写锁（正在获取token），false表示没有
     */
    public boolean isTokenRefreshInProgress() {
        return lock.isWriteLocked();
    }

    /**
     * 获取当前持有读锁的线程数量
     * 
     * @return 持有读锁的线程数量
     */
    public int getReadLockCount() {
        return lock.getReadLockCount();
    }

    /**
     * 获取缓存状态信息（用于监控和调试）
     * 
     * @return 包含缓存状态的字符串描述
     */
    public String getCacheStatus() {
        lock.readLock().lock();
        try {
            TokenInfo token = tokenCache.get();
            String lockStatus = String.format("readers=%d, write_locked=%s",
                    lock.getReadLockCount(),
                    lock.isWriteLocked());
            
            if (token == null) {
                return String.format("Cache: empty, Lock: [%s]", lockStatus);
            }
            return String.format("Cache: token=%s..., valid=%s, expiring_soon=%s, remaining=%ds, Lock: [%s]",
                    token.getToken().substring(0, Math.min(10, token.getToken().length())),
                    token.isValid(),
                    token.isExpiringSoon(),
                    token.getRemainingSeconds(),
                    lockStatus);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 记录缓存状态日志（用于监控和调试）
     */
    public void logCacheStatus() {
        String status = getCacheStatus();
        FsLogger.debug("Token cache status: {}", status);
    }

    /**
     * 记录详细的token信息（用于调试）
     */
    private void logTokenDetails(TokenInfo token, String operation) {
        if (token != null) {
            Map<String, Object> context = new HashMap<>();
            context.put("operation", operation);
            context.put("token_prefix", token.getToken().substring(0, Math.min(10, token.getToken().length())));
            context.put("valid", token.isValid());
            context.put("expiring_soon", token.isExpiringSoon());
            context.put("remaining_seconds", token.getRemainingSeconds());
            context.put("thread", Thread.currentThread().getName());
            
            FsLogger.debug("Token details - operation: {}, valid: {}, expiring_soon: {}, remaining: {}s", 
                          operation, token.isValid(), token.isExpiringSoon(), token.getRemainingSeconds());
        }
    }
}