package cn.isliu.core.service;


import okhttp3.Request;
import okhttp3.RequestBody;

import cn.isliu.core.client.FeishuApiClient;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义数据验证服务 提供官方SDK未覆盖的数据验证API
 */
public class CustomDataValidationService extends FeishuApiClient {

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public CustomDataValidationService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量处理数据验证请求
     * 
     * @param spreadsheetToken 电子表格Token
     * @param request 批量处理请求
     * @return 批量处理响应
     * @throws IOException 请求异常
     */
    public ApiResponse dataValidationBatchUpdate(String spreadsheetToken, DataValidationBatchUpdateRequest request)
        throws IOException {
        List<DataValidationRequest> requests = request.getRequests();
        ApiResponse response = null;

        // 如果没有请求，返回空响应
        if (requests == null || requests.isEmpty()) {
            ApiResponse emptyResponse = new ApiResponse();
            emptyResponse.setCode(400);
            emptyResponse.setMsg("No data validation operations found");
            return emptyResponse;
        }

        // 依次处理每个请求
        for (DataValidationRequest validationRequest : requests) {
            // 处理查询下拉列表请求
            if (validationRequest.getQueryValidation() != null) {
                QueryValidationRequest queryValidation = validationRequest.getQueryValidation();

                // 验证请求参数
                if (queryValidation.getRange() == null || queryValidation.getRange().isEmpty()) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("Range cannot be empty for data validation query");
                    return errorResponse;
                }

                // 构建基本URL
                String baseUrl = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dataValidation";

                // 构建查询参数
                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                urlBuilder.append("?range=")
                    .append(URLEncoder.encode(queryValidation.getRange(), StandardCharsets.UTF_8.toString()));

                // 添加dataValidationType参数
                urlBuilder.append("&dataValidationType=list");

                String url = urlBuilder.toString();
                Request httpRequest = createAuthenticatedRequest(url, "GET", null).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理删除下拉列表请求
            else if (validationRequest.getDeleteValidation() != null) {
                DeleteValidationRequest deleteValidation = validationRequest.getDeleteValidation();

                // 验证请求参数
                if (deleteValidation.getRange() == null || deleteValidation.getRange().isEmpty()) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("Range cannot be empty for data validation delete");
                    return errorResponse;
                }

                if (deleteValidation.getDataValidationIds() == null
                    || deleteValidation.getDataValidationIds().isEmpty()) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("DataValidationIds cannot be empty for data validation delete");
                    return errorResponse;
                }

                // 构建基本URL
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dataValidation";

                // 构建删除请求体
                DeleteValidationRequestBody requestBody = new DeleteValidationRequestBody();
                DeleteValidationRange validationRange = new DeleteValidationRange();
                validationRange.setRange(deleteValidation.getRange());
                validationRange.setDataValidationIds(deleteValidation.getDataValidationIds());
                requestBody.getDataValidationRanges().add(validationRange);

                RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "DELETE", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理设置下拉列表请求
            else if (validationRequest.getRange() != null && "list".equals(validationRequest.getDataValidationType())) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dataValidation";

                RequestBody body = RequestBody.create(gson.toJson(validationRequest), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理更新下拉列表请求
            else if (validationRequest.getSheetId() != null && validationRequest.getDataValidationId() != null
                && "list".equals(validationRequest.getDataValidationType())) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/dataValidation/"
                    + validationRequest.getSheetId() + "/" + validationRequest.getDataValidationId();

                // 创建新的请求体，不包含sheetId和dataValidationId字段
                DataValidationRequest requestBody = new DataValidationRequest();
                requestBody.setDataValidationType(validationRequest.getDataValidationType());
                requestBody.setDataValidation(validationRequest.getDataValidation());

                RequestBody body = RequestBody.create(gson.toJson(requestBody), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "PUT", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 这里可以添加其他数据验证类型的处理
        }

        // 如果所有请求都成功处理，返回最后一个成功的响应
        // 如果没有处理任何请求(没有有效的操作类型)，返回错误响应
        if (response == null) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setCode(400);
            errorResponse.setMsg("No valid data validation operation found");
            return errorResponse;
        }

        return response;
    }

    /**
     * 删除下拉列表设置请求体
     */
    private static class DeleteValidationRequestBody {
        private List<DeleteValidationRange> dataValidationRanges;

        public DeleteValidationRequestBody() {
            this.dataValidationRanges = new ArrayList<>();
        }

