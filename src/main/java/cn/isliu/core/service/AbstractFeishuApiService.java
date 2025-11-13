package cn.isliu.core.service;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.exception.FsHelperException;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.ratelimit.ApiOperation;
import cn.isliu.core.ratelimit.FeishuApiExecutor;
import cn.isliu.core.ratelimit.FeishuApiOperationResolver;
import com.google.gson.Gson;
import com.lark.oapi.core.utils.Jsons;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书API客户端抽象类 提供基础的HTTP请求处理和认证逻辑
 */
public abstract class AbstractFeishuApiService {
    protected final FeishuClient feishuClient;
    protected final OkHttpClient httpClient;
    protected final Gson gson;
    protected final TenantTokenManager tokenManager;
    protected final FeishuApiExecutor apiExecutor;
    protected final String tenantKey;

    protected static final String BASE_URL = "https://open.feishu.cn/open-apis";
    protected static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public AbstractFeishuApiService(FeishuClient feishuClient) {
        this.feishuClient = feishuClient;
        this.httpClient = feishuClient.getHttpClient();
        this.gson = Jsons.DEFAULT;
        this.tokenManager = new TenantTokenManager(feishuClient);
        this.apiExecutor = feishuClient.apiExecutor();
        this.tenantKey = feishuClient.getAppId();
    }

    /**
     * 获取租户访问令牌
     * 
     * 使用TenantTokenManager进行智能的token管理，包括缓存、过期检测和自动刷新。
     * 
     * @return 访问令牌
     * @throws IOException 请求异常
     */
    protected String getTenantAccessToken() throws IOException {
        try {
            return tokenManager.getTenantAccessToken();
        } catch (FsHelperException e) {
            throw new IOException("Failed to get tenant access token: " + e.getMessage(), e);
        }
    }

    /**
     * 构建带认证的请求
     * 
     * @param url 请求URL
     * @param method HTTP方法
     * @param body 请求体
     * @return 请求构建器
     * @throws IOException 认证异常
     */
    protected Request.Builder createAuthenticatedRequest(String url, String method, RequestBody body)
        throws IOException {
        String token = getTenantAccessToken();
        return new Request.Builder().url(url).header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json; charset=utf-8").method(method, body);
    }

    /**
     * 执行请求并处理响应
     * 
     * @param request 请求对象
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException 请求异常
     */
    protected <T> T executeRequest(Request request, Class<T> responseClass) throws IOException {
        return executeRequest(null, request, responseClass);
    }

    protected <T> T executeRequest(String spreadsheetToken, Request request, Class<T> responseClass) throws IOException {
        ApiOperation operation = FeishuApiOperationResolver.resolve(request);
        String docToken = spreadsheetToken != null ? spreadsheetToken
                : FeishuApiOperationResolver.extractSpreadsheetToken(request);
        try {
            return apiExecutor.execute(tenantKey, operation, docToken,
                    () -> doExecuteRequest(request, responseClass));
        } catch (FsHelperException | IOException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new IOException("飞书接口调用失败", ex);
        }
    }

    private <T> T doExecuteRequest(Request request, Class<T> responseClass) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                if (response.code() == 429) {
                    FsHelperException exception = FsHelperException.builder(ErrorCode.API_CALL_FAILED)
                            .message("飞书接口触发频控限制 (429)")
                            .context("httpStatus", response.code())
                            .context("x-ogw-ratelimit-limit", response.header("x-ogw-ratelimit-limit"))
                            .context("x-ogw-ratelimit-reset", response.header("x-ogw-ratelimit-reset"))
                            .context("requestId", response.header("X-Tt-Logid"))
                            .build();
                    FsLogger.warn("飞书接口频控：url={}, headers={}", request.url(),
                            response.headers().toMultimap());
                    throw exception;
                }
                throw new IOException("Request failed: " + response);
            }

            String responseBody = response.body().string();
            return gson.fromJson(responseBody, responseClass);
        }
    }

    protected <T> T executeWithOperation(ApiOperation operation, String spreadsheetToken,
                                         FeishuApiExecutor.CheckedCallable<T> action) throws Exception {
        return apiExecutor.execute(tenantKey, operation, spreadsheetToken, action);
    }
}