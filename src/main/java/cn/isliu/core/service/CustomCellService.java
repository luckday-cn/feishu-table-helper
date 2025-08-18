package cn.isliu.core.service;


import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义单元格服务 提供官方SDK未覆盖的单元格操作API
 */
public class CustomCellService extends AbstractFeishuApiService {

    /**
     * 构造函数
     *
     * @param feishuClient 飞书客户端
     */
    public CustomCellService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量操作单元格 支持合并单元格等操作 支持处理多个请求，如果有请求失败则中断后续请求
     *
     * @param spreadsheetToken 电子表格Token
     * @param request 批量操作请求
     * @return 批量操作响应
     * @throws IOException 请求异常
     */
    public ApiResponse cellsBatchUpdate(String spreadsheetToken, CellBatchUpdateRequest request)
        throws IOException {
        List<CellRequest> requests = request.getRequests();
        ApiResponse response = null;

        // 如果没有请求，返回空响应
        if (requests == null || requests.isEmpty()) {
            ApiResponse emptyResponse = new ApiResponse();
            emptyResponse.setCode(400);
            emptyResponse.setMsg("No cell operations found");
            return emptyResponse;
        }

        // 依次处理每个请求
        for (CellRequest cellRequest : requests) {
            // 处理合并单元格请求
            if (cellRequest.getMergeCells() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/merge_cells";

                String params;

                String type = cellRequest.getMergeCells().getType();
                if (type != null && !type.isEmpty() && "JSON_STR".equals(type)) {
                    params = cellRequest.getMergeCells().getParams();
                } else {
                    // 获取合并单元格范围
                    String range = cellRequest.getMergeCells().getRange();
                    if (range == null) {
                        ApiResponse errorResponse = new ApiResponse();
                        errorResponse.setCode(400);
                        errorResponse.setMsg("Invalid cell range");
                        return errorResponse;
                    }
                    params = gson.toJson(new MergeCellsRequestBody(range, cellRequest.getMergeCells().getMergeType()));
                }

                // 构建合并单元格请求体
                RequestBody body = RequestBody.create(params, JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理拆分单元格请求
            else if (cellRequest.getUnmergeCells() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/unmerge_cells";

                // 获取拆分单元格范围
                String range = cellRequest.getUnmergeCells().getRange();
                if (range == null) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("Invalid cell range");
                    return errorResponse;
                }

                // 构建拆分单元格请求体
                RequestBody body = RequestBody.create(gson.toJson(new UnmergeCellsRequestBody(range)), JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理设置单元格样式请求
            else if (cellRequest.getStyleCells() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/style";

                // 获取单元格范围
                String range = cellRequest.getStyleCells().getRange();
                if (range == null) {
                    ApiResponse errorResponse = new ApiResponse();
                    errorResponse.setCode(400);
                    errorResponse.setMsg("Invalid cell range");
                    return errorResponse;
                }

                // 构建设置样式请求体
                RequestBody body = RequestBody.create(
                    gson.toJson(new StyleCellsRequestBody(range, cellRequest.getStyleCells().getStyle())),
                    JSON_MEDIA_TYPE);

                Request httpRequest = createAuthenticatedRequest(url, "PUT", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 处理批量设置单元格样式请求
            else if (cellRequest.getStyleCellsBatch() != null) {
                String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/styles_batch_update";

                String type = cellRequest.getStyleCellsBatch().getType();
                String params = "";
                if (type != null && !type.isEmpty() && "JSON_STR".equals(type)) {
                    params = cellRequest.getStyleCellsBatch().getParams();
                } else {
                    // 获取单元格范围和样式
                    List<String> ranges = cellRequest.getStyleCellsBatch().getRanges();
                    Style style = cellRequest.getStyleCellsBatch().getStyle();

                    if (ranges == null || ranges.isEmpty()) {
                        ApiResponse errorResponse = new ApiResponse();
                        errorResponse.setCode(400);
                        errorResponse.setMsg("Invalid cell ranges");
                        return errorResponse;
                    }

                    // 构建批量设置样式请求体
                    StyleBatchUpdateRequest styleBatchRequest = new StyleBatchUpdateRequest();
                    StyleBatchData styleBatchData = new StyleBatchData();
                    styleBatchData.setRanges(ranges);
                    styleBatchData.setStyle(style);
                    styleBatchRequest.getData().add(styleBatchData);
                    params = gson.toJson(styleBatchRequest);
                }

                RequestBody body = RequestBody.create(params, JSON_MEDIA_TYPE);
                Request httpRequest = createAuthenticatedRequest(url, "PUT", body).build();
                response = executeRequest(httpRequest, ApiResponse.class);

                // 如果请求失败，中断后续请求
                if (!response.success()) {
                    return response;
                }
            }
            // 这里可以添加其他单元格操作类型
        }

        // 如果所有请求都成功处理，返回最后一个成功的响应
        // 如果没有处理任何请求(没有有效的操作类型)，返回错误响应
        if (response == null) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setCode(400);
            errorResponse.setMsg("No valid cell operation found");
            return errorResponse;
        }

        return response;
    }

    /**
     * 批量操作单元格请求
     */
    public static class CellBatchUpdateRequest {
        private List<CellRequest> requests;

        public CellBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<CellRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<CellRequest> requests) {
            this.requests = requests;
        }

        /**
         * 创建批量操作单元格请求的构建器
         *
         * @return 批量操作单元格请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量操作单元格请求的构建器
         */
        public static class Builder {
            private final CellBatchUpdateRequest request;

            public Builder() {
                request = new CellBatchUpdateRequest();
            }

            /**
             * 添加一个单元格操作请求
             *
             * @param cellRequest 单元格操作请求
             * @return 当前构建器
             */
            public Builder addRequest(CellRequest cellRequest) {
                request.requests.add(cellRequest);
                return this;
            }

            /**
             * 构建批量操作单元格请求
             *
             * @return 批量操作单元格请求
             */
            public CellBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 单元格操作请求
     */
    public static class CellRequest {
        private MergeCellsRequest mergeCells;
        private UnmergeCellsRequest unmergeCells;
        private StyleCellsRequest styleCells;
        private StyleCellsBatchRequest styleCellsBatch;

        /**
         * 获取合并单元格请求
         *
         * @return 合并单元格请求
         */
        public MergeCellsRequest getMergeCells() {
            return mergeCells;
        }

        /**
         * 设置合并单元格请求
         *
         * @param mergeCells 合并单元格请求
         */
        public void setMergeCells(MergeCellsRequest mergeCells) {
            this.mergeCells = mergeCells;
        }

        /**
         * 获取拆分单元格请求
         *
         * @return 拆分单元格请求
         */
        public UnmergeCellsRequest getUnmergeCells() {
            return unmergeCells;
        }

        /**
         * 设置拆分单元格请求
         *
         * @param unmergeCells 拆分单元格请求
         */
        public void setUnmergeCells(UnmergeCellsRequest unmergeCells) {
            this.unmergeCells = unmergeCells;
        }

        /**
         * 获取设置单元格样式请求
         *
         * @return 设置单元格样式请求
         */
        public StyleCellsRequest getStyleCells() {
            return styleCells;
        }

        /**
         * 设置单元格样式请求
         *
         * @param styleCells 设置单元格样式请求
         */
        public void setStyleCells(StyleCellsRequest styleCells) {
            this.styleCells = styleCells;
        }

        /**
         * 获取批量设置单元格样式请求
         *
         * @return 批量设置单元格样式请求
         */
        public StyleCellsBatchRequest getStyleCellsBatch() {
            return styleCellsBatch;
        }

        /**
         * 设置批量设置单元格样式请求
         *
         * @param styleCellsBatch 批量设置单元格样式请求
         */
        public void setStyleCellsBatch(StyleCellsBatchRequest styleCellsBatch) {
            this.styleCellsBatch = styleCellsBatch;
        }

        /**
         * 创建合并单元格的请求构建器 用于合并指定范围的单元格
         *
         * @return 合并单元格的构建器
         */
        public static MergeCellsBuilder mergeCells() {
            return new MergeCellsBuilder();
        }

        /**
         * 创建拆分单元格的请求构建器 用于拆分指定范围的单元格
         *
         * @return 拆分单元格的构建器
         */
        public static UnmergeCellsBuilder unmergeCells() {
            return new UnmergeCellsBuilder();
        }

        /**
         * 创建设置单元格样式的请求构建器 用于设置指定范围单元格的样式
         *
         * @return 设置单元格样式的构建器
         */
        public static StyleCellsBuilder styleCells() {
            return new StyleCellsBuilder();
        }

        /**
         * 创建批量设置单元格样式的请求构建器 用于一次设置多个区域单元格的样式
         *
         * @return 批量设置单元格样式的构建器
         */
        public static StyleCellsBatchBuilder styleCellsBatch() {
            return new StyleCellsBatchBuilder();
        }

        /**
         * 合并单元格的构建器 用于构建合并单元格的请求
         */
        public static class MergeCellsBuilder {
            private final CellRequest request;
            private final MergeCellsRequest mergeCells;

            public MergeCellsBuilder() {
                request = new CellRequest();
                mergeCells = new MergeCellsRequest();
                request.setMergeCells(mergeCells);
            }

            public MergeCellsBuilder setReqType(String reqType) {
                mergeCells.setType(reqType);
                return this;
            }

            public MergeCellsBuilder setReqParams(String reqParams) {
                mergeCells.setParams(reqParams);
                return this;
            }

            /**
             * 设置要合并的单元格所在的工作表ID
             *
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public MergeCellsBuilder sheetId(String sheetId) {
                mergeCells.sheetId = sheetId;
                return this;
            }

            /**
             * 设置要合并的单元格范围的开始位置
             *
             * @param startPosition 开始位置，如A1
             * @return 当前构建器
             */
            public MergeCellsBuilder startPosition(String startPosition) {
                mergeCells.startPosition = startPosition;
                return this;
            }

            /**
             * 设置要合并的单元格范围的结束位置
             *
             * @param endPosition 结束位置，如B2
             * @return 当前构建器
             */
            public MergeCellsBuilder endPosition(String endPosition) {
                mergeCells.endPosition = endPosition;
                return this;
            }

            /**
             * 设置合并方式
             *
             * @param mergeType 合并方式，可选值：MERGE_ALL（合并所有单元格）、MERGE_ROWS（按行合并）、MERGE_COLUMNS（按列合并）
             * @return 当前构建器
             */
            public MergeCellsBuilder mergeType(String mergeType) {
                mergeCells.mergeType = mergeType;
                return this;
            }

            /**
             * 构建合并单元格请求
             *
             * @return 单元格操作请求
             */
            public CellRequest build() {
                return request;
            }
        }

        /**
         * 拆分单元格的构建器 用于构建拆分单元格的请求
         */
        public static class UnmergeCellsBuilder {
            private final CellRequest request;
            private final UnmergeCellsRequest unmergeCells;

            public UnmergeCellsBuilder() {
                request = new CellRequest();
                unmergeCells = new UnmergeCellsRequest();
                request.setUnmergeCells(unmergeCells);
            }

            /**
             * 设置要拆分的单元格所在的工作表ID
             *
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public UnmergeCellsBuilder sheetId(String sheetId) {
                unmergeCells.sheetId = sheetId;
                return this;
            }

            /**
             * 设置要拆分的单元格范围的开始位置
             *
             * @param startPosition 开始位置，如A1
             * @return 当前构建器
             */
            public UnmergeCellsBuilder startPosition(String startPosition) {
                unmergeCells.startPosition = startPosition;
                return this;
            }

            /**
             * 设置要拆分的单元格范围的结束位置
             *
             * @param endPosition 结束位置，如B2
             * @return 当前构建器
             */
            public UnmergeCellsBuilder endPosition(String endPosition) {
                unmergeCells.endPosition = endPosition;
                return this;
            }

            /**
             * 构建拆分单元格请求
             *
             * @return 单元格操作请求
             */
            public CellRequest build() {
                return request;
            }
        }
    }

    /**
     * 合并单元格请求
     */
    public static class MergeCellsRequest {
        /**
         * 工作表ID
         */
        private String sheetId;

        /**
         * 单元格范围的开始位置
         */
        private String startPosition;

        /**
         * 单元格范围的结束位置
         */
        private String endPosition;

        /**
         * 兼容旧版接口的完整range
         */
        private String legacyRange;

        /**
         * 合并方式 可选值： MERGE_ALL：合并所有单元格 MERGE_ROWS：按行合并 MERGE_COLUMNS：按列合并
         */
        private String mergeType;

        private String type;
        private String params;

        /**
         * 获取要合并的单元格范围
         *
         * @return 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public String getRange() {
            if (legacyRange != null && !legacyRange.isEmpty()) {
                return legacyRange;
            }

            if (sheetId != null && startPosition != null && endPosition != null) {
                return sheetId + "!" + startPosition + ":" + endPosition;
            }

            return null;
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
         * 获取单元格范围的开始位置
         *
         * @return 开始位置
         */
        public String getStartPosition() {
            return startPosition;
        }

        /**
         * 设置单元格范围的开始位置
         *
         * @param startPosition 开始位置，如A1
         */
        public void setStartPosition(String startPosition) {
            this.startPosition = startPosition;
        }

        /**
         * 获取单元格范围的结束位置
         *
         * @return 结束位置
         */
        public String getEndPosition() {
            return endPosition;
        }

        /**
         * 设置单元格范围的结束位置
         *
         * @param endPosition 结束位置，如B2
         */
        public void setEndPosition(String endPosition) {
            this.endPosition = endPosition;
        }

        /**
         * 获取合并方式
         *
         * @return 合并方式
         */
        public String getMergeType() {
            return mergeType;
        }

        /**
         * 设置合并方式
         *
         * @param mergeType 合并方式，可选值：MERGE_ALL、MERGE_ROWS、MERGE_COLUMNS
         */
        public void setMergeType(String mergeType) {
            this.mergeType = mergeType;
        }


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }
    }

    /**
     * 拆分单元格请求
     */
    public static class UnmergeCellsRequest {
        /**
         * 工作表ID
         */
        private String sheetId;

        /**
         * 单元格范围的开始位置
         */
        private String startPosition;

        /**
         * 单元格范围的结束位置
         */
        private String endPosition;

        /**
         * 兼容旧版接口的完整range
         */
        private String legacyRange;

        /**
         * 获取要拆分的单元格范围
         *
         * @return 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public String getRange() {
            if (legacyRange != null && !legacyRange.isEmpty()) {
                return legacyRange;
            }

            if (sheetId != null && startPosition != null && endPosition != null) {
                return sheetId + "!" + startPosition + ":" + endPosition;
            }

            return null;
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
         * 获取单元格范围的开始位置
         *
         * @return 开始位置
         */
        public String getStartPosition() {
            return startPosition;
        }

        /**
         * 设置单元格范围的开始位置
         *
         * @param startPosition 开始位置，如A1
         */
        public void setStartPosition(String startPosition) {
            this.startPosition = startPosition;
        }

        /**
         * 获取单元格范围的结束位置
         *
         * @return 结束位置
         */
        public String getEndPosition() {
            return endPosition;
        }

        /**
         * 设置单元格范围的结束位置
         *
         * @param endPosition 结束位置，如B2
         */
        public void setEndPosition(String endPosition) {
            this.endPosition = endPosition;
        }
    }

    /**
     * 设置单元格样式请求
     */
    public static class StyleCellsRequest {
        /**
         * 工作表ID
         */
        private String sheetId;

        /**
         * 单元格范围的开始位置
         */
        private String startPosition;

        /**
         * 单元格范围的结束位置
         */
        private String endPosition;

        /**
         * 兼容旧版接口的完整range
         */
        private String legacyRange;

        /**
         * 单元格样式
         */
        private Style style;

        /**
         * 获取要设置样式的单元格范围
         *
         * @return 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public String getRange() {
            if (legacyRange != null && !legacyRange.isEmpty()) {
                return legacyRange;
            }

            if (sheetId != null && startPosition != null && endPosition != null) {
                return sheetId + "!" + startPosition + ":" + endPosition;
            }

            return null;
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
         * 获取单元格范围的开始位置
         *
         * @return 开始位置
         */
        public String getStartPosition() {
            return startPosition;
        }

        /**
         * 设置单元格范围的开始位置
         *
         * @param startPosition 开始位置，如A1
         */
        public void setStartPosition(String startPosition) {
            this.startPosition = startPosition;
        }

        /**
         * 获取单元格范围的结束位置
         *
         * @return 结束位置
         */
        public String getEndPosition() {
            return endPosition;
        }

        /**
         * 设置单元格范围的结束位置
         *
         * @param endPosition 结束位置，如B2
         */
        public void setEndPosition(String endPosition) {
            this.endPosition = endPosition;
        }

        /**
         * 设置兼容旧版接口的完整range
         *
         * @param legacyRange 完整range
         */
        public void setLegacyRange(String legacyRange) {
            this.legacyRange = legacyRange;
        }

        /**
         * 获取单元格样式
         *
         * @return 单元格样式
         */
        public Style getStyle() {
            return style;
        }

        /**
         * 设置单元格样式
         *
         * @param style 单元格样式
         */
        public void setStyle(Style style) {
            this.style = style;
        }
    }

    /**
     * 单元格样式
     */
    public static class Style {
        private Font font;
        private Integer textDecoration;
        private String formatter;
        private Integer hAlign;
        private Integer vAlign;
        private String foreColor;
        private String backColor;
        private String borderType;
        private String borderColor;
        private Boolean clean;

        /**
         * 获取字体样式
         *
         * @return 字体样式
         */
        public Font getFont() {
            return font;
        }

        /**
         * 设置字体样式
         *
         * @param font 字体样式
         */
        public void setFont(Font font) {
            this.font = font;
        }

        /**
         * 获取文本装饰
         *
         * @return 文本装饰，0：默认样式，1：下划线，2：删除线，3：下划线和删除线
         */
        public Integer getTextDecoration() {
            return textDecoration;
        }

        /**
         * 设置文本装饰
         *
         * @param textDecoration 文本装饰，0：默认样式，1：下划线，2：删除线，3：下划线和删除线
         */
        public void setTextDecoration(Integer textDecoration) {
            this.textDecoration = textDecoration;
        }

        /**
         * 获取数字格式
         *
         * @return 数字格式
         */
        public String getFormatter() {
            return formatter;
        }

        /**
         * 设置数字格式
         *
         * @param formatter 数字格式
         */
        public void setFormatter(String formatter) {
            this.formatter = formatter;
        }

        /**
         * 获取水平对齐方式
         *
         * @return 水平对齐方式，0：左对齐，1：中对齐，2：右对齐
         */
        public Integer getHAlign() {
            return hAlign;
        }

        /**
         * 设置水平对齐方式
         *
         * @param hAlign 水平对齐方式，0：左对齐，1：中对齐，2：右对齐
         */
        public void setHAlign(Integer hAlign) {
            this.hAlign = hAlign;
        }

        /**
         * 获取垂直对齐方式
         *
         * @return 垂直对齐方式，0：上对齐，1：中对齐，2：下对齐
         */
        public Integer getVAlign() {
            return vAlign;
        }

        /**
         * 设置垂直对齐方式
         *
         * @param vAlign 垂直对齐方式，0：上对齐，1：中对齐，2：下对齐
         */
        public void setVAlign(Integer vAlign) {
            this.vAlign = vAlign;
        }

        /**
         * 获取字体颜色
         *
         * @return 字体颜色，十六进制颜色代码
         */
        public String getForeColor() {
            return foreColor;
        }

        /**
         * 设置字体颜色
         *
         * @param foreColor 字体颜色，十六进制颜色代码
         */
        public void setForeColor(String foreColor) {
            this.foreColor = foreColor;
        }

        /**
         * 获取背景颜色
         *
         * @return 背景颜色，十六进制颜色代码
         */
        public String getBackColor() {
            return backColor;
        }

        /**
         * 设置背景颜色
         *
         * @param backColor 背景颜色，十六进制颜色代码
         */
        public void setBackColor(String backColor) {
            this.backColor = backColor;
        }

        /**
         * 获取边框类型
         *
         * @return 边框类型
         */
        public String getBorderType() {
            return borderType;
        }

        /**
         * 设置边框类型
         *
         * @param borderType
         *            边框类型，可选值：FULL_BORDER、OUTER_BORDER、INNER_BORDER、NO_BORDER、LEFT_BORDER、RIGHT_BORDER、TOP_BORDER、BOTTOM_BORDER
         */
        public void setBorderType(String borderType) {
            this.borderType = borderType;
        }

        /**
         * 获取边框颜色
         *
         * @return 边框颜色，十六进制颜色代码
         */
        public String getBorderColor() {
            return borderColor;
        }

        /**
         * 设置边框颜色
         *
         * @param borderColor 边框颜色，十六进制颜色代码
         */
        public void setBorderColor(String borderColor) {
            this.borderColor = borderColor;
        }

        /**
         * 获取是否清除所有格式
         *
         * @return 是否清除所有格式
         */
        public Boolean getClean() {
            return clean;
        }

        /**
         * 设置是否清除所有格式
         *
         * @param clean 是否清除所有格式
         */
        public void setClean(Boolean clean) {
            this.clean = clean;
        }
    }

    /**
     * 字体样式
     */
    public static class Font {
        private Boolean bold;
        private Boolean italic;
        private String fontSize;
        private Boolean clean;

        /**
         * 获取是否加粗
         *
         * @return 是否加粗
         */
        public Boolean getBold() {
            return bold;
        }

        /**
         * 设置是否加粗
         *
         * @param bold 是否加粗
         */
        public void setBold(Boolean bold) {
            this.bold = bold;
        }

        /**
         * 获取是否斜体
         *
         * @return 是否斜体
         */
        public Boolean getItalic() {
            return italic;
        }

        /**
         * 设置是否斜体
         *
         * @param italic 是否斜体
         */
        public void setItalic(Boolean italic) {
            this.italic = italic;
        }

        /**
         * 获取字体大小
         *
         * @return 字体大小，如10pt/1.5
         */
        public String getFontSize() {
            return fontSize;
        }

        /**
         * 设置字体大小
         *
         * @param fontSize 字体大小，如10pt/1.5
         */
        public void setFontSize(String fontSize) {
            this.fontSize = fontSize;
        }

        /**
         * 获取是否清除字体格式
         *
         * @return 是否清除字体格式
         */
        public Boolean getClean() {
            return clean;
        }

        /**
         * 设置是否清除字体格式
         *
         * @param clean 是否清除字体格式
         */
        public void setClean(Boolean clean) {
            this.clean = clean;
        }
    }

    /**
     * 合并单元格请求体（用于API请求）
     */
    private static class MergeCellsRequestBody {
        private final String range;
        private final String mergeType;

        /**
         * 构造函数
         *
         * @param range 单元格范围
         * @param mergeType 合并方式
         */
        public MergeCellsRequestBody(String range, String mergeType) {
            this.range = range;
            this.mergeType = mergeType;
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
         * 获取合并方式
         *
         * @return 合并方式
         */
        public String getMergeType() {
            return mergeType;
        }
    }

    /**
     * 拆分单元格请求体（用于API请求）
     */
    private static class UnmergeCellsRequestBody {
        private final String range;

        /**
         * 构造函数
         *
         * @param range 单元格范围
         */
        public UnmergeCellsRequestBody(String range) {
            this.range = range;
        }

        /**
         * 获取单元格范围
         *
         * @return 单元格范围
         */
        public String getRange() {
            return range;
        }
    }

    /**
     * 设置单元格样式请求体（用于API请求）
     */
    private static class StyleCellsRequestBody {
        private final AppendStyle appendStyle;

        /**
         * 构造函数
         *
         * @param range 单元格范围
         * @param style 单元格样式
         */
        public StyleCellsRequestBody(String range, Style style) {
            this.appendStyle = new AppendStyle(range, style);
        }

        /**
         * 获取appendStyle
         *
         * @return appendStyle
         */
        public AppendStyle getAppendStyle() {
            return appendStyle;
        }

        /**
         * 设置单元格样式请求的appendStyle部分
         */
        private static class AppendStyle {
            private final String range;
            private final Style style;

            /**
             * 构造函数
             *
             * @param range 单元格范围
             * @param style 单元格样式
             */
            public AppendStyle(String range, Style style) {
                this.range = range;
                this.style = style;
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
             * 获取单元格样式
             *
             * @return 单元格样式
             */
            public Style getStyle() {
                return style;
            }
        }
    }

    /**
     * 设置单元格样式的构建器 用于构建设置单元格样式的请求
     */
    public static class StyleCellsBuilder {
        private final CellRequest request;
        private final StyleCellsRequest styleCells;
        private final Style style;
        private Font font;

        public StyleCellsBuilder() {
            request = new CellRequest();
            styleCells = new StyleCellsRequest();
            style = new Style();
            styleCells.setStyle(style);
            request.setStyleCells(styleCells);
        }

        /**
         * 设置要设置样式的单元格所在的工作表ID
         *
         * @param sheetId 工作表ID
         * @return 当前构建器
         */
        public StyleCellsBuilder sheetId(String sheetId) {
            styleCells.setSheetId(sheetId);
            return this;
        }

        /**
         * 设置要设置样式的单元格范围的开始位置
         *
         * @param startPosition 开始位置，如A1
         * @return 当前构建器
         */
        public StyleCellsBuilder startPosition(String startPosition) {
            styleCells.setStartPosition(startPosition);
            return this;
        }

        /**
         * 设置要设置样式的单元格范围的结束位置
         *
         * @param endPosition 结束位置，如B2
         * @return 当前构建器
         */
        public StyleCellsBuilder endPosition(String endPosition) {
            styleCells.setEndPosition(endPosition);
            return this;
        }

        /**
         * 设置是否加粗
         *
         * @param bold 是否加粗
         * @return 当前构建器
         */
        public StyleCellsBuilder bold(Boolean bold) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setBold(bold);
            return this;
        }

        /**
         * 设置是否斜体
         *
         * @param italic 是否斜体
         * @return 当前构建器
         */
        public StyleCellsBuilder italic(Boolean italic) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setItalic(italic);
            return this;
        }

        /**
         * 设置字体大小
         *
         * @param fontSize 字体大小，如10pt/1.5
         * @return 当前构建器
         */
        public StyleCellsBuilder fontSize(String fontSize) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setFontSize(fontSize);
            return this;
        }

        /**
         * 设置是否清除字体格式
         *
         * @param clean 是否清除字体格式
         * @return 当前构建器
         */
        public StyleCellsBuilder fontClean(Boolean clean) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setClean(clean);
            return this;
        }

        /**
         * 设置文本装饰
         *
         * @param textDecoration 文本装饰，0：默认样式，1：下划线，2：删除线，3：下划线和删除线
         * @return 当前构建器
         */
        public StyleCellsBuilder textDecoration(Integer textDecoration) {
            style.setTextDecoration(textDecoration);
            return this;
        }

        /**
         * 设置数字格式
         *
         * @param formatter 数字格式
         * @return 当前构建器
         */
        public StyleCellsBuilder formatter(String formatter) {
            style.setFormatter(formatter);
            return this;
        }

        /**
         * 设置水平对齐方式
         *
         * @param hAlign 水平对齐方式，0：左对齐，1：中对齐，2：右对齐
         * @return 当前构建器
         */
        public StyleCellsBuilder hAlign(Integer hAlign) {
            style.setHAlign(hAlign);
            return this;
        }

        /**
         * 设置垂直对齐方式
         *
         * @param vAlign 垂直对齐方式，0：上对齐，1：中对齐，2：下对齐
         * @return 当前构建器
         */
        public StyleCellsBuilder vAlign(Integer vAlign) {
            style.setVAlign(vAlign);
            return this;
        }

        /**
         * 设置字体颜色
         *
         * @param foreColor 字体颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBuilder foreColor(String foreColor) {
            style.setForeColor(foreColor);
            return this;
        }

        /**
         * 设置背景颜色
         *
         * @param backColor 背景颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBuilder backColor(String backColor) {
            style.setBackColor(backColor);
            return this;
        }

        /**
         * 设置边框类型
         *
         * @param borderType
         *            边框类型，可选值：FULL_BORDER、OUTER_BORDER、INNER_BORDER、NO_BORDER、LEFT_BORDER、RIGHT_BORDER、TOP_BORDER、BOTTOM_BORDER
         * @return 当前构建器
         */
        public StyleCellsBuilder borderType(String borderType) {
            style.setBorderType(borderType);
            return this;
        }

        /**
         * 设置边框颜色
         *
         * @param borderColor 边框颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBuilder borderColor(String borderColor) {
            style.setBorderColor(borderColor);
            return this;
        }

        /**
         * 设置是否清除所有格式
         *
         * @param clean 是否清除所有格式
         * @return 当前构建器
         */
        public StyleCellsBuilder clean(Boolean clean) {
            style.setClean(clean);
            return this;
        }

        /**
         * 构建设置单元格样式请求
         *
         * @return 单元格操作请求
         */
        public CellRequest build() {
            return request;
        }
    }

    /**
     * 批量设置单元格样式请求
     */
    public static class StyleBatchUpdateRequest {
        private List<StyleBatchData> data;

        public StyleBatchUpdateRequest() {
            this.data = new ArrayList<>();
        }

        public List<StyleBatchData> getData() {
            return data;
        }

        public void setData(List<StyleBatchData> data) {
            this.data = data;
        }

        /**
         * 创建批量设置单元格样式请求的构建器
         *
         * @return 批量设置单元格样式请求的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量设置单元格样式请求的构建器
         */
        public static class Builder {
            private final StyleBatchUpdateRequest request;

            public Builder() {
                request = new StyleBatchUpdateRequest();
            }

            /**
             * 添加一组样式设置
             *
             * @param styleBatchData 样式设置数据
             * @return 当前构建器
             */
            public Builder addStyleBatch(StyleBatchData styleBatchData) {
                request.data.add(styleBatchData);
                return this;
            }

            /**
             * 构建批量设置单元格样式请求
             *
             * @return 批量设置单元格样式请求
             */
            public StyleBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 批量设置单元格样式数据
     */
    public static class StyleBatchData {
        private List<String> ranges;
        private Style style;

        public StyleBatchData() {
            this.ranges = new ArrayList<>();
        }

        public List<String> getRanges() {
            return ranges;
        }

        public void setRanges(List<String> ranges) {
            this.ranges = ranges;
        }

        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
        }

        /**
         * 创建批量设置单元格样式数据的构建器
         *
         * @return 批量设置单元格样式数据的构建器
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * 批量设置单元格样式数据的构建器
         */
        public static class Builder {
            private final StyleBatchData data;
            private final Style style;
            private Font font;

            public Builder() {
                data = new StyleBatchData();
                style = new Style();
                data.setStyle(style);
            }

            /**
             * 添加要设置样式的单元格范围
             *
             * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
             * @return 当前构建器
             */
            public Builder addRange(String range) {
                data.ranges.add(range);
                return this;
            }

            /**
             * 设置是否加粗
             *
             * @param bold 是否加粗
             * @return 当前构建器
             */
            public Builder bold(Boolean bold) {
                if (font == null) {
                    font = new Font();
                    style.setFont(font);
                }
                font.setBold(bold);
                return this;
            }

            /**
             * 设置是否斜体
             *
             * @param italic 是否斜体
             * @return 当前构建器
             */
            public Builder italic(Boolean italic) {
                if (font == null) {
                    font = new Font();
                    style.setFont(font);
                }
                font.setItalic(italic);
                return this;
            }

            /**
             * 设置字体大小
             *
             * @param fontSize 字体大小，如10pt/1.5
             * @return 当前构建器
             */
            public Builder fontSize(String fontSize) {
                if (font == null) {
                    font = new Font();
                    style.setFont(font);
                }
                font.setFontSize(fontSize);
                return this;
            }

            /**
             * 设置是否清除字体格式
             *
             * @param clean 是否清除字体格式
             * @return 当前构建器
             */
            public Builder fontClean(Boolean clean) {
                if (font == null) {
                    font = new Font();
                    style.setFont(font);
                }
                font.setClean(clean);
                return this;
            }

            /**
             * 设置文本装饰
             *
             * @param textDecoration 文本装饰，0：默认样式，1：下划线，2：删除线，3：下划线和删除线
             * @return 当前构建器
             */
            public Builder textDecoration(Integer textDecoration) {
                style.setTextDecoration(textDecoration);
                return this;
            }

            /**
             * 设置数字格式
             *
             * @param formatter 数字格式
             * @return 当前构建器
             */
            public Builder formatter(String formatter) {
                style.setFormatter(formatter);
                return this;
            }

            /**
             * 设置水平对齐方式
             *
             * @param hAlign 水平对齐方式，0：左对齐，1：中对齐，2：右对齐
             * @return 当前构建器
             */
            public Builder hAlign(Integer hAlign) {
                style.setHAlign(hAlign);
                return this;
            }

            /**
             * 设置垂直对齐方式
             *
             * @param vAlign 垂直对齐方式，0：上对齐，1：中对齐，2：下对齐
             * @return 当前构建器
             */
            public Builder vAlign(Integer vAlign) {
                style.setVAlign(vAlign);
                return this;
            }

            /**
             * 设置字体颜色
             *
             * @param foreColor 字体颜色，十六进制颜色代码
             * @return 当前构建器
             */
            public Builder foreColor(String foreColor) {
                style.setForeColor(foreColor);
                return this;
            }

            /**
             * 设置背景颜色
             *
             * @param backColor 背景颜色，十六进制颜色代码
             * @return 当前构建器
             */
            public Builder backColor(String backColor) {
                style.setBackColor(backColor);
                return this;
            }

            /**
             * 设置边框类型
             *
             * @param borderType
             *            边框类型，可选值：FULL_BORDER、OUTER_BORDER、INNER_BORDER、NO_BORDER、LEFT_BORDER、RIGHT_BORDER、TOP_BORDER、BOTTOM_BORDER
             * @return 当前构建器
             */
            public Builder borderType(String borderType) {
                style.setBorderType(borderType);
                return this;
            }

            /**
             * 设置边框颜色
             *
             * @param borderColor 边框颜色，十六进制颜色代码
             * @return 当前构建器
             */
            public Builder borderColor(String borderColor) {
                style.setBorderColor(borderColor);
                return this;
            }

            /**
             * 设置是否清除所有格式
             *
             * @param clean 是否清除所有格式
             * @return 当前构建器
             */
            public Builder clean(Boolean clean) {
                style.setClean(clean);
                return this;
            }

            /**
             * 构建批量设置单元格样式数据
             *
             * @return 批量设置单元格样式数据
             */
            public StyleBatchData build() {
                return data;
            }
        }
    }

    /**
     * 批量设置单元格样式请求
     */
    public static class StyleCellsBatchRequest {
        @Deprecated
        private List<String> ranges;
        private List<CellRange> cellRanges;
        private Style style;
        private String type;
        private String params;

        public StyleCellsBatchRequest() {
            this.ranges = new ArrayList<>();
            this.cellRanges = new ArrayList<>();
        }

        /**
         * 获取单元格范围列表（用于API请求）
         *
         * @return 单元格范围列表
         */
        public List<String> getRanges() {
            // 如果有新的结构化范围，优先使用它们
            if (cellRanges != null && !cellRanges.isEmpty()) {
                List<String> result = new ArrayList<>();
                for (CellRange cellRange : cellRanges) {
                    result.add(cellRange.getRange());
                }
                return result;
            }
            return ranges;
        }

        /**
         * 获取单元格结构化范围列表
         *
         * @return 单元格结构化范围列表
         */
        public List<CellRange> getCellRanges() {
            return cellRanges;
        }

        /**
         * 设置单元格结构化范围列表
         *
         * @param cellRanges 单元格结构化范围列表
         */
        public void setCellRanges(List<CellRange> cellRanges) {
            this.cellRanges = cellRanges;
        }

        /**
         * 添加单元格结构化范围
         *
         * @param cellRange 单元格结构化范围
         */
        public void addCellRange(CellRange cellRange) {
            if (this.cellRanges == null) {
                this.cellRanges = new ArrayList<>();
            }
            this.cellRanges.add(cellRange);
        }

        /**
         * 添加单元格结构化范围
         *
         * @param sheetId 工作表ID
         * @param startPosition 开始位置
         * @param endPosition 结束位置
         */
        public void addCellRange(String sheetId, String startPosition, String endPosition) {
            addCellRange(new CellRange(sheetId, startPosition, endPosition));
        }

        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getParams() {
            return params;
        }
        public void setParams(String params) {
            this.params = params;
        }
    }

    /**
     * 单元格范围
     */
    public static class CellRange {
        private String sheetId;
        private String startPosition;
        private String endPosition;

        /**
         * 默认构造函数
         */
        public CellRange() {}

        /**
         * 构造函数
         *
         * @param sheetId 工作表ID
         * @param startPosition 开始位置
         * @param endPosition 结束位置
         */
        public CellRange(String sheetId, String startPosition, String endPosition) {
            this.sheetId = sheetId;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        /**
         * 获取单元格范围
         *
         * @return 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         */
        public String getRange() {
            if (sheetId != null && startPosition != null && endPosition != null) {
                return sheetId + "!" + startPosition + ":" + endPosition;
            }
            return null;
        }

        /**
         * 从范围字符串解析
         *
         * @param range 单元格范围，格式为 <sheetId>!<开始位置>:<结束位置>
         * @return 单元格范围对象
         */
        public static CellRange fromRange(String range) {
            CellRange cellRange = new CellRange();
            try {
                String[] parts = range.split("!");
                cellRange.sheetId = parts[0];
                String[] positions = parts[1].split(":");
                cellRange.startPosition = positions[0];
                cellRange.endPosition = positions[1];
            } catch (Exception e) {
                // 解析失败，返回空对象
            }
            return cellRange;
        }

        public String getSheetId() {
            return sheetId;
        }

        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }

        public String getStartPosition() {
            return startPosition;
        }

        public void setStartPosition(String startPosition) {
            this.startPosition = startPosition;
        }

        public String getEndPosition() {
            return endPosition;
        }

        public void setEndPosition(String endPosition) {
            this.endPosition = endPosition;
        }
    }

    /**
     * 批量设置单元格样式的构建器 用于构建批量设置单元格样式的请求
     */
    public static class StyleCellsBatchBuilder {
        private final CellRequest request;
        private final StyleCellsBatchRequest styleCellsBatch;
        private final Style style;
        private Font font;

        public StyleCellsBatchBuilder() {
            request = new CellRequest();
            styleCellsBatch = new StyleCellsBatchRequest();
            style = new Style();
            styleCellsBatch.setStyle(style);
            request.setStyleCellsBatch(styleCellsBatch);
        }

        public StyleCellsBatchBuilder setReqType(String reqType) {
            styleCellsBatch.setType(reqType);
            return this;
        }

        public StyleCellsBatchBuilder setParams(String params) {
            styleCellsBatch.setParams(params);
            return this;
        }

        /**
         * 添加要设置样式的单元格范围
         *
         * @param sheetId 工作表ID
         * @param startPosition 开始位置，如A1
         * @param endPosition 结束位置，如B2
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder addRange(String sheetId, String startPosition, String endPosition) {
            styleCellsBatch.addCellRange(sheetId, startPosition, endPosition);
            return this;
        }

        /**
         * 设置是否加粗
         *
         * @param bold 是否加粗
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder bold(Boolean bold) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setBold(bold);
            return this;
        }

        /**
         * 设置是否斜体
         *
         * @param italic 是否斜体
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder italic(Boolean italic) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setItalic(italic);
            return this;
        }

        /**
         * 设置字体大小
         *
         * @param fontSize 字体大小，如10pt/1.5
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder fontSize(String fontSize) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setFontSize(fontSize);
            return this;
        }

        /**
         * 设置是否清除字体格式
         *
         * @param clean 是否清除字体格式
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder fontClean(Boolean clean) {
            if (font == null) {
                font = new Font();
                style.setFont(font);
            }
            font.setClean(clean);
            return this;
        }

        /**
         * 设置文本装饰
         *
         * @param textDecoration 文本装饰，0：默认样式，1：下划线，2：删除线，3：下划线和删除线
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder textDecoration(Integer textDecoration) {
            style.setTextDecoration(textDecoration);
            return this;
        }

        /**
         * 设置数字格式
         *
         * @param formatter 数字格式
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder formatter(String formatter) {
            style.setFormatter(formatter);
            return this;
        }

        /**
         * 设置水平对齐方式
         *
         * @param hAlign 水平对齐方式，0：左对齐，1：中对齐，2：右对齐
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder hAlign(Integer hAlign) {
            style.setHAlign(hAlign);
            return this;
        }

        /**
         * 设置垂直对齐方式
         *
         * @param vAlign 垂直对齐方式，0：上对齐，1：中对齐，2：下对齐
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder vAlign(Integer vAlign) {
            style.setVAlign(vAlign);
            return this;
        }

        /**
         * 设置字体颜色
         *
         * @param foreColor 字体颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder foreColor(String foreColor) {
            style.setForeColor(foreColor);
            return this;
        }

        /**
         * 设置背景颜色
         *
         * @param backColor 背景颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder backColor(String backColor) {
            style.setBackColor(backColor);
            return this;
        }

        /**
         * 设置边框类型
         *
         * @param borderType
         *            边框类型，可选值：FULL_BORDER、OUTER_BORDER、INNER_BORDER、NO_BORDER、LEFT_BORDER、RIGHT_BORDER、TOP_BORDER、BOTTOM_BORDER
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder borderType(String borderType) {
            style.setBorderType(borderType);
            return this;
        }

        /**
         * 设置边框颜色
         *
         * @param borderColor 边框颜色，十六进制颜色代码
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder borderColor(String borderColor) {
            style.setBorderColor(borderColor);
            return this;
        }

        /**
         * 设置是否清除所有格式
         *
         * @param clean 是否清除所有格式
         * @return 当前构建器
         */
        public StyleCellsBatchBuilder clean(Boolean clean) {
            style.setClean(clean);
            return this;
        }

        /**
         * 构建批量设置单元格样式请求
         *
         * @return 单元格操作请求
         */
        public CellRequest build() {
            return request;
        }
    }
}