        public List<DeleteValidationRange> getDataValidationRanges() {
            return dataValidationRanges;
        }

        public void setDataValidationRanges(List<DeleteValidationRange> dataValidationRanges) {
            this.dataValidationRanges = dataValidationRanges;
        }
    }

    /**
     * 删除下拉列表设置范围
     */
    private static class DeleteValidationRange {
        private String range;
        private List<Integer> dataValidationIds;

        public String getRange() {
            return range;
        }

        public void setRange(String range) {
            this.range = range;
        }

        public List<Integer> getDataValidationIds() {
            return dataValidationIds;
        }

        public void setDataValidationIds(List<Integer> dataValidationIds) {
            this.dataValidationIds = dataValidationIds;
        }
    }

    /**
     * 批量处理数据验证请求
     */
    public static class DataValidationBatchUpdateRequest {
        private List<DataValidationRequest> requests;

        public DataValidationBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<DataValidationRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<DataValidationRequest> requests) {
            this.requests = requests;
        }

        /**
         * 创建批量处理数据验证请求的构建器
         * 
         * @return 批量处理数据验证请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量处理数据验证请求的构建器
         */
        public static class Builder {
            private final DataValidationBatchUpdateRequest request;

            public Builder() {
                request = new DataValidationBatchUpdateRequest();
            }

            /**
             * 添加一个数据验证请求
             * 
             * @param validationRequest 数据验证请求
             * @return 当前构建器
             */
            public Builder addRequest(DataValidationRequest validationRequest) {
                request.requests.add(validationRequest);
                return this;
            }

            /**
             * 构建批量处理数据验证请求
             * 
             * @return 批量处理数据验证请求
             */
            public DataValidationBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 数据验证请求
     */
    public static class DataValidationRequest {
        private String range;
        private String dataValidationType;
        private DataValidation dataValidation;
        private String sheetId;
        private Integer dataValidationId;
        private QueryValidationRequest queryValidation;
        private DeleteValidationRequest deleteValidation;

        /**
         * 获取单元格范围
         * 
         * @return 单元格范围
         */
        public String getRange() {
            return range;
        }

        /**
         * 设置单元格范围
         * 
         * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public void setRange(String range) {
            this.range = range;
        }

        /**
         * 获取数据验证类型
         * 
         * @return 数据验证类型
         */
        public String getDataValidationType() {
            return dataValidationType;
        }

        /**
         * 设置数据验证类型
         * 
         * @param dataValidationType 数据验证类型
         */
        public void setDataValidationType(String dataValidationType) {
            this.dataValidationType = dataValidationType;
        }

        /**
         * 获取数据验证规则
         * 
         * @return 数据验证规则
         */
        public DataValidation getDataValidation() {
            return dataValidation;
        }

        /**
         * 设置数据验证规则
         * 
         * @param dataValidation 数据验证规则
         */
        public void setDataValidation(DataValidation dataValidation) {
            this.dataValidation = dataValidation;
        }

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
         * 获取下拉列表ID
         * 
         * @return 下拉列表ID
         */
        public Integer getDataValidationId() {
            return dataValidationId;
        }

        /**
         * 设置下拉列表ID
         * 
         * @param dataValidationId 下拉列表ID
         */
        public void setDataValidationId(Integer dataValidationId) {
            this.dataValidationId = dataValidationId;
        }

        /**
         * 获取查询下拉列表设置请求
         * 
         * @return 查询下拉列表设置请求
         */
        public QueryValidationRequest getQueryValidation() {
            return queryValidation;
        }

        /**
         * 设置查询下拉列表设置请求
         * 
         * @param queryValidation 查询下拉列表设置请求
         */
        public void setQueryValidation(QueryValidationRequest queryValidation) {
            this.queryValidation = queryValidation;
        }

        /**
         * 获取删除下拉列表设置请求
         * 
         * @return 删除下拉列表设置请求
         */
        public DeleteValidationRequest getDeleteValidation() {
            return deleteValidation;
        }

        /**
         * 设置删除下拉列表设置请求
         * 
         * @param deleteValidation 删除下拉列表设置请求
         */
        public void setDeleteValidation(DeleteValidationRequest deleteValidation) {
            this.deleteValidation = deleteValidation;
        }

        /**
         * 创建设置下拉列表的请求构建器
         * 
         * @return 设置下拉列表的构建器
         */
        public static ListDataValidationBuilder listValidation() {
            return new ListDataValidationBuilder();
        }

