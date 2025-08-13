package cn.isliu.core.client;

import cn.isliu.core.service.*;
import com.lark.oapi.Client;
import com.lark.oapi.core.enums.AppType;
import com.lark.oapi.service.drive.DriveService;
import com.lark.oapi.service.sheets.SheetsService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * 飞书扩展客户端 封装官方SDK客户端并提供额外API支持
 */
public class FeishuClient {
    private final Client officialClient;
    private final OkHttpClient httpClient;
    private final String appId;
    private final String appSecret;

    // 自定义服务，处理官方SDK未覆盖的API
    private volatile CustomSheetService customSheetService;
    private volatile CustomDimensionService customDimensionService;
    private volatile CustomCellService customCellService;
    private volatile CustomValueService customValueService;
    private volatile CustomDataValidationService customDataValidationService;
    private volatile CustomProtectedDimensionService customProtectedDimensionService;

    private FeishuClient(String appId, String appSecret, Client officialClient, OkHttpClient httpClient) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.officialClient = officialClient;
        this.httpClient = httpClient;
    }

    /**
     * 创建客户端构建器
     * 
     * @param appId 应用ID
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
        if (customSheetService == null) {
            synchronized (this) {
                if (customSheetService == null) {
                    customSheetService = new CustomSheetService(this);
                }
            }
        }
        return customSheetService;
    }

    /**
     * 获取扩展行列服务
     * 
     * @return 扩展行列服务
     */
    public CustomDimensionService customDimensions() {
        if (customDimensionService == null) {
            synchronized (this) {
                if (customDimensionService == null) {
                    customDimensionService = new CustomDimensionService(this);
                }
            }
        }
        return customDimensionService;
    }

    /**
     * 获取扩展单元格服务
     * 
     * @return 扩展单元格服务
     */
    public CustomCellService customCells() {
        if (customCellService == null) {
            synchronized (this) {
                if (customCellService == null) {
                    customCellService = new CustomCellService(this);
                }
            }
        }
        return customCellService;
    }

    /**
     * 获取扩展数据值服务
     * 
     * @return 扩展数据值服务
     */
    public CustomValueService customValues() {
        if (customValueService == null) {
            synchronized (this) {
                if (customValueService == null) {
                    customValueService = new CustomValueService(this);
                }
            }
        }
        return customValueService;
    }

    /**
     * 获取自定义数据验证服务
     * 
     * @return 自定义数据验证服务
     */
    public CustomDataValidationService customDataValidations() {
        if (customDataValidationService == null) {
            synchronized (this) {
                if (customDataValidationService == null) {
                    customDataValidationService = new CustomDataValidationService(this);
                }
            }
        }
        return customDataValidationService;
    }

    /**
     * 获取扩展保护范围服务
     * 
     * @return 扩展保护范围服务
     */
    public CustomProtectedDimensionService customProtectedDimensions() {
        if (customProtectedDimensionService == null) {
            synchronized (this) {
                if (customProtectedDimensionService == null) {
                    customProtectedDimensionService = new CustomProtectedDimensionService(this);
                }
            }
        }
        return customProtectedDimensionService;
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
    OkHttpClient getHttpClient() {
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