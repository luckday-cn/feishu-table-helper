package cn.isliu.core.pojo;

/**
 * Token信息数据模型类
 * 
 * 封装飞书API的tenant_access_token相关信息，包括token值、过期时间和获取时间。
 * 提供便利方法用于检查token有效性、计算剩余时间和判断是否即将过期。
 * 
 * @author FsHelper
 * @since 1.0
 */
public class TokenInfo {

    /**
     * 实际的access token字符串
     */
    private final String token;

    /**
     * token过期的绝对时间戳（毫秒）
     */
    private final long expiresAt;

    /**
     * token获取的时间戳（毫秒），用于调试和日志
     */
    private final long fetchedAt;

    /**
     * 构造函数
     *
     * @param token 实际的access token字符串
     * @param expiresAt token过期的绝对时间戳（毫秒）
     * @param fetchedAt token获取的时间戳（毫秒）
     */
    public TokenInfo(String token, long expiresAt, long fetchedAt) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.fetchedAt = fetchedAt;
    }

    /**
     * 根据token值和有效期秒数创建TokenInfo实例
     * 
     * @param token 实际的access token字符串
     * @param expireSeconds token有效期（秒）
     * @return TokenInfo实例
     */
    public static TokenInfo create(String token, int expireSeconds) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (expireSeconds * 1000L);
        return new TokenInfo(token, expiresAt, now);
    }

    /**
     * 获取token字符串
     *
     * @return token字符串
     */
    public String getToken() {
        return token;
    }

    /**
     * 获取token过期时间戳
     *
     * @return 过期时间戳（毫秒）
     */
    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * 获取token获取时间戳
     *
     * @return 获取时间戳（毫秒）
     */
    public long getFetchedAt() {
        return fetchedAt;
    }

    /**
     * 检查token是否仍然有效
     * 
     * @return true表示token仍在有效期内，false表示已过期
     */
    public boolean isValid() {
        return System.currentTimeMillis() < expiresAt;
    }

    /**
     * 计算token剩余有效时间
     * 
     * @return 剩余有效时间（秒），如果已过期则返回0
     */
    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * 判断token是否即将过期
     * 
     * 根据飞书API文档，当剩余有效期小于30分钟时，调用接口会返回新的token。
     * 
     * @return true表示剩余有效期小于30分钟，false表示剩余有效期大于等于30分钟
     */
    public boolean isExpiringSoon() {
        return getRemainingSeconds() < 1800; // 30分钟 = 1800秒
    }

    @Override
    public String toString() {
        return "TokenInfo{" +
                "token='" + (token != null ? token.substring(0, Math.min(token.length(), 10)) + "..." : "null") + '\'' +
                ", expiresAt=" + expiresAt +
                ", fetchedAt=" + fetchedAt +
                ", remainingSeconds=" + getRemainingSeconds() +
                ", isValid=" + isValid() +
                ", isExpiringSoon=" + isExpiringSoon() +
                '}';
    }
}