        /**
         * 创建更新下拉列表的请求构建器
         * 
         * @return 更新下拉列表的构建器
         */
        public static UpdateListDataValidationBuilder updateListValidation() {
            return new UpdateListDataValidationBuilder();
        }

        /**
         * 创建查询下拉列表的请求构建器
         * 
         * @return 查询下拉列表的构建器
         */
        public static QueryListDataValidationBuilder queryListValidation() {
            return new QueryListDataValidationBuilder();
        }

        /**
         * 创建删除下拉列表的请求构建器
         * 
         * @return 删除下拉列表的构建器
         */
        public static DeleteListDataValidationBuilder deleteListValidation() {
            return new DeleteListDataValidationBuilder();
        }

        /**
         * 设置下拉列表的构建器
         */
        public static class ListDataValidationBuilder {
            private final DataValidationRequest request;
            private final DataValidation dataValidation;
            private final DataValidationOptions options;

            public ListDataValidationBuilder() {
                request = new DataValidationRequest();
                dataValidation = new DataValidation();
                options = new DataValidationOptions();

                request.setDataValidationType("list");
                dataValidation.setOptions(options);
                request.setDataValidation(dataValidation);
            }

            /**
             * 设置单元格范围
             * 
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public ListDataValidationBuilder range(String range) {
                request.setRange(range);
                return this;
            }

            /**
             * 设置单元格范围
             * 
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public ListDataValidationBuilder range(String sheetId, String startPosition, String endPosition) {
                request.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param value 选项值
             * @return 当前构建器
             */
            public ListDataValidationBuilder addValue(String value) {
                dataValidation.getConditionValues().add(value);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param values 选项值列表
             * @return 当前构建器
             */
            public ListDataValidationBuilder addValues(List<String> values) {
                dataValidation.getConditionValues().addAll(values);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param values 选项值数组
             * @return 当前构建器
             */
            public ListDataValidationBuilder addValues(String... values) {
                for (String value : values) {
                    dataValidation.getConditionValues().add(value);
                }
                return this;
            }

            /**
             * 设置是否支持多选
             * 
             * @param multipleValues 是否支持多选
             * @return 当前构建器
             */
            public ListDataValidationBuilder multipleValues(boolean multipleValues) {
                options.setMultipleValues(multipleValues);
                return this;
            }

            /**
             * 设置是否为下拉选项设置颜色
             * 
             * @param highlightValidData 是否为下拉选项设置颜色
             * @return 当前构建器
             */
            public ListDataValidationBuilder highlightValidData(boolean highlightValidData) {
                options.setHighlightValidData(highlightValidData);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param color 颜色，格式为RGB 16进制，如"#fffd00"
             * @return 当前构建器
             */
            public ListDataValidationBuilder addColor(String color) {
                options.getColors().add(color);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param colors 颜色列表
             * @return 当前构建器
             */
            public ListDataValidationBuilder addColors(List<String> colors) {
                options.getColors().addAll(colors);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param colors 颜色数组
             * @return 当前构建器
             */
            public ListDataValidationBuilder addColors(String... colors) {
                for (String color : colors) {
                    options.getColors().add(color);
                }
                return this;
            }

            /**
             * 构建设置下拉列表请求
             * 
             * @return 数据验证请求
             */
            public DataValidationRequest build() {
                return request;
            }
        }

        /**
         * 更新下拉列表的构建器
         */
        public static class UpdateListDataValidationBuilder {
            private final DataValidationRequest request;
            private final DataValidation dataValidation;
            private final DataValidationOptions options;

            public UpdateListDataValidationBuilder() {
                request = new DataValidationRequest();
                dataValidation = new DataValidation();
                options = new DataValidationOptions();

                request.setDataValidationType("list");
                dataValidation.setOptions(options);
                request.setDataValidation(dataValidation);
            }

            /**
             * 设置工作表ID和下拉列表ID
             * 
             * @param sheetId 工作表ID
             * @param dataValidationId 下拉列表ID
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder target(String sheetId, int dataValidationId) {
                request.setSheetId(sheetId);
                request.setDataValidationId(dataValidationId);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param value 选项值
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addValue(String value) {
                dataValidation.getConditionValues().add(value);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param values 选项值列表
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addValues(List<String> values) {
                dataValidation.getConditionValues().addAll(values);
                return this;
            }

            /**
             * 添加下拉选项值
             * 
             * @param values 选项值数组
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addValues(String... values) {
                for (String value : values) {
                    dataValidation.getConditionValues().add(value);
                }
                return this;
            }

            /**
             * 设置下拉选项值（替换现有值）
             * 
             * @param values 选项值列表
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder setValues(List<String> values) {
                dataValidation.setConditionValues(new ArrayList<>(values));
                return this;
            }

            /**
             * 设置下拉选项值（替换现有值）
             * 
             * @param values 选项值数组
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder setValues(String... values) {
                List<String> valueList = new ArrayList<>();
                for (String value : values) {
                    valueList.add(value);
                }
                dataValidation.setConditionValues(valueList);
                return this;
            }

            /**
             * 设置是否支持多选
             * 
             * @param multipleValues 是否支持多选
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder multipleValues(boolean multipleValues) {
                options.setMultipleValues(multipleValues);
                return this;
            }

            /**
             * 设置是否为下拉选项设置颜色
             * 
             * @param highlightValidData 是否为下拉选项设置颜色
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder highlightValidData(boolean highlightValidData) {
                options.setHighlightValidData(highlightValidData);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param color 颜色，格式为RGB 16进制，如"#fffd00"
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addColor(String color) {
                options.getColors().add(color);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param colors 颜色列表
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addColors(List<String> colors) {
                options.getColors().addAll(colors);
                return this;
            }

            /**
             * 添加下拉选项颜色
             * 
             * @param colors 颜色数组
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder addColors(String... colors) {
                for (String color : colors) {
                    options.getColors().add(color);
                }
                return this;
            }

            /**
             * 设置下拉选项颜色（替换现有颜色）
             * 
             * @param colors 颜色列表
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder setColors(List<String> colors) {
                options.setColors(new ArrayList<>(colors));
                return this;
            }

            /**
             * 设置下拉选项颜色（替换现有颜色）
             * 
             * @param colors 颜色数组
             * @return 当前构建器
             */
            public UpdateListDataValidationBuilder setColors(String... colors) {
                List<String> colorList = new ArrayList<>();
                for (String color : colors) {
                    colorList.add(color);
                }
                options.setColors(colorList);
                return this;
            }

            /**
             * 构建更新下拉列表请求
             * 
             * @return 数据验证请求
             */
            public DataValidationRequest build() {
                return request;
            }
        }

        /**
         * 查询下拉列表的构建器
         */
        public static class QueryListDataValidationBuilder {
            private final DataValidationRequest request;
            private final QueryValidationRequest queryValidation;

            public QueryListDataValidationBuilder() {
                request = new DataValidationRequest();
                queryValidation = new QueryValidationRequest();
                request.setQueryValidation(queryValidation);
            }

            /**
             * 设置单元格范围
             * 
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public QueryListDataValidationBuilder range(String range) {
                queryValidation.setRange(range);
                return this;
            }

            /**
             * 设置单元格范围
             * 
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public QueryListDataValidationBuilder range(String sheetId, String startPosition, String endPosition) {
                queryValidation.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 构建查询下拉列表请求
             * 
             * @return 数据验证请求
             */
            public DataValidationRequest build() {
                return request;
            }
        }

        /**
         * 删除下拉列表的构建器
         */
        public static class DeleteListDataValidationBuilder {
            private final DataValidationRequest request;
            private final DeleteValidationRequest deleteValidation;

            public DeleteListDataValidationBuilder() {
                request = new DataValidationRequest();
                deleteValidation = new DeleteValidationRequest();
                request.setDeleteValidation(deleteValidation);
            }

            /**
             * 设置单元格范围
             * 
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public DeleteListDataValidationBuilder range(String range) {
                deleteValidation.setRange(range);
                return this;
            }

            /**
             * 设置单元格范围
             * 
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public DeleteListDataValidationBuilder range(String sheetId, String startPosition, String endPosition) {
                deleteValidation.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加要删除的下拉列表ID
             * 
             * @param dataValidationId 下拉列表ID
             * @return 当前构建器
             */
            public DeleteListDataValidationBuilder addDataValidationId(int dataValidationId) {
                deleteValidation.getDataValidationIds().add(dataValidationId);
                return this;
            }

            /**
             * 添加要删除的下拉列表ID列表
             * 
             * @param dataValidationIds 下拉列表ID列表
             * @return 当前构建器
             */
            public DeleteListDataValidationBuilder addDataValidationIds(List<Integer> dataValidationIds) {
                deleteValidation.getDataValidationIds().addAll(dataValidationIds);
                return this;
            }

            /**
             * 添加要删除的下拉列表ID数组
             * 
             * @param dataValidationIds 下拉列表ID数组
             * @return 当前构建器
             */
            public DeleteListDataValidationBuilder addDataValidationIds(Integer... dataValidationIds) {
                for (Integer id : dataValidationIds) {
                    deleteValidation.getDataValidationIds().add(id);
                }
                return this;
            }

            /**
             * 构建删除下拉列表请求
             * 
             * @return 数据验证请求
             */
            public DataValidationRequest build() {
                return request;
            }
        }
    }

    /**
     * 查询下拉列表设置请求
     */
    public static class QueryValidationRequest {
        private String range;

        /**
         * 获取单元格范围
         * 
         * @return 单元格范围
         */
        public String getRange() {
            return range;
        }

        /**
         * 设置单元格范围
         * 
         * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public void setRange(String range) {
            this.range = range;
        }
    }

    /**
     * 删除下拉列表设置请求
     */
    public static class DeleteValidationRequest {
        private String range;
        private List<Integer> dataValidationIds;

        public DeleteValidationRequest() {
            this.dataValidationIds = new ArrayList<>();
        }

        /**
         * 获取单元格范围
         * 
         * @return 单元格范围
         */
        public String getRange() {
            return range;
        }

        /**
         * 设置单元格范围
         * 
         * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public void setRange(String range) {
            this.range = range;
        }

        /**
         * 获取要删除的下拉列表ID列表
         * 
         * @return 下拉列表ID列表
         */
        public List<Integer> getDataValidationIds() {
            return dataValidationIds;
        }

        /**
         * 设置要删除的下拉列表ID列表
         * 
         * @param dataValidationIds 下拉列表ID列表
         */
        public void setDataValidationIds(List<Integer> dataValidationIds) {
            this.dataValidationIds = dataValidationIds;
        }
    }

    /**
     * 数据验证规则
     */
    public static class DataValidation {
        private List<String> conditionValues;
        private DataValidationOptions options;

        public DataValidation() {
            this.conditionValues = new ArrayList<>();
        }

        /**
         * 获取条件值列表
         * 
         * @return 条件值列表
         */
        public List<String> getConditionValues() {
            return conditionValues;
        }

        /**
         * 设置条件值列表
         * 
         * @param conditionValues 条件值列表
         */
        public void setConditionValues(List<String> conditionValues) {
            this.conditionValues = conditionValues;
        }

        /**
         * 获取选项配置
         * 
         * @return 选项配置
         */
        public DataValidationOptions getOptions() {
            return options;
        }

        /**
         * 设置选项配置
         * 
         * @param options 选项配置
         */
        public void setOptions(DataValidationOptions options) {
            this.options = options;
        }
    }

    /**
     * 数据验证选项配置
     */
    public static class DataValidationOptions {
        private Boolean multipleValues;
        private Boolean highlightValidData;
        private List<String> colors;

        public DataValidationOptions() {
            this.colors = new ArrayList<>();
        }

        /**
         * 获取是否支持多选
         * 
         * @return 是否支持多选
         */
        public Boolean getMultipleValues() {
            return multipleValues;
        }

        /**
         * 设置是否支持多选
         * 
         * @param multipleValues 是否支持多选
         */
        public void setMultipleValues(Boolean multipleValues) {
            this.multipleValues = multipleValues;
        }

        /**
         * 获取是否为下拉选项设置颜色
         * 
         * @return 是否为下拉选项设置颜色
         */
        public Boolean getHighlightValidData() {
            return highlightValidData;
        }

        /**
         * 设置是否为下拉选项设置颜色
         * 
         * @param highlightValidData 是否为下拉选项设置颜色
         */
        public void setHighlightValidData(Boolean highlightValidData) {
            this.highlightValidData = highlightValidData;
        }

        /**
         * 获取颜色列表
         * 
         * @return 颜色列表
         */
        public List<String> getColors() {
            return colors;
        }

        /**
         * 设置颜色列表
         * 
         * @param colors 颜色列表
         */
        public void setColors(List<String> colors) {
            this.colors = colors;
        }
    }
}