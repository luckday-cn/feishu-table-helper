package cn.isliu.core.service;

import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.pojo.ApiResponse;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义表格服务 提供官方SDK未覆盖的表格API
 */
public class CustomSheetService extends AbstractFeishuApiService {

    /**
     * 构造函数
     * 
     * @param feishuClient 飞书客户端
     */
    public CustomSheetService(FeishuClient feishuClient) {
        super(feishuClient);
    }

    /**
     * 批量更新工作表 支持添加、复制、删除等操作
     * 
     * @param spreadsheetToken 电子表格Token
     * @param request 批量更新请求
     * @return 批量更新响应
     * @throws IOException 请求异常
     */
    public ApiResponse sheetsBatchUpdate(String spreadsheetToken, SheetBatchUpdateRequest request)
        throws IOException {
        List<SheetRequest> requests = request.getRequests();
        // 更新工作表特殊字段兼容
        String userIdType = null;
        for (SheetRequest sheetRequest : requests) {
            UpdateSheetRequest updateSheet = sheetRequest.getUpdateSheet();
            if (updateSheet != null) {
                SheetPropertiesUpdate properties = updateSheet.getProperties();
                userIdType = properties.getUserIdType();
                break;
            }
        }
        String url = BASE_URL + "/sheets/v2/spreadsheets/" + spreadsheetToken + "/sheets_batch_update";
        if (userIdType != null && !userIdType.isEmpty()) {
            url += "?user_id_type=" + userIdType;
        }

        RequestBody body = RequestBody.create(gson.toJson(request), JSON_MEDIA_TYPE);

        Request httpRequest = createAuthenticatedRequest(url, "POST", body).build();
        return executeRequest(httpRequest, ApiResponse.class);
    }

    /**
     * 批量更新工作表请求
     */
    public static class SheetBatchUpdateRequest {
        /**
         * 支持增加、复制、删除和更新工作表 一次请求可以同时进行多个操作
         */
        private List<SheetRequest> requests;

        public SheetBatchUpdateRequest() {
            this.requests = new ArrayList<>();
        }

        public List<SheetRequest> getRequests() {
            return requests;
        }

        public void setRequests(List<SheetRequest> requests) {
            this.requests = requests;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder {
            private final SheetBatchUpdateRequest request;

            public Builder() {
                request = new SheetBatchUpdateRequest();
            }

            public Builder addRequest(SheetRequest sheetRequest) {
                request.requests.add(sheetRequest);
                return this;
            }

            public SheetBatchUpdateRequest build() {
                return request;
            }
        }
    }

    /**
     * 工作表请求
     */
    public static class SheetRequest {
        /**
         * 增加工作表操作
         */
        private AddSheetRequest addSheet;

        /**
         * 复制工作表操作
         */
        private CopySheetRequest copySheet;

        /**
         * 删除工作表操作
         */
        private DeleteSheetRequest deleteSheet;

        /**
         * 更新工作表操作
         */
        private UpdateSheetRequest updateSheet;

        public AddSheetRequest getAddSheet() {
            return addSheet;
        }

        public void setAddSheet(AddSheetRequest addSheet) {
            this.addSheet = addSheet;
        }

        public CopySheetRequest getCopySheet() {
            return copySheet;
        }

        public void setCopySheet(CopySheetRequest copySheet) {
            this.copySheet = copySheet;
        }

        public DeleteSheetRequest getDeleteSheet() {
            return deleteSheet;
        }

        public void setDeleteSheet(DeleteSheetRequest deleteSheet) {
            this.deleteSheet = deleteSheet;
        }

        public UpdateSheetRequest getUpdateSheet() {
            return updateSheet;
        }

        public void setUpdateSheet(UpdateSheetRequest updateSheet) {
            this.updateSheet = updateSheet;
        }

        /**
         * 创建添加工作表的请求构建器 用于在电子表格中添加新的工作表
         * 
         * @return 添加工作表的构建器
         */
        public static AddSheetBuilder addSheet() {
            return new AddSheetBuilder();
        }

        /**
         * 创建复制工作表的请求构建器 用于复制已有的工作表到同一电子表格中
         * 
         * @return 复制工作表的构建器
         */
        public static CopySheetBuilder copySheet() {
            return new CopySheetBuilder();
        }

        /**
         * 创建删除工作表的请求构建器 用于删除电子表格中的指定工作表
         * 
         * @return 删除工作表的构建器
         */
        public static DeleteSheetBuilder deleteSheet() {
            return new DeleteSheetBuilder();
        }

        /**
         * 创建更新工作表的请求构建器 用于更新工作表的标题、位置、显示状态、冻结行列、保护设置等属性
         * 
         * @return 更新工作表的构建器
         */
        public static UpdateSheetBuilder updateSheet() {
            return new UpdateSheetBuilder();
        }

