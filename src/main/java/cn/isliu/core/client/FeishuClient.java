package cn.isliu.core.client;

import com.lark.oapi.Client;
import com.lark.oapi.core.enums.AppType;
import com.lark.oapi.service.drive.DriveService;
import com.lark.oapi.service.sheets.SheetsService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

import cn.isliu.core.service.*;

/**
 * 飞书客户端，用于与飞书API进行交互
 * <p>
 * 该客户端整合了官方SDK和自定义扩展功能，提供了对飞书表格的完整操作能力。
 * 包括官方提供的基础功能和项目自定义的扩展功能。
 */
public class FeishuClient {
    private final Client officialClient;
    private final OkHttpClient httpClient;
    private final String appId;
    private final String appSecret;

    // 服务管理器，用于统一管理自定义服务实例
    private final ServiceManager<FeishuClient> serviceManager = new ServiceManager<>(this);

    private FeishuClient(String appId, String appSecret, Client officialClient, OkHttpClient httpClient) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.officialClient = officialClient;
        this.httpClient = httpClient;
    }

    /**
     * 创建客户端构建器
     *
     * @param appId     应用ID
     * @param appSecret 应用密钥
     * @return 构建器
     */
    public static Builder newBuilder(String appId, String appSecret) {
        return new Builder(appId, appSecret);
    }

    /**
     * 获取官方表格服务
     *
     * @return 官方表格服务
     */
    public SheetsService sheets() {
        return officialClient.sheets();
    }

    /**
     * 获取官方驱动服务
     *
     * @return 官方驱动服务
     */
    public DriveService drive() {
        return officialClient.drive();
    }

    /**
     * 获取扩展表格服务
     *
     * @return 扩展表格服务
     */
    public CustomSheetService customSheets() {
        return serviceManager.getService(CustomSheetService.class, () -> new CustomSheetService(this));
    }

    /**
     * 获取扩展行列服务
     *
     * @return 扩展行列服务
     */
    public CustomDimensionService customDimensions() {
        return serviceManager.getService(CustomDimensionService.class, () -> new CustomDimensionService(this));
    }

    /**
     * 获取扩展单元格服务
     *
     * @return 扩展单元格服务
     */
    public CustomCellService customCells() {
        return serviceManager.getService(CustomCellService.class, () -> new CustomCellService(this));
    }

    /**
     * 获取扩展数据值服务
     *
     * @return 扩展数据值服务
     */
    public CustomValueService customValues() {
        return serviceManager.getService(CustomValueService.class, () -> new CustomValueService(this));
    }

    /**
     * 获取自定义数据验证服务
     *
     * @return 自定义数据验证服务
     */
    public CustomDataValidationService customDataValidations() {
        return serviceManager.getService(CustomDataValidationService.class, () -> new CustomDataValidationService(this));
    }

    /**
     * 获取扩展保护范围服务
     *
     * @return 扩展保护范围服务
     */
    public CustomProtectedDimensionService customProtectedDimensions() {
        return serviceManager.getService(CustomProtectedDimensionService.class, () -> new CustomProtectedDimensionService(this));
    }

    /**
     * 获取官方客户端
     *
     * @return 官方Client实例
     */
    public Client getOfficialClient() {
        return officialClient;
    }

    /**
     * 获取HTTP客户端
     *
     * @return OkHttp客户端实例
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * 获取应用ID
     *
     * @return 应用ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * 获取应用密钥
     *
     * @return 应用密钥
     */
    public String getAppSecret() {
        return appSecret;
    }

    /**
     * FeishuClient构建器
     */
    public static class Builder {
        private final String appId;
        private final String appSecret;
        private OkHttpClient.Builder httpClientBuilder;
        private AppType appType = AppType.SELF_BUILT;
        private boolean logReqAtDebug = false;

        private Builder(String appId, String appSecret) {
            this.appId = appId;
            this.appSecret = appSecret;
            // 默认OkHttp配置
            this.httpClientBuilder =
                    new OkHttpClient.Builder().connectTimeout(10, TimeUnit.MINUTES).readTimeout(10, TimeUnit.MINUTES)
                            .writeTimeout(10, TimeUnit.MINUTES).connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES));
        }

        /**
         * 配置HTTP客户端
         *
         * @param builder OkHttp客户端构建器
         * @return 当前构建器
         */
        public Builder httpClient(OkHttpClient.Builder builder) {
            this.httpClientBuilder = builder;
            return this;
        }

        /**
         * 设置应用类型
         *
         * @param appType 应用类型
         * @return 当前构建器
         */
        public Builder appType(AppType appType) {
            this.appType = appType;
            return this;
        }

        /**
         * 是否在debug级别打印请求
         *
         * @param logReqAtDebug 是否打印
         * @return 当前构建器
         */
        public Builder logReqAtDebug(boolean logReqAtDebug) {
            this.logReqAtDebug = logReqAtDebug;
            return this;
        }

        /**
         * 构建FeishuClient实例
         *
         * @return FeishuClient实例
         */
        public FeishuClient build() {
            // 构建官方Client
            Client officialClient =
                    Client.newBuilder(appId, appSecret).appType(appType).logReqAtDebug(logReqAtDebug).build();

            // 构建OkHttpClient
            OkHttpClient httpClient = httpClientBuilder.build();

            return new FeishuClient(appId, appSecret, officialClient, httpClient);
        }
    }
}