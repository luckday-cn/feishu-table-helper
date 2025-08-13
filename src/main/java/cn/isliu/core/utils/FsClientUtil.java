package cn.isliu.core.utils;

import cn.isliu.core.client.FeishuClient;


public class FsClientUtil {

    public static FeishuClient client;

    /**
     * 获取飞书客户端
     *
     * @param appId     飞书应用ID
     * @param appSecret 飞书应用密钥
     * @return 飞书客户端
     */
    public static FeishuClient initFeishuClient(String appId, String appSecret) {
        client = FeishuClient.newBuilder(appId, appSecret).build();
        return client;
    }

    /**
     * 设置飞书客户端
     *
     * @param appId     飞书应用ID
     * @param appSecret 飞书应用密钥
     */
    public static void setClient(String appId, String appSecret) {
        client = FeishuClient.newBuilder(appId, appSecret).build();
    }

    public static FeishuClient getFeishuClient() {
        return client;
    }
}
