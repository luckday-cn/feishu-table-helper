package cn.isliu.core.service;

import cn.isliu.core.client.FeishuApiClient;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义数据值服务 提供官方SDK未覆盖的数据操作API
 */
public class CustomValueService extends FeishuApiClient {

    /**
     * 构造函数
     *
     * @param feishuClient 飞书客户端
     */
    public CustomValueService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量操作数据值 支持在指定范围前插入数据、在指定范围后追加数据等操作
     *
     * @param spreadsheetToken 电子表格Token
     * @param request 批量操作请求
     * @return 批量操作响应
     * @throws IOException 请求异常
     */
    public ApiResponse valueBatchUpdate(String spreadsheetToken, ValueBatchUpdateRequest request) throws IOException {
        List<ValueRequest> requests = request.getRequests();
        ApiResponse response = null;

        // 如果没有请求，返回空响应
        if (requests == null || requests.isEmpty()) {
            ApiResponse emptyResponse = new ApiResponse();
            emptyResponse.setCode(400);
            emptyResponse.setMsg("No value operations found");
            return emptyResponse;
        }

        // 依次处理每个请求
        for (ValueRequest valueRequest : requests) {
            // 处理在指定范围前插入数据请求
            if (valueRequest.getPrependValues() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values_prepend";

                RequestBody body = RequestBody.create(gson.toJson(valueRequest.getPrependValues()), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理在指定范围后追加数据请求
            else if (valueRequest.getAppendValues() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values_append";

                // 添加insertDataOption参数
                String insertDataOption = valueRequest.getAppendValues().getInsertDataOption();
                if (insertDataOption != null && !insertDataOption.isEmpty()) {
                    url += "?insertDataOption=" + insertDataOption;
                }

                RequestBody body = RequestBody.create(gson.toJson(valueRequest.getAppendValues()), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理写入图片请求
            else if (valueRequest.getImageValues() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values_image";

                RequestBody body = RequestBody.create(gson.toJson(valueRequest.getImageValues()), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理获取单个范围请求
            else if (valueRequest.getGetValues() != null) {
                ValueGetRequest getValues = valueRequest.getGetValues();
                String baseUrl =
                    BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values/" + getValues.getRange();

                // 构建查询参数
                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                boolean hasParam = false;

                // 添加valueRenderOption参数
                if (getValues.getValueRenderOption() != null && !getValues.getValueRenderOption().isEmpty()) {
                    urlBuilder.append(hasParam ? "&" : "?").append("valueRenderOption=")
                        .append(URLEncoder.encode(getValues.getValueRenderOption(), StandardCharsets.UTF_8.toString()));
                    hasParam = true;
                }

                // 添加dateTimeRenderOption参数
                if (getValues.getDateTimeRenderOption() != null && !getValues.getDateTimeRenderOption().isEmpty()) {
                    urlBuilder.append(hasParam ? "&" : "?").append("dateTimeRenderOption=").append(
                        URLEncoder.encode(getValues.getDateTimeRenderOption(), StandardCharsets.UTF_8.toString()));
                    hasParam = true;
                }

                // 添加user_id_type参数
                if (getValues.getUserIdType() != null && !getValues.getUserIdType().isEmpty()) {
                    urlBuilder.append(hasParam ? "&" : "?").append("user_id_type=")
                        .append(URLEncoder.encode(getValues.getUserIdType(), StandardCharsets.UTF_8.toString()));
                }

                String url = urlBuilder.toString();
                Request httpRequest = createAuthenticatedRequest(url, "GET", null).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理批量获取多个范围请求
            else if (valueRequest.getBatchGetValues() != null) {
                ValueBatchGetRequest batchGetValues = valueRequest.getBatchGetValues();

                // 检查ranges是否为空
                if (batchGetValues.getRanges() == null || batchGetValues.getRanges().isEmpty()) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("Ranges cannot be empty for batch get values");
                    return errorResponse;
                }

                // 构建ranges参数，使用逗号分隔
                StringBuilder rangesBuilder = new StringBuilder();
                for (int i = 0; i < batchGetValues.getRanges().size(); i++) {
                    if (i > 0) {
                        rangesBuilder.append(",");
                    }
                    rangesBuilder.append(batchGetValues.getRanges().get(i));
                }

                // 构建基本URL
                String baseUrl = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values_batch_get";

                // 构建查询参数
                StringBuilder urlBuilder = new StringBuilder(baseUrl);
                urlBuilder.append("?ranges=")
                    .append(URLEncoder.encode(rangesBuilder.toString(), StandardCharsets.UTF_8.toString()));

                // 添加valueRenderOption参数
                if (batchGetValues.getValueRenderOption() != null && !batchGetValues.getValueRenderOption().isEmpty()) {
                    urlBuilder.append("&valueRenderOption=").append(
                        URLEncoder.encode(batchGetValues.getValueRenderOption(), StandardCharsets.UTF_8.toString()));
                }

                // 添加dateTimeRenderOption参数
                if (batchGetValues.getDateTimeRenderOption() != null
                    && !batchGetValues.getDateTimeRenderOption().isEmpty()) {
                    urlBuilder.append("&dateTimeRenderOption=").append(
                        URLEncoder.encode(batchGetValues.getDateTimeRenderOption(), StandardCharsets.UTF_8.toString()));
                }

                // 添加user_id_type参数
                if (batchGetValues.getUserIdType() != null && !batchGetValues.getUserIdType().isEmpty()) {
                    urlBuilder.append("&user_id_type=")
                        .append(URLEncoder.encode(batchGetValues.getUserIdType(), StandardCharsets.UTF_8.toString()));
                }

                String url = urlBuilder.toString();
                Request httpRequest = createAuthenticatedRequest(url, "GET", null).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理向单个范围写入数据请求
            else if (valueRequest.getPutValues() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values";

                RequestBody body = RequestBody.create(gson.toJson(valueRequest.getPutValues()), JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "PUT", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理向多个范围写入数据请求
            else if (valueRequest.getBatchPutValues() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/values_batch_update";
                String params = gson.toJson(valueRequest.getBatchPutValues());
                RequestBody body = RequestBody.create(params, JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 这里可以添加其他数据操作类型
        }

        // 如果所有请求都成功处理，返回最后一个成功的响应
        // 如果没有处理任何请求(没有有效的操作类型)，返回错误响应
        if (response == null) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setCode(400);
            errorResponse.setMsg("No valid value operation found");
            return errorResponse;
        }

        return response;
    }

    /**
     * 批量操作数据值请求
     */
    public static class ValueBatchUpdateRequest {
        private List<ValueRequest> requests;

        public ValueBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<ValueRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<ValueRequest> requests) {
            this.requests = requests;
        }

        /**
         * 创建批量操作数据值请求的构建器
         *
         * @return 批量操作数据值请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量操作数据值请求的构建器
         */
        public static class Builder {
            private final ValueBatchUpdateRequest request;

            public Builder() {
                request = new ValueBatchUpdateRequest();
            }

            /**
             * 添加一个数据值操作请求
             *
             * @param valueRequest 数据值操作请求
             * @return 当前构建器
             */
            public Builder addRequest(ValueRequest valueRequest) {
                request.requests.add(valueRequest);
                return this;
            }

            /**
             * 构建批量操作数据值请求
             *
             * @return 批量操作数据值请求
             */
            public ValueBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 数据值操作请求
     */
    public static class ValueRequest {
        private ValuePrependRequest prependValues;
        private ValueAppendRequest appendValues;
        private ValueImageRequest imageValues;
        private ValueGetRequest getValues;
        private ValueBatchGetRequest batchGetValues;
        private ValuePutRequest putValues;
        private ValueBatchUpdatePutRequest batchPutValues;

        /**
         * 获取在指定范围前插入数据请求
         *
         * @return 在指定范围前插入数据请求
         */
        public ValuePrependRequest getPrependValues() {
            return prependValues;
        }

        /**
         * 设置在指定范围前插入数据请求
         *
         * @param prependValues 在指定范围前插入数据请求
         */
        public void setPrependValues(ValuePrependRequest prependValues) {
            this.prependValues = prependValues;
        }

        /**
         * 获取在指定范围后追加数据请求
         *
         * @return 在指定范围后追加数据请求
         */
        public ValueAppendRequest getAppendValues() {
            return appendValues;
        }

        /**
         * 设置在指定范围后追加数据请求
         *
         * @param appendValues 在指定范围后追加数据请求
         */
        public void setAppendValues(ValueAppendRequest appendValues) {
            this.appendValues = appendValues;
        }

        /**
         * 获取写入图片请求
         *
         * @return 写入图片请求
         */
        public ValueImageRequest getImageValues() {
            return imageValues;
        }

        /**
         * 设置写入图片请求
         *
         * @param imageValues 写入图片请求
         */
        public void setImageValues(ValueImageRequest imageValues) {
            this.imageValues = imageValues;
        }

        /**
         * 获取单个范围数据请求
         *
         * @return 获取单个范围数据请求
         */
        public ValueGetRequest getGetValues() {
            return getValues;
        }

        /**
         * 设置获取单个范围数据请求
         *
         * @param getValues 获取单个范围数据请求
         */
        public void setGetValues(ValueGetRequest getValues) {
            this.getValues = getValues;
        }

        /**
         * 获取批量获取多个范围数据请求
         *
         * @return 批量获取多个范围数据请求
         */
        public ValueBatchGetRequest getBatchGetValues() {
            return batchGetValues;
        }

        /**
         * 设置批量获取多个范围数据请求
         *
         * @param batchGetValues 批量获取多个范围数据请求
         */
        public void setBatchGetValues(ValueBatchGetRequest batchGetValues) {
            this.batchGetValues = batchGetValues;
        }

        /**
         * 获取向单个范围写入数据请求
         *
         * @return 向单个范围写入数据请求
         */
        public ValuePutRequest getPutValues() {
            return putValues;
        }

        /**
         * 设置向单个范围写入数据请求
         *
         * @param putValues 向单个范围写入数据请求
         */
        public void setPutValues(ValuePutRequest putValues) {
            this.putValues = putValues;
        }

        /**
         * 获取向多个范围写入数据请求
         *
         * @return 向多个范围写入数据请求
         */
        public ValueBatchUpdatePutRequest getBatchPutValues() {
            return batchPutValues;
        }

        /**
         * 设置向多个范围写入数据请求
         *
         * @param batchPutValues 向多个范围写入数据请求
         */
        public void setBatchPutValues(ValueBatchUpdatePutRequest batchPutValues) {
            this.batchPutValues = batchPutValues;
        }

        /**
         * 创建在指定范围前插入数据的请求构建器
         *
         * @return 在指定范围前插入数据的构建器
         */
        public static PrependValuesBuilder prependValues() {
            return new PrependValuesBuilder();
        }

        /**
         * 创建在指定范围后追加数据的请求构建器
         *
         * @return 在指定范围后追加数据的构建器
         */
        public static AppendValuesBuilder appendValues() {
            return new AppendValuesBuilder();
        }

        /**
         * 创建写入图片的请求构建器
         *
         * @return 写入图片的构建器
         */
        public static ImageValuesBuilder imageValues() {
            return new ImageValuesBuilder();
        }

        /**
         * 创建获取单个范围数据的请求构建器
         *
         * @return 获取单个范围数据的构建器
         */
        public static GetValuesBuilder getValues() {
            return new GetValuesBuilder();
        }

        /**
         * 创建批量获取多个范围数据的请求构建器
         *
         * @return 批量获取多个范围数据的构建器
         */
        public static BatchGetValuesBuilder batchGetValues() {
            return new BatchGetValuesBuilder();
        }

        /**
         * 创建向单个范围写入数据的请求构建器
         *
         * @return 向单个范围写入数据的构建器
         */
        public static PutValuesBuilder putValues() {
            return new PutValuesBuilder();
        }

        /**
         * 创建向多个范围写入数据的请求构建器
         *
         * @return 向多个范围写入数据的构建器
         */
        public static BatchPutValuesBuilder batchPutValues() {
            return new BatchPutValuesBuilder();
        }

        /**
         * 在指定范围前插入数据的构建器
         */
        public static class PrependValuesBuilder {
            private final ValueRequest request;
            private final ValuePrependRequest prependValues;
            private final ValueRange valueRange;

            public PrependValuesBuilder() {
                request = new ValueRequest();
                prependValues = new ValuePrependRequest();
                valueRange = new ValueRange();
                prependValues.setValueRange(valueRange);
                request.setPrependValues(prependValues);
            }

            /**
             * 设置要插入数据的单元格范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public PrependValuesBuilder range(String range) {
                valueRange.setRange(range);
                return this;
            }

            /**
             * 设置要插入数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public PrependValuesBuilder range(String sheetId, String startPosition, String endPosition) {
                valueRange.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public PrependValuesBuilder addRow(List<Object> rowData) {
                valueRange.getValues().add(rowData);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public PrependValuesBuilder addRow(Object... values) {
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                valueRange.getValues().add(row);
                return this;
            }

            /**
             * 构建在指定范围前插入数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 在指定范围后追加数据的构建器
         */
        public static class AppendValuesBuilder {
            private final ValueRequest request;
            private final ValueAppendRequest appendValues;
            private final ValueRange valueRange;

            public AppendValuesBuilder() {
                request = new ValueRequest();
                appendValues = new ValueAppendRequest();
                valueRange = new ValueRange();
                appendValues.setValueRange(valueRange);
                request.setAppendValues(appendValues);
            }

            /**
             * 设置要追加数据的单元格范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public AppendValuesBuilder range(String range) {
                valueRange.setRange(range);
                return this;
            }

            /**
             * 设置要追加数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public AppendValuesBuilder range(String sheetId, String startPosition, String endPosition) {
                valueRange.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 设置追加数据的方式
             *
             * @param insertDataOption 追加数据方式，可选值：OVERWRITE（默认，覆盖已有数据）、INSERT_ROWS（插入行后追加）
             * @return 当前构建器
             */
            public AppendValuesBuilder insertDataOption(String insertDataOption) {
                appendValues.setInsertDataOption(insertDataOption);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public AppendValuesBuilder addRow(List<Object> rowData) {
                valueRange.getValues().add(rowData);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public AppendValuesBuilder addRow(Object... values) {
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                valueRange.getValues().add(row);
                return this;
            }

            /**
             * 构建在指定范围后追加数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 写入图片的构建器
         */
        public static class ImageValuesBuilder {
            private final ValueRequest request;
            private final ValueImageRequest imageValues;

            public ImageValuesBuilder() {
                request = new ValueRequest();
                imageValues = new ValueImageRequest();
                request.setImageValues(imageValues);
            }

            /**
             * 设置要写入图片的单元格范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<单元格位置>:<单元格位置>
             * @return 当前构建器
             */
            public ImageValuesBuilder range(String range) {
                imageValues.setRange(range);
                return this;
            }

            /**
             * 设置要写入图片的单元格范围
             *
             * @param sheetId 工作表ID
             * @param position 单元格位置
             * @return 当前构建器
             */
            public ImageValuesBuilder range(String sheetId, String position) {
                imageValues.setRange(sheetId + "!" + position + ":" + position);
                return this;
            }

            /**
             * 设置要写入图片的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置，应与开始位置相同
             * @return 当前构建器
             */
            public ImageValuesBuilder range(String sheetId, String startPosition, String endPosition) {
                imageValues.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 设置要写入的图片数据
             *
             * @param image 图片二进制数据
             * @return 当前构建器
             */
            public ImageValuesBuilder image(byte[] image) {
                imageValues.setImage(image);
                return this;
            }

            /**
             * 设置图片名称
             *
             * @param name 图片名称，需要包含后缀名，如"test.png"
             * @return 当前构建器
             */
            public ImageValuesBuilder name(String name) {
                imageValues.setName(name);
                return this;
            }

            /**
             * 构建写入图片请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 获取单个范围数据的构建器
         */
        public static class GetValuesBuilder {
            private final ValueRequest request;
            private final ValueGetRequest getValues;

            public GetValuesBuilder() {
                request = new ValueRequest();
                getValues = new ValueGetRequest();
                request.setGetValues(getValues);
            }

            /**
             * 设置要获取数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public GetValuesBuilder range(String sheetId, String startPosition, String endPosition) {
                getValues.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 设置单元格数据的格式
             *
             * @param valueRenderOption 单元格数据格式，可选值： - ToString：返回纯文本的值（数值类型除外） - Formula：单元格中含有公式时，返回公式本身 -
             *            FormattedValue：计算并格式化单元格 - UnformattedValue：计算但不对单元格进行格式化
             * @return 当前构建器
             */
            public GetValuesBuilder valueRenderOption(String valueRenderOption) {
                getValues.setValueRenderOption(valueRenderOption);
                return this;
            }

            /**
             * 设置日期时间单元格数据的格式
             *
             * @param dateTimeRenderOption 日期时间单元格数据格式，可选值： - FormattedString：返回格式化后的字符串
             * @return 当前构建器
             */
            public GetValuesBuilder dateTimeRenderOption(String dateTimeRenderOption) {
                getValues.setDateTimeRenderOption(dateTimeRenderOption);
                return this;
            }

            /**
             * 设置用户ID类型
             *
             * @param userIdType 用户ID类型，可选值： - open_id：标识一个用户在某个应用中的身份 - union_id：标识一个用户在某个应用开发商下的身份
             * @return 当前构建器
             */
            public GetValuesBuilder userIdType(String userIdType) {
                getValues.setUserIdType(userIdType);
                return this;
            }

            /**
             * 构建获取单个范围数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 批量获取多个范围数据的构建器
         */
        public static class BatchGetValuesBuilder {
            private final ValueRequest request;
            private final ValueBatchGetRequest batchGetValues;

            public BatchGetValuesBuilder() {
                request = new ValueRequest();
                batchGetValues = new ValueBatchGetRequest();
                request.setBatchGetValues(batchGetValues);
            }

            /**
             * 添加要获取数据的单元格范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public BatchGetValuesBuilder addRange(String range) {
                batchGetValues.addRange(range);
                return this;
            }

            /**
             * 添加要获取数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public BatchGetValuesBuilder addRange(String sheetId, String startPosition, String endPosition) {
                String range = sheetId + "!" + startPosition + ":" + endPosition;
                batchGetValues.addRange(range);
                return this;
            }

            /**
             * 设置单元格数据的格式
             *
             * @param valueRenderOption 单元格数据格式，可选值： - ToString：返回纯文本的值（数值类型除外） - Formula：单元格中含有公式时，返回公式本身 -
             *            FormattedValue：计算并格式化单元格 - UnformattedValue：计算但不对单元格进行格式化
             * @return 当前构建器
             */
            public BatchGetValuesBuilder valueRenderOption(String valueRenderOption) {
                batchGetValues.setValueRenderOption(valueRenderOption);
                return this;
            }

            /**
             * 设置日期时间单元格数据的格式
             *
             * @param dateTimeRenderOption 日期时间单元格数据格式，可选值： - FormattedString：返回格式化后的字符串
             * @return 当前构建器
             */
            public BatchGetValuesBuilder dateTimeRenderOption(String dateTimeRenderOption) {
                batchGetValues.setDateTimeRenderOption(dateTimeRenderOption);
                return this;
            }

            /**
             * 设置用户ID类型
             *
             * @param userIdType 用户ID类型，可选值： - open_id：标识一个用户在某个应用中的身份 - union_id：标识一个用户在某个应用开发商下的身份
             * @return 当前构建器
             */
            public BatchGetValuesBuilder userIdType(String userIdType) {
                batchGetValues.setUserIdType(userIdType);
                return this;
            }

            /**
             * 构建批量获取多个范围数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 向单个范围写入数据的构建器
         */
        public static class PutValuesBuilder {
            private final ValueRequest request;
            private final ValuePutRequest putValues;
            private final ValueRange valueRange;

            public PutValuesBuilder() {
                request = new ValueRequest();
                putValues = new ValuePutRequest();
                valueRange = new ValueRange();
                putValues.setValueRange(valueRange);
                request.setPutValues(putValues);
            }

            /**
             * 设置要写入数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public PutValuesBuilder range(String sheetId, String startPosition, String endPosition) {
                valueRange.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public PutValuesBuilder addRow(List<Object> rowData) {
                valueRange.getValues().add(rowData);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public PutValuesBuilder addRow(Object... values) {
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                valueRange.getValues().add(row);
                return this;
            }

            /**
             * 构建向单个范围写入数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }

        /**
         * 向多个范围写入数据的构建器
         */
        public static class BatchPutValuesBuilder {
            private final ValueRequest request;
            private final ValueBatchUpdatePutRequest batchPutValues;
            private ValueRangeItem currentItem;

            public BatchPutValuesBuilder() {
                request = new ValueRequest();
                batchPutValues = new ValueBatchUpdatePutRequest();
                request.setBatchPutValues(batchPutValues);
            }

            public BatchPutValuesBuilder setReqType(String reqType) {
                batchPutValues.setType(reqType);
                return this;
            }

            public BatchPutValuesBuilder setReqParams(String reqParams) {
                batchPutValues.setParams(reqParams);
                return this;
            }

            /**
             * 添加一个新的范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public BatchPutValuesBuilder addRange(String range) {
                currentItem = new ValueRangeItem();
                currentItem.setRange(range);
                batchPutValues.getValueRanges().add(currentItem);
                return this;
            }

            public BatchPutValuesBuilder setType(String type) {
                currentItem.setType(type);
                return this;
            }

            /**
             * 添加一个新的范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public BatchPutValuesBuilder addRange(String sheetId, String startPosition, String endPosition) {
                String range = sheetId + "!" + startPosition + ":" + endPosition;
                return addRange(range);
            }

            /**
             * 为当前范围添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public BatchPutValuesBuilder addRow(List<Object> rowData) {
                if (currentItem == null) {
                    throw new IllegalStateException("Must call addRange before addRow");
                }
                currentItem.getValues().add(rowData);
                return this;
            }

            /**
             * 为当前范围添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public BatchPutValuesBuilder addRow(Object... values) {
                if (currentItem == null) {
                    throw new IllegalStateException("Must call addRange before addRow");
                }
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                currentItem.getValues().add(row);
                return this;
            }

            /**
             * 构建向多个范围写入数据请求
             *
             * @return 数据值操作请求
             */
            public ValueRequest build() {
                return request;
            }
        }
    }

    /**
     * 值范围
     */
    public static class ValueRange {
        private String range;
        private List<List<Object>> values;

        public ValueRange() {
            this.values = new ArrayList<>();
        }

        public String getRange() {
            return range;
        }

        public void setRange(String range) {
            this.range = range;
        }

        public List<List<Object>> getValues() {
            return values;
        }

        public void setValues(List<List<Object>> values) {
            this.values = values;
        }
    }

    /**
     * 在指定范围前插入数据请求
     */
    public static class ValuePrependRequest {
        private ValueRange valueRange;

        public ValuePrependRequest() {}

        public ValueRange getValueRange() {
            return valueRange;
        }

        public void setValueRange(ValueRange valueRange) {
            this.valueRange = valueRange;
        }

        /**
         * 创建插入数据请求的构建器
         *
         * @return 插入数据请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 插入数据请求的构建器
         */
        public static class Builder {
            private final ValuePrependRequest request;
            private final ValueRange valueRange;

            public Builder() {
                request = new ValuePrependRequest();
                valueRange = new ValueRange();
                request.setValueRange(valueRange);
            }

            /**
             * 设置要插入数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public Builder range(String sheetId, String startPosition, String endPosition) {
                valueRange.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public Builder addRow(List<Object> rowData) {
                valueRange.getValues().add(rowData);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public Builder addRow(Object... values) {
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                valueRange.getValues().add(row);
                return this;
            }

            /**
             * 构建插入数据请求
             *
             * @return 插入数据请求
             */
            public ValuePrependRequest build() {
                return request;
            }
        }
    }

    /**
     * 在指定范围后追加数据请求
     */
    public static class ValueAppendRequest {
        private ValueRange valueRange;
        private String insertDataOption;

        public ValueAppendRequest() {}

        public ValueRange getValueRange() {
            return valueRange;
        }

        public void setValueRange(ValueRange valueRange) {
            this.valueRange = valueRange;
        }

        /**
         * 获取追加数据的方式
         *
         * @return 追加数据方式，可选值：OVERWRITE（默认，覆盖已有数据）、INSERT_ROWS（插入行后追加）
         */
        public String getInsertDataOption() {
            return insertDataOption;
        }

        /**
         * 设置追加数据的方式
         *
         * @param insertDataOption 追加数据方式，可选值：OVERWRITE（默认，覆盖已有数据）、INSERT_ROWS（插入行后追加）
         */
        public void setInsertDataOption(String insertDataOption) {
            this.insertDataOption = insertDataOption;
        }

        /**
         * 创建追加数据请求的构建器
         *
         * @return 追加数据请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 追加数据请求的构建器
         */
        public static class Builder {
            private final ValueAppendRequest request;
            private final ValueRange valueRange;

            public Builder() {
                request = new ValueAppendRequest();
                valueRange = new ValueRange();
                request.setValueRange(valueRange);
            }

            /**
             * 设置要追加数据的单元格范围
             *
             * @param sheetId 工作表ID
             * @param startPosition 开始位置
             * @param endPosition 结束位置
             * @return 当前构建器
             */
            public Builder range(String sheetId, String startPosition, String endPosition) {
                valueRange.setRange(sheetId + "!" + startPosition + ":" + endPosition);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param rowData 行数据
             * @return 当前构建器
             */
            public Builder addRow(List<Object> rowData) {
                valueRange.getValues().add(rowData);
                return this;
            }

            /**
             * 添加一行数据
             *
             * @param values 行数据
             * @return 当前构建器
             */
            public Builder addRow(Object... values) {
                List<Object> row = new ArrayList<>();
                for (Object value : values) {
                    row.add(value);
                }
                valueRange.getValues().add(row);
                return this;
            }

            /**
             * 构建追加数据请求
             *
             * @return 追加数据请求
             */
            public ValueAppendRequest build() {
                return request;
            }
        }
    }

    /**
     * 写入图片请求
     */
    public static class ValueImageRequest {
        private String range;
        private byte[] image;
        private String name;

        public ValueImageRequest() {}

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
         * @param range 单元格范围，格式为 <sheetId>!<单元格位置>:<单元格位置>
         */
        public void setRange(String range) {
            this.range = range;
        }

        /**
         * 获取图片数据
         *
         * @return 图片二进制数据
         */
        public byte[] getImage() {
            return image;
        }

        /**
         * 设置图片数据
         *
         * @param image 图片二进制数据
         */
        public void setImage(byte[] image) {
            this.image = image;
        }

        /**
         * 获取图片名称
         *
         * @return 图片名称
         */
        public String getName() {
            return name;
        }

        /**
         * 设置图片名称
         *
         * @param name 图片名称，需要包含后缀名，如"test.png"
         */
        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 获取单个范围数据请求
     */
    public static class ValueGetRequest {
        private String range;
        private String valueRenderOption;
        private String dateTimeRenderOption;
        private String userIdType;

        public ValueGetRequest() {}

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
         * 获取单元格数据格式
         *
         * @return 单元格数据格式
         */
        public String getValueRenderOption() {
            return valueRenderOption;
        }

        /**
         * 设置单元格数据格式
         *
         * @param valueRenderOption 单元格数据格式
         */
        public void setValueRenderOption(String valueRenderOption) {
            this.valueRenderOption = valueRenderOption;
        }

        /**
         * 获取日期时间单元格数据格式
         *
         * @return 日期时间单元格数据格式
         */
        public String getDateTimeRenderOption() {
            return dateTimeRenderOption;
        }

        /**
         * 设置日期时间单元格数据格式
         *
         * @param dateTimeRenderOption 日期时间单元格数据格式
         */
        public void setDateTimeRenderOption(String dateTimeRenderOption) {
            this.dateTimeRenderOption = dateTimeRenderOption;
        }

        /**
         * 获取用户ID类型
         *
         * @return 用户ID类型
         */
        public String getUserIdType() {
            return userIdType;
        }

        /**
         * 设置用户ID类型
         *
         * @param userIdType 用户ID类型
         */
        public void setUserIdType(String userIdType) {
            this.userIdType = userIdType;
        }
    }

    /**
     * 批量获取多个范围数据请求
     */
    public static class ValueBatchGetRequest {
        private List<String> ranges;
        private String valueRenderOption;
        private String dateTimeRenderOption;
        private String userIdType;

        private String type;
        private String params;

        public ValueBatchGetRequest() {
            this.ranges = new ArrayList<>();
        }

        /**
         * 获取单元格范围列表
         *
         * @return 单元格范围列表
         */
        public List<String> getRanges() {
            return ranges;
        }

        /**
         * 设置单元格范围列表
         *
         * @param ranges 单元格范围列表
         */
        public void setRanges(List<String> ranges) {
            this.ranges = ranges;
        }

        /**
         * 添加单元格范围
         *
         * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public void addRange(String range) {
            if (this.ranges == null) {
                this.ranges = new ArrayList<>();
            }
            this.ranges.add(range);
        }

        /**
         * 获取单元格数据格式
         *
         * @return 单元格数据格式
         */
        public String getValueRenderOption() {
            return valueRenderOption;
        }

        /**
         * 设置单元格数据格式
         *
         * @param valueRenderOption 单元格数据格式
         */
        public void setValueRenderOption(String valueRenderOption) {
            this.valueRenderOption = valueRenderOption;
        }

        /**
         * 获取日期时间单元格数据格式
         *
         * @return 日期时间单元格数据格式
         */
        public String getDateTimeRenderOption() {
            return dateTimeRenderOption;
        }

        /**
         * 设置日期时间单元格数据格式
         *
         * @param dateTimeRenderOption 日期时间单元格数据格式
         */
        public void setDateTimeRenderOption(String dateTimeRenderOption) {
            this.dateTimeRenderOption = dateTimeRenderOption;
        }

        /**
         * 获取用户ID类型
         *
         * @return 用户ID类型
         */
        public String getUserIdType() {
            return userIdType;
        }

        /**
         * 设置用户ID类型
         *
         * @param userIdType 用户ID类型
         */
        public void setUserIdType(String userIdType) {
            this.userIdType = userIdType;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setParams(String params) {
            this.params = params;
        }

        public String getType() {
            return type;
        }

        public String getParams() {
            return params;
        }
    }

    /**
     * 向单个范围写入数据请求
     */
    public static class ValuePutRequest {
        private ValueRange valueRange;

        public ValuePutRequest() {}

        /**
         * 获取值范围
         *
         * @return 值范围
         */
        public ValueRange getValueRange() {
            return valueRange;
        }

        /**
         * 设置值范围
         *
         * @param valueRange 值范围
         */
        public void setValueRange(ValueRange valueRange) {
            this.valueRange = valueRange;
        }
    }

    /**
     * 向多个范围写入数据请求
     */
    public static class ValueBatchUpdatePutRequest {
        private List<ValueRangeItem> valueRanges;
        private String type;
        private String params;

        public ValueBatchUpdatePutRequest() {
            this.valueRanges = new ArrayList<>();
        }

        /**
         * 获取值范围列表
         *
         * @return 值范围列表
         */
        public List<ValueRangeItem> getValueRanges() {
            return valueRanges;
        }

        /**
         * 设置值范围列表
         *
         * @param valueRanges 值范围列表
         */
        public void setValueRanges(List<ValueRangeItem> valueRanges) {
            this.valueRanges = valueRanges;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setParams(String params) {
            this.params = params;
        }

        public String getType() {
            return type;
        }

        public String getParams() {
            return params;
        }
    }

    /**
     * 值范围项
     */
    public static class ValueRangeItem {
        private String range;
        private String type;
        private List<List<Object>> values;

        public ValueRangeItem() {
            this.values = new ArrayList<>();
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
         * 获取数据值
         *
         * @return 数据值
         */
        public List<List<Object>> getValues() {
            return values;
        }

        /**
         * 设置数据值
         *
         * @param values 数据值
         */
        public void setValues(List<List<Object>> values) {
            this.values = values;
        }


        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}