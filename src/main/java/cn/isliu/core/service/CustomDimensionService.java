package cn.isliu.core.service;

import cn.isliu.core.client.FeishuApiClient;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义维度服务 提供官方SDK未覆盖的行列操作API
 */
public class CustomDimensionService extends FeishuApiClient {

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public CustomDimensionService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量操作行列 支持添加、插入等操作 支持处理多个请求，如果有请求失败则中断后续请求
     * 
     * @param spreadsheetToken 电子表格Token
     * @param request 批量操作请求
     * @return 批量操作响应
     * @throws IOException 请求异常
     */
    public ApiResponse dimensionsBatchUpdate(String spreadsheetToken,
        DimensionBatchUpdateRequest request) throws IOException {
        List<DimensionRequest> requests = request.getRequests();
        ApiResponse response = null;

        // 如果没有请求，返回空响应
        if (requests == null || requests.isEmpty()) {
            ApiResponse emptyResponse = new ApiResponse();
            emptyResponse.setCode(400);
            emptyResponse.setMsg("No dimension operations found");
            return emptyResponse;
        }

        // 依次处理每个请求
        for (DimensionRequest dimensionRequest : requests) {
            // 处理添加行列请求
            if (dimensionRequest.getAddDimension() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dimension_range";

                // 构建添加行列请求体
                RequestBody body = RequestBody.create(
                    gson.toJson(new AddDimensionRequestBody(dimensionRequest.getAddDimension())), JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理插入行列请求
            else if (dimensionRequest.getInsertDimension() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/insert_dimension_range";

                // 构建插入行列请求体
                RequestBody body =
                    RequestBody.create(gson.toJson(new InsertDimensionRequestBody(dimensionRequest.getInsertDimension(),
                        dimensionRequest.getInheritStyle())), JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理更新行列请求
            else if (dimensionRequest.getUpdateDimension() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dimension_range";

                // 构建更新行列请求体
                RequestBody body =
                    RequestBody.create(gson.toJson(new UpdateDimensionRequestBody(dimensionRequest.getUpdateDimension(),
                        dimensionRequest.getDimensionProperties())), JSON_MEDIA_TYPE);

                // 使用PUT方法
                Request httpRequest = createAuthenticatedRequest(url, "PUT", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理删除行列请求
            else if (dimensionRequest.getDeleteDimension() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dimension_range";

                // 构建删除行列请求体
                RequestBody body = RequestBody.create(
                    gson.toJson(new DeleteDimensionRequestBody(dimensionRequest.getDeleteDimension())),
                    JSON_MEDIA_TYPE);

                // 使用DELETE方法
                Request httpRequest = createAuthenticatedRequest(url, "DELETE", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
        }

        // 如果所有请求都成功处理，返回最后一个成功的响应
        // 如果没有处理任何请求(没有有效的操作类型)，返回错误响应
        if (response == null) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setCode(400);
            errorResponse.setMsg("No valid dimension operation found");
            return errorResponse;
        }

        return response;
    }

    /**
     * 批量操作行列请求
     */
    public static class DimensionBatchUpdateRequest {
        private List<DimensionRequest> requests;

        public DimensionBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<DimensionRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<DimensionRequest> requests) {
            this.requests = requests;
        }

        /**
         * 创建批量操作行列请求的构建器
         * 
         * @return 批量操作行列请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量操作行列请求的构建器
         */
        public static class Builder {
            private final DimensionBatchUpdateRequest request;

            public Builder() {
                request = new DimensionBatchUpdateRequest();
            }

            /**
             * 添加一个维度操作请求
             * 
             * @param dimensionRequest 维度操作请求，如添加行列或插入行列
             * @return 当前构建器
             */
            public Builder addRequest(DimensionRequest dimensionRequest) {
                request.requests.add(dimensionRequest);
                return this;
            }

            /**
             * 构建批量操作行列请求
             * 
             * @return 批量操作行列请求
             */
            public DimensionBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 行列操作请求
     */
    public static class DimensionRequest {
        private DimensionRange addDimension;
        private InsertDimensionRange insertDimension;
        private UpdateDimensionRange updateDimension;
        private UpdateDimensionRange deleteDimension;
        private DimensionProperties dimensionProperties;
        private String inheritStyle;

        /**
         * 获取添加行列的维度范围
         * 
         * @return 添加行列的维度范围
         */
        public DimensionRange getAddDimension() {
            return addDimension;
        }

        /**
         * 设置添加行列的维度范围
         * 
         * @param addDimension 添加行列的维度范围
         */
        public void setAddDimension(DimensionRange addDimension) {
            this.addDimension = addDimension;
        }

        /**
         * 获取插入行列的维度范围
         * 
         * @return 插入行列的维度范围
         */
        public InsertDimensionRange getInsertDimension() {
            return insertDimension;
        }

        /**
         * 设置插入行列的维度范围
         * 
         * @param insertDimension 插入行列的维度范围
         */
        public void setInsertDimension(InsertDimensionRange insertDimension) {
            this.insertDimension = insertDimension;
        }

        /**
         * 获取更新行列的维度范围
         * 
         * @return 更新行列的维度范围
         */
        public UpdateDimensionRange getUpdateDimension() {
            return updateDimension;
        }

        /**
         * 设置更新行列的维度范围
         * 
         * @param updateDimension 更新行列的维度范围
         */
        public void setUpdateDimension(UpdateDimensionRange updateDimension) {
            this.updateDimension = updateDimension;
        }

        /**
         * 获取删除行列的维度范围
         * 
         * @return 删除行列的维度范围
         */
        public UpdateDimensionRange getDeleteDimension() {
            return deleteDimension;
        }

        /**
         * 设置删除行列的维度范围
         * 
         * @param deleteDimension 删除行列的维度范围
         */
        public void setDeleteDimension(UpdateDimensionRange deleteDimension) {
            this.deleteDimension = deleteDimension;
        }

        /**
         * 获取维度属性
         * 
         * @return 维度属性
         */
        public DimensionProperties getDimensionProperties() {
            return dimensionProperties;
        }

        /**
         * 设置维度属性
         * 
         * @param dimensionProperties 维度属性
         */
        public void setDimensionProperties(DimensionProperties dimensionProperties) {
            this.dimensionProperties = dimensionProperties;
        }

        /**
         * 获取继承样式方式
         * 
         * @return 继承样式方式，可选值：BEFORE（继承前一行/列样式）、AFTER（继承后一行/列样式）
         */
        public String getInheritStyle() {
            return inheritStyle;
        }

        /**
         * 设置继承样式方式
         * 
         * @param inheritStyle 继承样式方式，可选值：BEFORE（继承前一行/列样式）、AFTER（继承后一行/列样式）
         */
        public void setInheritStyle(String inheritStyle) {
            this.inheritStyle = inheritStyle;
        }

        /**
         * 创建添加行列的维度请求构建器 用于在工作表末尾增加指定数量的行或列
         * 
         * @return 添加行列的构建器
         */
        public static AddDimensionBuilder addDimension() {
            return new AddDimensionBuilder();
        }

        /**
         * 创建插入行列的维度请求构建器 用于在工作表指定位置插入行或列
         * 
         * @return 插入行列的构建器
         */
        public static InsertDimensionBuilder insertDimension() {
            return new InsertDimensionBuilder();
        }

        /**
         * 创建更新行列的维度请求构建器 用于更新行或列的属性（如可见性、大小等）
         * 
         * @return 更新行列的构建器
         */
        public static UpdateDimensionBuilder updateDimension() {
            return new UpdateDimensionBuilder();
        }

        /**
         * 创建删除行列的维度请求构建器 用于构建删除指定范围行列的请求
         * 
         * @return 删除行列的构建器
         */
        public static DeleteDimensionBuilder deleteDimension() {
            return new DeleteDimensionBuilder();
        }

        /**
         * 添加行列的构建器 用于构建添加行列的请求
         */
        public static class AddDimensionBuilder {
            private final DimensionRequest request;
            private final DimensionRange dimension;

            public AddDimensionBuilder() {
                request = new DimensionRequest();
                dimension = new DimensionRange();
                request.setAddDimension(dimension);
            }

            /**
             * 设置工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public AddDimensionBuilder sheetId(String sheetId) {
                dimension.sheetId = sheetId;
                return this;
            }

            /**
             * 设置操作的维度类型
             * 
             * @param majorDimension 维度类型，可选值：ROWS（行）、COLUMNS（列）
             * @return 当前构建器
             */
            public AddDimensionBuilder majorDimension(String majorDimension) {
                dimension.majorDimension = majorDimension;
                return this;
            }

            /**
             * 设置要添加的行数或列数
             * 
             * @param length 要添加的数量，取值范围(0,5000]
             * @return 当前构建器
             */
            public AddDimensionBuilder length(Integer length) {
                dimension.length = length;
                return this;
            }

            /**
             * 构建添加行列请求
             * 
             * @return 维度操作请求
             */
            public DimensionRequest build() {
                return request;
            }
        }

        /**
         * 插入行列的构建器 用于构建在指定位置插入行列的请求
         */
        public static class InsertDimensionBuilder {
            private final DimensionRequest request;
            private final InsertDimensionRange dimension;

            public InsertDimensionBuilder() {
                request = new DimensionRequest();
                dimension = new InsertDimensionRange();
                request.setInsertDimension(dimension);
            }

            /**
             * 设置工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public InsertDimensionBuilder sheetId(String sheetId) {
                dimension.sheetId = sheetId;
                return this;
            }

            /**
             * 设置操作的维度类型
             * 
             * @param majorDimension 维度类型，可选值：ROWS（行）、COLUMNS（列）
             * @return 当前构建器
             */
            public InsertDimensionBuilder majorDimension(String majorDimension) {
                dimension.majorDimension = majorDimension;
                return this;
            }

            /**
             * 设置插入的起始位置
             * 
             * @param startIndex 起始位置索引，从0开始计数
             * @return 当前构建器
             */
            public InsertDimensionBuilder startIndex(Integer startIndex) {
                dimension.startIndex = startIndex;
                return this;
            }

            /**
             * 设置插入的结束位置
             * 
             * @param endIndex 结束位置索引，从0开始计数
             * @return 当前构建器
             */
            public InsertDimensionBuilder endIndex(Integer endIndex) {
                dimension.endIndex = endIndex;
                return this;
            }

            /**
             * 设置是否继承样式
             * 
             * @param inheritStyle 继承样式方式，可选值：BEFORE（继承前一行/列样式）、AFTER（继承后一行/列样式）
             * @return 当前构建器
             */
            public InsertDimensionBuilder inheritStyle(String inheritStyle) {
                request.inheritStyle = inheritStyle;
                return this;
            }

            /**
             * 构建插入行列请求
             * 
             * @return 维度操作请求
             */
            public DimensionRequest build() {
                return request;
            }
        }

        /**
         * 更新行列的构建器 用于构建更新行列属性的请求
         */
        public static class UpdateDimensionBuilder {
            private final DimensionRequest request;
            private final UpdateDimensionRange dimension;
            private final DimensionProperties properties;

            public UpdateDimensionBuilder() {
                request = new DimensionRequest();
                dimension = new UpdateDimensionRange();
                properties = new DimensionProperties();
                request.setUpdateDimension(dimension);
                request.setDimensionProperties(properties);
            }

            /**
             * 设置工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public UpdateDimensionBuilder sheetId(String sheetId) {
                dimension.sheetId = sheetId;
                return this;
            }

            /**
             * 设置操作的维度类型
             * 
             * @param majorDimension 维度类型，可选值：ROWS（行）、COLUMNS（列）
             * @return 当前构建器
             */
            public UpdateDimensionBuilder majorDimension(String majorDimension) {
                dimension.majorDimension = majorDimension;
                return this;
            }

            /**
             * 设置更新的起始位置
             * 
             * @param startIndex 起始位置索引，从1开始计数
             * @return 当前构建器
             */
            public UpdateDimensionBuilder startIndex(Integer startIndex) {
                dimension.startIndex = startIndex;
                return this;
            }

            /**
             * 设置更新的结束位置
             * 
             * @param endIndex 结束位置索引，从1开始计数
             * @return 当前构建器
             */
            public UpdateDimensionBuilder endIndex(Integer endIndex) {
                dimension.endIndex = endIndex;
                return this;
            }

            /**
             * 设置是否显示行或列
             * 
             * @param visible true表示显示，false表示隐藏
             * @return 当前构建器
             */
            public UpdateDimensionBuilder visible(Boolean visible) {
                properties.visible = visible;
                return this;
            }

            /**
             * 设置行高或列宽
             * 
             * @param fixedSize 行高或列宽，单位为像素，0表示隐藏
             * @return 当前构建器
             */
            public UpdateDimensionBuilder fixedSize(Integer fixedSize) {
                properties.fixedSize = fixedSize;
                return this;
            }

            /**
             * 构建更新行列请求
             * 
             * @return 维度操作请求
             */
            public DimensionRequest build() {
                return request;
            }
        }

        /**
         * 删除行列的构建器 用于构建删除指定范围行列的请求
         */
        public static class DeleteDimensionBuilder {
            private final DimensionRequest request;
            private final UpdateDimensionRange dimension;

            public DeleteDimensionBuilder() {
                request = new DimensionRequest();
                dimension = new UpdateDimensionRange();
                request.setDeleteDimension(dimension);
            }

            /**
             * 设置工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public DeleteDimensionBuilder sheetId(String sheetId) {
                dimension.sheetId = sheetId;
                return this;
            }

            /**
             * 设置操作的维度类型
             * 
             * @param majorDimension 维度类型，可选值：ROWS（行）、COLUMNS（列）
             * @return 当前构建器
             */
            public DeleteDimensionBuilder majorDimension(String majorDimension) {
                dimension.majorDimension = majorDimension;
                return this;
            }

            /**
             * 设置删除的起始位置
             * 
             * @param startIndex 起始位置索引，从1开始计数
             * @return 当前构建器
             */
            public DeleteDimensionBuilder startIndex(Integer startIndex) {
                dimension.startIndex = startIndex;
                return this;
            }

            /**
             * 设置删除的结束位置
             * 
             * @param endIndex 结束位置索引，从1开始计数
             * @return 当前构建器
             */
            public DeleteDimensionBuilder endIndex(Integer endIndex) {
                dimension.endIndex = endIndex;
                return this;
            }

            /**
             * 构建删除行列请求
             * 
             * @return 维度操作请求
             */
            public DimensionRequest build() {
                return request;
            }
        }
    }

    /**
     * 添加行列维度范围
     */
    public static class DimensionRange {
        /**
         * 电子表格工作表的ID 必填字段
         */
        private String sheetId;

        /**
         * 更新的维度 必填字段 可选值： ROWS：行 COLUMNS：列
         */
        private String majorDimension;

        /**
         * 要增加的行数或列数 必填字段 取值范围为(0,5000]
         */
        private Integer length;

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
         * 获取维度类型
         * 
         * @return 维度类型，ROWS表示行，COLUMNS表示列
         */
        public String getMajorDimension() {
            return majorDimension;
        }

        /**
         * 设置维度类型
         * 
         * @param majorDimension 维度类型，ROWS表示行，COLUMNS表示列
         */
        public void setMajorDimension(String majorDimension) {
            this.majorDimension = majorDimension;
        }

        /**
         * 获取要增加的行数或列数
         * 
         * @return 行数或列数
         */
        public Integer getLength() {
            return length;
        }

        /**
         * 设置要增加的行数或列数
         * 
         * @param length 行数或列数，取值范围(0,5000]
         */
        public void setLength(Integer length) {
            this.length = length;
        }
    }

    /**
     * 插入维度范围
     */
    public static class InsertDimensionRange {
        /**
         * 电子表格工作表的ID 必填字段
         */
        private String sheetId;

        /**
         * 要更新的维度 必填字段 可选值： ROWS：行 COLUMNS：列
         */
        private String majorDimension;

        /**
         * 插入的行或列的起始位置 必填字段 从0开始计数 若startIndex为3，则从第4行或列开始插入空行或列 包含第4行或列
         */
        private Integer startIndex;

        /**
         * 插入的行或列结束的位置 必填字段 从0开始计数 若endIndex为7，则从第8行结束插入行 第8行不再插入空行
         */
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
         * 获取维度类型
         * 
         * @return 维度类型，ROWS表示行，COLUMNS表示列
         */
        public String getMajorDimension() {
            return majorDimension;
        }

        /**
         * 设置维度类型
         * 
         * @param majorDimension 维度类型，ROWS表示行，COLUMNS表示列
         */
        public void setMajorDimension(String majorDimension) {
            this.majorDimension = majorDimension;
        }

        /**
         * 获取插入的起始位置
         * 
         * @return 起始位置索引
         */
        public Integer getStartIndex() {
            return startIndex;
        }

        /**
         * 设置插入的起始位置
         * 
         * @param startIndex 起始位置索引，从0开始计数
         */
        public void setStartIndex(Integer startIndex) {
            this.startIndex = startIndex;
        }

        /**
         * 获取插入的结束位置
         * 
         * @return 结束位置索引
         */
        public Integer getEndIndex() {
            return endIndex;
        }

        /**
         * 设置插入的结束位置
         * 
         * @param endIndex 结束位置索引，从0开始计数
         */
        public void setEndIndex(Integer endIndex) {
            this.endIndex = endIndex;
        }
    }

    /**
     * 更新维度范围
     */
    public static class UpdateDimensionRange {
        /**
         * 电子表格工作表的ID 必填字段
         */
        private String sheetId;

        /**
         * 要更新的维度 必填字段 可选值： ROWS：行 COLUMNS：列
         */
        private String majorDimension;

        /**
         * 要更新的行或列的起始位置 必填字段 从1开始计数 若startIndex为3，则从第3行或列开始更新属性 包含第3行或列
         */
        private Integer startIndex;

        /**
         * 要更新的行或列结束的位置 必填字段 从1开始计数 若endIndex为7，则更新至第7行结束 包含第7行
         */
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
         * 获取维度类型
         * 
         * @return 维度类型，ROWS表示行，COLUMNS表示列
         */
        public String getMajorDimension() {
            return majorDimension;
        }

        /**
         * 设置维度类型
         * 
         * @param majorDimension 维度类型，ROWS表示行，COLUMNS表示列
         */
        public void setMajorDimension(String majorDimension) {
            this.majorDimension = majorDimension;
        }

        /**
         * 获取更新的起始位置
         * 
         * @return 起始位置索引
         */
        public Integer getStartIndex() {
            return startIndex;
        }

        /**
         * 设置更新的起始位置
         * 
         * @param startIndex 起始位置索引，从1开始计数
         */
        public void setStartIndex(Integer startIndex) {
            this.startIndex = startIndex;
        }

        /**
         * 获取更新的结束位置
         * 
         * @return 结束位置索引
         */
        public Integer getEndIndex() {
            return endIndex;
        }

        /**
         * 设置更新的结束位置
         * 
         * @param endIndex 结束位置索引，从1开始计数
         */
        public void setEndIndex(Integer endIndex) {
            this.endIndex = endIndex;
        }
    }

    /**
     * 维度属性
     */
    public static class DimensionProperties {
        /**
         * 是否显示行或列 可选值： true：显示行或列 false：隐藏行或列
         */
        private Boolean visible;

        /**
         * 行高或列宽 单位为像素 fixedSize为0时，等价于隐藏行或列
         */
        private Integer fixedSize;

        /**
         * 获取是否显示
         * 
         * @return true表示显示，false表示隐藏
         */
        public Boolean getVisible() {
            return visible;
        }

        /**
         * 设置是否显示
         * 
         * @param visible true表示显示，false表示隐藏
         */
        public void setVisible(Boolean visible) {
            this.visible = visible;
        }

        /**
         * 获取行高或列宽
         * 
         * @return 行高或列宽，单位为像素
         */
        public Integer getFixedSize() {
            return fixedSize;
        }

        /**
         * 设置行高或列宽
         * 
         * @param fixedSize 行高或列宽，单位为像素，0表示隐藏
         */
        public void setFixedSize(Integer fixedSize) {
            this.fixedSize = fixedSize;
        }
    }

    /**
     * 添加行列请求体（用于API请求）
     */
    private static class AddDimensionRequestBody {
        private final DimensionRange dimension;

        /**
         * 构造函数
         * 
         * @param dimension 维度范围
         */
        public AddDimensionRequestBody(DimensionRange dimension) {
            this.dimension = dimension;
        }

        /**
         * 获取维度范围
         * 
         * @return 维度范围
         */
        public DimensionRange getDimension() {
            return dimension;
        }
    }

    /**
     * 插入行列请求体（用于API请求）
     */
    private static class InsertDimensionRequestBody {
        private final InsertDimensionRange dimension;
        private final String inheritStyle;

        /**
         * 构造函数
         * 
         * @param dimension 维度范围
         * @param inheritStyle 继承样式方式
         */
        public InsertDimensionRequestBody(InsertDimensionRange dimension, String inheritStyle) {
            this.dimension = dimension;
            this.inheritStyle = inheritStyle;
        }

        /**
         * 获取维度范围
         * 
         * @return 维度范围
         */
        public InsertDimensionRange getDimension() {
            return dimension;
        }

        /**
         * 获取继承样式方式
         * 
         * @return 继承样式方式
         */
        public String getInheritStyle() {
            return inheritStyle;
        }
    }

    /**
     * 更新行列请求体（用于API请求）
     */
    private static class UpdateDimensionRequestBody {
        private final UpdateDimensionRange dimension;
        private final DimensionProperties dimensionProperties;

        /**
         * 构造函数
         * 
         * @param dimension 维度范围
         * @param dimensionProperties 维度属性
         */
        public UpdateDimensionRequestBody(UpdateDimensionRange dimension, DimensionProperties dimensionProperties) {
            this.dimension = dimension;
            this.dimensionProperties = dimensionProperties;
        }

        /**
         * 获取维度范围
         * 
         * @return 维度范围
         */
        public UpdateDimensionRange getDimension() {
            return dimension;
        }

        /**
         * 获取维度属性
         * 
         * @return 维度属性
         */
        public DimensionProperties getDimensionProperties() {
            return dimensionProperties;
        }
    }

    /**
     * 删除行列请求体（用于API请求）
     */
    private static class DeleteDimensionRequestBody {
        private final UpdateDimensionRange dimension;

        /**
         * 构造函数
         * 
         * @param dimension 维度范围
         */
        public DeleteDimensionRequestBody(UpdateDimensionRange dimension) {
            this.dimension = dimension;
        }

        /**
         * 获取维度范围
         * 
         * @return 维度范围
         */
        public UpdateDimensionRange getDimension() {
            return dimension;
        }
    }
}