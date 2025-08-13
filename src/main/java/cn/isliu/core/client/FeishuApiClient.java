package cn.isliu.core.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lark.oapi.core.utils.Jsons;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 飞书API客户端抽象类 提供基础的HTTP请求处理和认证逻辑
 */
public abstract class FeishuApiClient {
    protected final FeishuClient feishuClient;
    protected final OkHttpClient httpClient;
    protected final Gson gson;

    protected static final String BASE_URL = "https://open.feishu.cn/open-apis";
    protected static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public FeishuApiClient(FeishuClient feishuClient) {
        this.feishuClient = feishuClient;
        this.httpClient = feishuClient.getHttpClient();
        this.gson = Jsons.DEFAULT;
    }

    /**
     * 获取租户访问令牌
     * 
     * @return 访问令牌
     * @throws IOException 请求异常
     */
    protected String getTenantAccessToken() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", feishuClient.getAppId());
        params.put("app_secret", feishuClient.getAppSecret());

        RequestBody body = RequestBody.create(gson.toJson(params), JSON_MEDIA_TYPE);

        Request request =
            new Request.Builder().url(BASE_URL + "/auth/v3/tenant_access_token/internal").post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to get tenant access token: " + response);
            }

            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            if (jsonResponse.has("tenant_access_token")) {
                return jsonResponse.get("tenant_access_token").getAsString();
            } else {
                throw new IOException("Invalid token response: " + jsonResponse);
            }
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
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Request failed: " + response);
            }

            String responseBody = response.body().string();
            return gson.fromJson(responseBody, responseClass);
        }
    }
}