        /**
         * 添加工作表的构建器 用于构建添加新工作表的请求
         */
        public static class AddSheetBuilder {
            private final SheetRequest request;
            private final AddSheetRequest addSheet;

            public AddSheetBuilder() {
                request = new SheetRequest();
                addSheet = new AddSheetRequest();
                request.setAddSheet(addSheet);
            }

            /**
             * 获取或初始化properties对象
             * 
             * @return properties对象
             */
            private SheetProperties getProperties() {
                if (addSheet.properties == null) {
                    addSheet.properties = new SheetProperties();
                }
                return addSheet.properties;
            }

            /**
             * 设置工作表标题
             * 
             * @param title 工作表标题，不能包含特殊字符：/ \ ? * [ ] :
             * @return 当前构建器
             */
            public AddSheetBuilder title(String title) {
                getProperties().title = title;
                return this;
            }

            /**
             * 设置工作表在电子表格中的位置索引
             * 
             * @param index 位置索引，从0开始计数，不填默认在第0位置添加
             * @return 当前构建器
             */
            public AddSheetBuilder index(Integer index) {
                getProperties().index = index;
                return this;
            }

            /**
             * 构建添加工作表请求
             * 
             * @return 工作表操作请求
             */
            public SheetRequest build() {
                return request;
            }
        }

        /**
         * 复制工作表的构建器 用于构建复制现有工作表的请求
         */
        public static class CopySheetBuilder {
            private final SheetRequest request;
            private final CopySheetRequest copySheet;

            public CopySheetBuilder() {
                request = new SheetRequest();
                copySheet = new CopySheetRequest();
                request.setCopySheet(copySheet);
            }

            /**
             * 获取或初始化source对象
             * 
             * @return source对象
             */
            private SheetSource getSource() {
                if (copySheet.source == null) {
                    copySheet.source = new SheetSource();
                }
                return copySheet.source;
            }

            /**
             * 获取或初始化destination对象
             * 
             * @return destination对象
             */
            private SheetDestination getDestination() {
                if (copySheet.destination == null) {
                    copySheet.destination = new SheetDestination();
                }
                return copySheet.destination;
            }

            /**
             * 设置源工作表ID
             * 
             * @param sheetId 要复制的源工作表ID
             * @return 当前构建器
             */
            public CopySheetBuilder sourceSheetId(String sheetId) {
                getSource().sheetId = sheetId;
                return this;
            }

            /**
             * 设置目标工作表标题
             * 
             * @param title 复制后的工作表标题，不填则默认为"源工作表名称(副本_源工作表的index值)"
             * @return 当前构建器
             */
            public CopySheetBuilder destinationTitle(String title) {
                getDestination().title = title;
                return this;
            }

            /**
             * 构建复制工作表请求
             * 
             * @return 工作表操作请求
             */
            public SheetRequest build() {
                return request;
            }
        }

        /**
         * 删除工作表的构建器 用于构建删除工作表的请求
         */
        public static class DeleteSheetBuilder {
            private final SheetRequest request;
            private final DeleteSheetRequest deleteSheet;

            public DeleteSheetBuilder() {
                request = new SheetRequest();
                deleteSheet = new DeleteSheetRequest();
                request.setDeleteSheet(deleteSheet);
            }

            /**
             * 设置要删除的工作表ID
             * 
             * @param sheetId 要删除的工作表ID
             * @return 当前构建器
             */
            public DeleteSheetBuilder sheetId(String sheetId) {
                deleteSheet.sheetId = sheetId;
                return this;
            }

            /**
             * 构建删除工作表请求
             * 
             * @return 工作表操作请求
             */
            public SheetRequest build() {
                return request;
            }
        }

        /**
         * 更新工作表的构建器 用于构建更新工作表属性的请求
         */
        public static class UpdateSheetBuilder {
            private final SheetRequest request;
            private final UpdateSheetRequest updateSheet;

            public UpdateSheetBuilder() {
                request = new SheetRequest();
                updateSheet = new UpdateSheetRequest();
                request.setUpdateSheet(updateSheet);
            }

            /**
             * 获取或初始化properties对象
             * 
             * @return properties对象
             */
            private SheetPropertiesUpdate getProperties() {
                if (updateSheet.properties == null) {
                    updateSheet.properties = new SheetPropertiesUpdate();
                }
                return updateSheet.properties;
            }

            /**
             * 设置要更新的工作表ID
             * 
             * @param sheetId 工作表ID
             * @return 当前构建器
             */
            public UpdateSheetBuilder sheetId(String sheetId) {
                getProperties().sheetId = sheetId;
                return this;
            }

