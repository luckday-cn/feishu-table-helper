package cn.isliu.core.service;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.RootFolderMetaResponse;
import okhttp3.Request;

import java.io.IOException;

/**
 * 飞书文件服务
 * 
 * 处理飞书云盘相关的API调用，包括获取根目录元数据等功能
 * 
 * @author FsHelper
 * @since 1.0
 */
public class CustomFileService extends AbstractFeishuApiService {

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public CustomFileService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 获取根目录元数据
     * 
     * 调用飞书开放平台API获取当前租户的根目录token和相关信息
     * API接口: GET https://open.feishu.cn/open-apis/drive/explorer/v2/root_folder/meta
     * 
     * @return 根目录元数据响应
     * @throws IOException 网络请求异常
     */
    public RootFolderMetaResponse getRootFolderMeta() throws IOException {
        String url = BASE_URL + "/drive/explorer/v2/root_folder/meta";
        
        Request request = createAuthenticatedRequest(url, "GET", null).build();
        
        return executeRequest(request, RootFolderMetaResponse.class);
    }
}
