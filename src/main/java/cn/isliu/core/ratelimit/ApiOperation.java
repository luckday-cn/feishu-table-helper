package cn.isliu.core.ratelimit;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * 飞书 API 操作枚举
 *
 * <p>枚举定义了不同 API 行为的频控规则，便于统一限流管理。</p>
 */
public enum ApiOperation {

    CREATE_SPREADSHEET("创建表格", Duration.ofMinutes(1), 20, false),
    UPDATE_SPREADSHEET_PROPERTIES("修改电子表格属性", Duration.ofMinutes(1), 20, true),
    GET_SPREADSHEET("获取电子表格信息", Duration.ofMinutes(1), 100, false),
    SHEET_OPERATION("操作工作表", Duration.ofSeconds(1), 100, true),
    UPDATE_SHEET_PROPERTIES("更新工作表属性", Duration.ofSeconds(1), 100, true),
    MOVE_DIMENSION("移动行列", Duration.ofMinutes(1), 100, true),
    INSERT_DATA("插入数据", Duration.ofSeconds(1), 100, true),
    APPEND_DATA("追加数据", Duration.ofSeconds(1), 100, true),
    INSERT_ROWS_COLUMNS("插入行列", Duration.ofSeconds(1), 100, true),
    ADD_ROWS_COLUMNS("增加行列", Duration.ofSeconds(1), 100, true),
    UPDATE_ROWS_COLUMNS("更新行列", Duration.ofSeconds(1), 100, true),
    DELETE_ROWS_COLUMNS("删除行列", Duration.ofSeconds(1), 100, true),
    READ_SINGLE_RANGE("读取单个范围", Duration.ofSeconds(1), 100, false),
    READ_MULTI_RANGE("读取多个范围", Duration.ofSeconds(1), 100, false),
    WRITE_SINGLE_RANGE("向单个范围写入数据", Duration.ofSeconds(1), 100, true),
    WRITE_MULTI_RANGE("向多个范围写入数据", Duration.ofSeconds(1), 100, true),
    SET_CELL_STYLE("设置单元格样式", Duration.ofSeconds(1), 100, true),
    BATCH_SET_CELL_STYLE("批量设置单元格样式", Duration.ofSeconds(1), 100, true),
    MERGE_CELLS("合并单元格", Duration.ofSeconds(1), 100, true),
    SPLIT_CELLS("拆分单元格", Duration.ofSeconds(1), 100, true),
    WRITE_IMAGE("写入图片", Duration.ofSeconds(1), 100, true),
    FIND_CELLS("查找单元格", Duration.ofMinutes(1), 100, false),
    REPLACE_CELLS("替换单元格", Duration.ofMinutes(1), 20, true),
    CREATE_CONDITIONAL_FORMAT("创建条件格式", Duration.ofSeconds(1), 100, true),
    GET_CONDITIONAL_FORMAT("获取条件格式", Duration.ofSeconds(1), 100, true),
    UPDATE_CONDITIONAL_FORMAT("更新条件格式", Duration.ofSeconds(1), 100, true),
    DELETE_CONDITIONAL_FORMAT("删除条件格式", Duration.ofSeconds(1), 100, true),
    ADD_PROTECTED_RANGE("增加保护范围", Duration.ofSeconds(1), 100, true),
    GET_PROTECTED_RANGE("获取保护范围", Duration.ofSeconds(1), 100, true),
    UPDATE_PROTECTED_RANGE("修改保护范围", Duration.ofSeconds(1), 100, true),
    DELETE_PROTECTED_RANGE("删除保护范围", Duration.ofSeconds(1), 100, true),
    SET_DROPDOWN("设置下拉列表", Duration.ofSeconds(1), 100, true),
    DELETE_DROPDOWN("删除下拉列表设置", Duration.ofSeconds(1), 100, true),
    UPDATE_DROPDOWN("更新下拉列表设置", Duration.ofSeconds(1), 100, true),
    QUERY_DROPDOWN("查询下拉列表设置", Duration.ofSeconds(1), 100, true),
    GET_FILTER("获取筛选", Duration.ofMinutes(1), 100, false),
    CREATE_FILTER("创建筛选", Duration.ofMinutes(1), 20, true),
    UPDATE_FILTER("更新筛选", Duration.ofMinutes(1), 20, true),
    DELETE_FILTER("删除筛选", Duration.ofMinutes(1), 100, true),
    CREATE_FILTER_VIEW("创建筛选视图", Duration.ofMinutes(1), 100, true),
    GET_FILTER_VIEW("获取筛选视图", Duration.ofMinutes(1), 100, true),
    QUERY_FILTER_VIEW("查询筛选视图", Duration.ofMinutes(1), 100, true),
    UPDATE_FILTER_VIEW("更新筛选视图", Duration.ofMinutes(1), 100, true),
    DELETE_FILTER_VIEW("删除筛选视图", Duration.ofMinutes(1), 100, true),
    CREATE_FILTER_CONDITION("创建筛选条件", Duration.ofMinutes(1), 100, true),
    GET_FILTER_CONDITION("获取筛选条件", Duration.ofMinutes(1), 100, true),
    QUERY_FILTER_CONDITION("查询筛选条件", Duration.ofMinutes(1), 100, true),
    UPDATE_FILTER_CONDITION("更新筛选条件", Duration.ofMinutes(1), 100, true),
    DELETE_FILTER_CONDITION("删除筛选条件", Duration.ofMinutes(1), 100, true),
    CREATE_FLOATING_IMAGE("创建浮动图片", Duration.ofMinutes(1), 100, true),
    GET_FLOATING_IMAGE("获取浮动图片", Duration.ofMinutes(1), 100, true),
    QUERY_FLOATING_IMAGE("查询浮动图片", Duration.ofMinutes(1), 100, true),
    UPDATE_FLOATING_IMAGE("更新浮动图片", Duration.ofMinutes(1), 100, true),
    DELETE_FLOATING_IMAGE("删除浮动图片", Duration.ofMinutes(1), 100, true),
    GENERIC_OPERATION("通用操作", Duration.ofSeconds(1), 50, false);

    private static final Map<ApiOperation, RateLimitRule> CACHE = new EnumMap<>(ApiOperation.class);

    static {
        for (ApiOperation operation : values()) {
            CACHE.put(operation, RateLimitRule.builder()
                    .operation(operation)
                    .window(operation.window)
                    .permits(operation.permits)
                    .requireDocumentLock(operation.requireDocumentLock)
                    .allow429Retry(operation.requireDocumentLock || operation.allow429Retry)
                    .build());
        }
    }

    private final String description;
    private final Duration window;
    private final int permits;
    private final boolean requireDocumentLock;
    private final boolean allow429Retry;

    ApiOperation(String description, Duration window, int permits, boolean requireDocumentLock) {
        this(description, window, permits, requireDocumentLock, true);
    }

    ApiOperation(String description,
                 Duration window,
                 int permits,
                 boolean requireDocumentLock,
                 boolean allow429Retry) {
        this.description = description;
        this.window = window;
        this.permits = permits;
        this.requireDocumentLock = requireDocumentLock;
        this.allow429Retry = allow429Retry;
    }

    public String getDescription() {
        return description;
    }

    public RateLimitRule getRule() {
        return CACHE.get(this);
    }
}