            /**
             * 设置工作表标题
             * 
             * @param title 工作表标题，不能包含特殊字符：/ \ ? * [ ] :
             * @return 当前构建器
             */
            public UpdateSheetBuilder title(String title) {
                getProperties().title = title;
                return this;
            }

            /**
             * 设置工作表在电子表格中的位置索引
             * 
             * @param index 位置索引，从0开始计数
             * @return 当前构建器
             */
            public UpdateSheetBuilder index(Integer index) {
                getProperties().index = index;
                return this;
            }

            /**
             * 设置工作表是否隐藏
             * 
             * @param hidden true表示隐藏，false表示显示
             * @return 当前构建器
             */
            public UpdateSheetBuilder hidden(Boolean hidden) {
                getProperties().hidden = hidden;
                return this;
            }

            /**
             * 设置冻结行数
             * 
             * @param frozenRowCount 冻结的行数，0表示取消冻结
             * @return 当前构建器
             */
            public UpdateSheetBuilder frozenRowCount(Integer frozenRowCount) {
                getProperties().frozenRowCount = frozenRowCount;
                return this;
            }

            /**
             * 设置冻结列数
             * 
             * @param frozenColCount 冻结的列数，0表示取消冻结
             * @return 当前构建器
             */
            public UpdateSheetBuilder frozenColCount(Integer frozenColCount) {
                getProperties().frozenColCount = frozenColCount;
                return this;
            }

            /**
             * 设置用户ID类型
             * 
             * @param userIdType 用户ID类型，建议使用open_id或union_id，可选值：open_id、union_id
             * @return 当前构建器
             */
            public UpdateSheetBuilder userIdType(String userIdType) {
                getProperties().userIdType = userIdType;
                return this;
            }

            /**
             * 设置工作表保护
             * 
             * @param lock 是否要保护该工作表，可选值：LOCK（保护）、UNLOCK（取消保护）
             * @param lockInfo 保护工作表的备注信息
             * @param userIDs 有编辑权限的用户ID列表
             * @return 当前构建器
             */
            public UpdateSheetBuilder protect(String lock, String lockInfo, List<String> userIDs) {
                SheetPropertiesUpdate properties = getProperties();
                if (properties.protect == null) {
                    properties.protect = new SheetProtect();
                }
                properties.protect.lock = lock;
                properties.protect.lockInfo = lockInfo;
                properties.protect.userIDs = userIDs;
                return this;
            }

            /**
             * 构建更新工作表请求
             * 
             * @return 工作表操作请求
             */
            public SheetRequest build() {
                return request;
            }
        }
    }

    /**
     * 添加工作表请求
     */
    public static class AddSheetRequest {
        /**
         * 工作表属性
         */
        private SheetProperties properties;

        public SheetProperties getProperties() {
            return properties;
        }

        public void setProperties(SheetProperties properties) {
            this.properties = properties;
        }
    }

    /**
     * 工作表属性
     */
    public static class SheetProperties {
        /**
         * 新增工作表的标题 必填字段
         */
        private String title;

        /**
         * 新增工作表的位置 不填默认在工作表的第0索引位置增加工作表
         */
        private Integer index;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }
    }

    /**
     * 复制工作表请求
     */
    public static class CopySheetRequest {
        /**
         * 需要复制的工作表资源
         */
        private SheetSource source;

        /**
         * 新工作表的属性
         */
        private SheetDestination destination;

        public SheetSource getSource() {
            return source;
        }

        public void setSource(SheetSource source) {
            this.source = source;
        }

        public SheetDestination getDestination() {
            return destination;
        }

        public void setDestination(SheetDestination destination) {
            this.destination = destination;
        }
    }

    /**
     * 工作表源
     */
    public static class SheetSource {
        /**
         * 源工作表的ID 必填字段
         */
        private String sheetId;

