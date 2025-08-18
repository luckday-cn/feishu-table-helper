package cn.isliu.core.service;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义保护范围服务 提供保护行列的API
 */
public class CustomProtectedDimensionService extends AbstractFeishuApiService {

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public CustomProtectedDimensionService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量操作保护范围 支持添加保护范围 支持处理多个请求，如果有请求失败则中断后续请求
     * 
     * @param spreadsheetToken 电子表格Token
     * @param request 批量操作请求
     * @return 批量操作响应
     * @throws IOException 请求异常
     */
    public ApiResponse protectedDimensionBatchUpdate(String spreadsheetToken,
        ProtectedDimensionBatchUpdateRequest request) throws IOException {
        List<ProtectedDimensionRequest> requests = request.getRequests();
        ApiResponse response = null;

        // 如果没有请求，返回空响应
        if (requests == null || requests.isEmpty()) {
            ApiResponse emptyResponse = new ApiResponse();
            emptyResponse.setCode(400);
            emptyResponse.setMsg("No protected dimension operations found");
            return emptyResponse;
        }

        // 依次处理每个请求
        for (ProtectedDimensionRequest protectedDimensionRequest : requests) {
            // 处理添加保护范围请求
            if (protectedDimensionRequest.getAddProtectedDimension() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/protected_dimension";

                // 构建添加保护范围请求体
                RequestBody body = RequestBody.create(
                    gson.toJson(
                        new AddProtectedDimensionRequestBody(protectedDimensionRequest.getAddProtectedDimension())),
                    JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 这里可以添加其他保护范围操作类型
        }

        // 如果所有请求都成功处理，返回最后一个成功的响应
        // 如果没有处理任何请求(没有有效的操作类型)，返回错误响应
        if (response == null) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setCode(400);
            errorResponse.setMsg("No valid protected dimension operation found");
            return errorResponse;
        }

        return response;
    }

    /**
     * 批量操作保护范围请求
     */
    public static class ProtectedDimensionBatchUpdateRequest {
        private List<ProtectedDimensionRequest> requests;

        public ProtectedDimensionBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<ProtectedDimensionRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<ProtectedDimensionRequest> requests) {
            this.requests = requests;
        }

        /**
         * 创建批量操作保护范围请求的构建器
         * 
         * @return 批量操作保护范围请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量操作保护范围请求的构建器
         */
        public static class Builder {
            private final ProtectedDimensionBatchUpdateRequest request;

            public Builder() {
                request = new ProtectedDimensionBatchUpdateRequest();
            }

            /**
             * 添加一个保护范围操作请求
             * 
             * @param protectedDimensionRequest 保护范围操作请求
             * @return 当前构建器
             */
            public Builder addRequest(ProtectedDimensionRequest protectedDimensionRequest) {
                request.requests.add(protectedDimensionRequest);
                return this;
            }

            /**
             * 构建批量操作保护范围请求
             * 
             * @return 批量操作保护范围请求
             */
            public ProtectedDimensionBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 保护范围操作请求
     */
    public static class ProtectedDimensionRequest {
        private List<AddProtectedDimensionRange> addProtectedDimension;

        /**
         * 获取添加保护范围的信息
         * 
         * @return 添加保护范围的信息
         */
        public List<AddProtectedDimensionRange> getAddProtectedDimension() {
            return addProtectedDimension;
        }

        /**
         * 设置添加保护范围的信息
         * 
         * @param addProtectedDimension 添加保护范围的信息
         */
        public void setAddProtectedDimension(List<AddProtectedDimensionRange> addProtectedDimension) {
            this.addProtectedDimension = addProtectedDimension;
        }

        /**
         * 创建添加保护范围构建器
         * 
         * @return 添加保护范围构建器
         */
        public static AddProtectedDimensionBuilder addProtectedDimension() {
            return new AddProtectedDimensionBuilder();
        }

        /**
         * 添加保护范围构建器
         */
        public static class AddProtectedDimensionBuilder {
            private final ProtectedDimensionRequest request;
            private final AddProtectedDimensionRange protectedDimension;

            public AddProtectedDimensionBuilder() {
                request = new ProtectedDimensionRequest();
                protectedDimension = new AddProtectedDimensionRange();

                List<AddProtectedDimensionRange> list = new ArrayList<>();
                list.add(protectedDimension);
                request.setAddProtectedDimension(list);
            }

            /**
             * 设置工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder sheetId(String sheetId) {
                protectedDimension.getDimension().setSheetId(sheetId);
                return this;
            }

            /**
             * 设置维度方向
             * 
             * @param majorDimension 维度方向，可选值：ROWS（行）、COLUMNS（列）
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder majorDimension(String majorDimension) {
                protectedDimension.getDimension().setMajorDimension(majorDimension);
                return this;
            }

            /**
             * 设置开始索引
             * 
             * @param startIndex 开始索引，从1开始计数
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder startIndex(Integer startIndex) {
                protectedDimension.getDimension().setStartIndex(startIndex);
                return this;
            }

            /**
             * 设置结束索引
             * 
             * @param endIndex 结束索引，从1开始计数
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder endIndex(Integer endIndex) {
                protectedDimension.getDimension().setEndIndex(endIndex);
                return this;
            }

            /**
             * 添加一个允许编辑的用户ID
             * 
             * @param userId 用户ID
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder addUser(String userId) {
                protectedDimension.getUsers().add(userId);
                return this;
            }

            /**
             * 设置多个允许编辑的用户ID
             * 
             * @param userIds 用户ID列表
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder users(List<String> userIds) {
                protectedDimension.setUsers(userIds);
                return this;
            }

            /**
             * 设置保护范围的备注信息
             * 
             * @param lockInfo 备注信息
             * @return 当前构建器
             */
            public AddProtectedDimensionBuilder lockInfo(String lockInfo) {
                protectedDimension.setLockInfo(lockInfo);
                return this;
            }

            /**
             * 构建保护范围请求
             * 
             * @return 保护范围请求
             */
            public ProtectedDimensionRequest build() {
                return request;
            }
        }
    }

    /**
     * 添加保护范围信息
     */
    public static class AddProtectedDimensionRange {
        private DimensionRange dimension;
        private List<String> users;
        private String lockInfo;

        public AddProtectedDimensionRange() {
            this.dimension = new DimensionRange();
            this.users = new ArrayList<>();
        }

        /**
         * 获取维度范围
         * 
         * @return 维度范围
         */
        public DimensionRange getDimension() {
            return dimension;
        }

        /**
         * 设置维度范围
         * 
         * @param dimension 维度范围
         */
        public void setDimension(DimensionRange dimension) {
            this.dimension = dimension;
        }

        /**
         * 获取允许编辑的用户ID列表
         * 
         * @return 用户ID列表
         */
        public List<String> getUsers() {
            return users;
        }

        /**
         * 设置允许编辑的用户ID列表
         * 
         * @param users 用户ID列表
         */
        public void setUsers(List<String> users) {
            this.users = users;
        }

        /**
         * 获取保护范围的备注信息
         * 
         * @return 备注信息
         */
        public String getLockInfo() {
            return lockInfo;
        }

        /**
         * 设置保护范围的备注信息
         * 
         * @param lockInfo 备注信息
         */
        public void setLockInfo(String lockInfo) {
            this.lockInfo = lockInfo;
        }
    }

    /**
     * 维度范围
     */
    public static class DimensionRange {
        private String sheetId;
        private String majorDimension;
        private Integer startIndex;
        private Integer endIndex;

        /**
         * 获取工作表ID
         * 
         * @return 工作表ID
         */
        public String getSheetId() {
            return sheetId;
        }

        /**
         * 设置工作表ID
         * 
         * @param sheetId 工作表ID
         */
        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }

        /**
         * 获取维度方向
         * 
         * @return 维度方向，ROWS（行）或COLUMNS（列）
         */
        public String getMajorDimension() {
            return majorDimension;
        }

        /**
         * 设置维度方向
         * 
         * @param majorDimension 维度方向，ROWS（行）或COLUMNS（列）
         */
        public void setMajorDimension(String majorDimension) {
            this.majorDimension = majorDimension;
        }

        /**
         * 获取开始索引
         * 
         * @return 开始索引，从1开始计数
         */
        public Integer getStartIndex() {
            return startIndex;
        }

        /**
         * 设置开始索引
         * 
         * @param startIndex 开始索引，从1开始计数
         */
        public void setStartIndex(Integer startIndex) {
            this.startIndex = startIndex;
        }

        /**
         * 获取结束索引
         * 
         * @return 结束索引，从1开始计数
         */
        public Integer getEndIndex() {
            return endIndex;
        }

        /**
         * 设置结束索引
         * 
         * @param endIndex 结束索引，从1开始计数
         */
        public void setEndIndex(Integer endIndex) {
            this.endIndex = endIndex;
        }
    }

    /**
     * 添加保护范围请求体
     */
    private static class AddProtectedDimensionRequestBody {
        private final List<AddProtectedDimensionRange> addProtectedDimension;

        /**
         * 构造函数
         * 
         * @param addProtectedDimension 添加保护范围信息列表
         */
        public AddProtectedDimensionRequestBody(List<AddProtectedDimensionRange> addProtectedDimension) {
            this.addProtectedDimension = addProtectedDimension;
        }

        /**
         * 获取添加保护范围信息列表
         * 
         * @return 添加保护范围信息列表
         */
        public List<AddProtectedDimensionRange> getAddProtectedDimension() {
            return addProtectedDimension;
        }
    }
}