package cn.isliu.core.pojo;

import com.google.gson.annotations.SerializedName;

/**
 * 飞书API获取租户访问令牌的响应模型类
 * 
 * 对应飞书API返回的JSON格式：
 * {
 *   "code": 0,
 *   "msg": "ok",
 *   "tenant_access_token": "t-caecc734c2e3328a62489fe0648c4b98779515d3",
 *   "expire": 7200
 * }
 * 
 * @author FsHelper
 * @since 1.0
 */
public class TokenResponse {

    /**
     * 响应状态码
     * 0表示成功，非0表示失败
     */
    @SerializedName("code")
    private int code;

    /**
     * 响应消息
     * 通常成功时为"ok"，失败时包含错误描述
     */
    @SerializedName("msg")
    private String msg;

    /**
     * 租户访问令牌
     * 用于后续API调用的认证
     */
    @SerializedName("tenant_access_token")
    private String tenant_access_token;

    /**
     * 令牌有效期（秒）
     * 通常为7200秒（2小时）
     */
    @SerializedName("expire")
    private int expire;

    /**
     * 默认构造函数
     */
    public TokenResponse() {
    }

    /**
     * 完整构造函数
     * 
     * @param code 响应状态码
     * @param msg 响应消息
     * @param tenant_access_token 租户访问令牌
     * @param expire 令牌有效期（秒）
     */
    public TokenResponse(int code, String msg, String tenant_access_token, int expire) {
        this.code = code;
        this.msg = msg;
        this.tenant_access_token = tenant_access_token;
        this.expire = expire;
    }

    /**
     * 获取响应状态码
     * 
     * @return 响应状态码，0表示成功
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应状态码
     * 
     * @param code 响应状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应消息
     * 
     * @return 响应消息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置响应消息
     * 
     * @param msg 响应消息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取租户访问令牌
     * 
     * @return 租户访问令牌字符串
     */
    public String getTenant_access_token() {
        return tenant_access_token;
    }

    /**
     * 设置租户访问令牌
     * 
     * @param tenant_access_token 租户访问令牌
     */
    public void setTenant_access_token(String tenant_access_token) {
        this.tenant_access_token = tenant_access_token;
    }

    /**
     * 获取令牌有效期
     * 
     * @return 令牌有效期（秒）
     */
    public int getExpire() {
        return expire;
    }

    /**
     * 设置令牌有效期
     * 
     * @param expire 令牌有效期（秒）
     */
    public void setExpire(int expire) {
        this.expire = expire;
    }

    /**
     * 检查响应是否成功
     * 
     * @return true表示API调用成功，false表示失败
     */
    public boolean isSuccess() {
        return code == 0;
    }

    /**
     * 检查是否包含有效的令牌数据
     * 
     * @return true表示包含有效的令牌数据
     */
    public boolean hasValidToken() {
        return isSuccess() && 
               tenant_access_token != null && 
               !tenant_access_token.trim().isEmpty() && 
               expire > 0;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", tenant_access_token='" + 
                (tenant_access_token != null ? 
                 tenant_access_token.substring(0, Math.min(tenant_access_token.length(), 10)) + "..." : 
                 "null") + '\'' +
                ", expire=" + expire +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenResponse that = (TokenResponse) o;

        if (code != that.code) return false;
        if (expire != that.expire) return false;
        if (!java.util.Objects.equals(msg, that.msg)) return false;
        return java.util.Objects.equals(tenant_access_token, that.tenant_access_token);
    }

    @Override
    public int hashCode() {
        int result = code;
        result = 31 * result + (msg != null ? msg.hashCode() : 0);
        result = 31 * result + (tenant_access_token != null ? tenant_access_token.hashCode() : 0);
        result = 31 * result + expire;
        return result;
    }
}