        public String getSheetId() {
            return sheetId;
        }

        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }
    }

    /**
     * 工作表目标
     */
    public static class SheetDestination {
        /**
         * 新工作表名称 不填默认为"源工作表名称"+"(副本_源工作表的index值)"，如"Sheet1(副本_0)"
         */
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * 删除工作表请求
     */
    public static class DeleteSheetRequest {
        /**
         * 要删除的工作表的ID 必填字段
         */
        private String sheetId;

        public String getSheetId() {
            return sheetId;
        }

        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }
    }

    /**
     * 更新工作表请求
     */
    public static class UpdateSheetRequest {
        /**
         * 工作表属性
         */
        private SheetPropertiesUpdate properties;

        public SheetPropertiesUpdate getProperties() {
            return properties;
        }

        public void setProperties(SheetPropertiesUpdate properties) {
            this.properties = properties;
        }
    }

    /**
     * 工作表属性更新
     */
    public static class SheetPropertiesUpdate {
        /**
         * 要更新的工作表的ID 必填字段
         */
        private String sheetId;

        /**
         * 工作表的标题 更新的标题需符合以下规则： 长度不超过100个字符 不包含这些特殊字符：/ \ ? * [ ] :
         */
        private String title;

        /**
         * 工作表的位置 从0开始计数
         */
        private Integer index;

        /**
         * 是否要隐藏表格 默认值为false
         */
        private Boolean hidden;

        /**
         * 要冻结至指定行的行索引 若填3，表示从第一行冻结至第三行 小于或等于工作表的最大行数，0表示取消冻结行
         */
        private Integer frozenRowCount;

        /**
         * 要冻结至指定列的列索引 若填3，表示从第一列冻结至第三列 小于等于工作表的最大列数，0表示取消冻结列
         */
        private Integer frozenColCount;

        /**
         * 工作表保护设置
         */
        private SheetProtect protect;

        /**
         * 用户ID类型 影响protect.userIDs字段的ID类型 用户 ID 类型。默认为 lark_id，建议选择 open_id 或 union_id。了解更多，参考用户身份概述。可选值：
         * open_id：标识一个用户在某个应用中的身份。同一个用户在不同应用中的 Open ID 不同。 union_id：标识一个用户在某个应用开发商下的身份。同一用户在同一开发商下的应用中的 Union ID
         * 是相同的，在不同开发商下的应用中的 Union ID 是不同的。通过 Union ID，应用开发商可以把同个用户在多个应用中的身份关联起来。
         */
        private String userIdType;

        public String getSheetId() {
            return sheetId;
        }

        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Boolean getHidden() {
            return hidden;
        }

        public void setHidden(Boolean hidden) {
            this.hidden = hidden;
        }

        public Integer getFrozenRowCount() {
            return frozenRowCount;
        }

        public void setFrozenRowCount(Integer frozenRowCount) {
            this.frozenRowCount = frozenRowCount;
        }

        public Integer getFrozenColCount() {
            return frozenColCount;
        }

        public void setFrozenColCount(Integer frozenColCount) {
            this.frozenColCount = frozenColCount;
        }

        public SheetProtect getProtect() {
            return protect;
        }

        public void setProtect(SheetProtect protect) {
            this.protect = protect;
        }

        public String getUserIdType() {
            return userIdType;
        }

        public void setUserIdType(String userIdType) {
            this.userIdType = userIdType;
        }
    }

    /**
     * 工作表保护设置
     */
    public static class SheetProtect {
        /**
         * 是否要保护该工作表 必填字段 可选值： LOCK：保护 UNLOCK：取消保护
         */
        private String lock;

        /**
         * 保护工作表的备注信息
         */
        private String lockInfo;

        /**
         * 添加除操作用户与所有者外其他用户的ID 为其开通保护范围的编辑权限 ID类型由查询参数user_id_type决定 user_id_type不为空时，该字段生效
         */
        private List<String> userIDs;

        public String getLock() {
            return lock;
        }

        public void setLock(String lock) {
            this.lock = lock;
        }

        public String getLockInfo() {
            return lockInfo;
        }

        public void setLockInfo(String lockInfo) {
            this.lockInfo = lockInfo;
        }

        public List<String> getUserIDs() {
            return userIDs;
        }

        public void setUserIDs(List<String> userIDs) {
            this.userIDs = userIDs;
        }
    }

    /**
     * 工作表
     */
    public static class Sheet {
        private String sheetId;
        private String title;
        private Integer index;
        private Integer rowCount;
        private Integer columnCount;
        private Boolean hidden;
        private Integer frozenRowCount;
        private Integer frozenColCount;

        public String getSheetId() {
            return sheetId;
        }

        public void setSheetId(String sheetId) {
            this.sheetId = sheetId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Integer getRowCount() {
            return rowCount;
        }

        public void setRowCount(Integer rowCount) {
            this.rowCount = rowCount;
        }

        public Integer getColumnCount() {
            return columnCount;
        }

        public void setColumnCount(Integer columnCount) {
            this.columnCount = columnCount;
        }

        public Boolean getHidden() {
            return hidden;
        }

        public void setHidden(Boolean hidden) {
            this.hidden = hidden;
        }

        public Integer getFrozenRowCount() {
            return frozenRowCount;
        }

        public void setFrozenRowCount(Integer frozenRowCount) {
            this.frozenRowCount = frozenRowCount;
        }

        public Integer getFrozenColCount() {
            return frozenColCount;
        }

        public void setFrozenColCount(Integer frozenColCount) {
            this.frozenColCount = frozenColCount;
        }
    }
}