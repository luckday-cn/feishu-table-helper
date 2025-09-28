package cn.isliu.core.builder;

import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomCellService;
import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.FsTableUtil;
import cn.isliu.core.utils.PropertyUtil;
import cn.isliu.core.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 表格构建器
 * 
 * 提供链式调用方式创建飞书表格，支持字段过滤等高级功能。
 */
public class SheetBuilder<T> {

    private final String sheetName;
    private final String spreadsheetToken;
    private final Class<T> clazz;
    private List<String> includeFields;
    private final Map<String, Object> customProperties = new HashMap<>();
    private final Map<String, String> fieldDescriptions = new HashMap<>();

    /**
     * 构造函数
     *
     * @param sheetName 工作表名称
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象
     */
    public SheetBuilder(String sheetName, String spreadsheetToken, Class<T> clazz) {
        this.sheetName = sheetName;
        this.spreadsheetToken = spreadsheetToken;
        this.clazz = clazz;
    }

    /**
     * 设置包含的字段列表
     *
     * 指定要包含在表格中的字段名称列表。如果不设置，则包含所有带有@TableProperty注解的字段。
     *
     * @param fields 要包含的字段名称列表
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> includeColumnField(List<String> fields) {
        this.includeFields = new ArrayList<>(fields);
        return this;
    }

    /**
     * 设置自定义属性
     *
     * 添加一个自定义属性，可以在构建表格时使用
     *
     * @param key 属性键
     * @param value 属性值
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> addCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
        return this;
    }

    /**
     * 批量设置自定义属性
     *
     * 批量添加自定义属性，可以在构建表格时使用
     *
     * @param properties 自定义属性映射
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> addCustomProperties(Map<String, Object> properties) {
        this.customProperties.putAll(properties);
        return this;
    }

    /**
     * 获取自定义属性
     *
     * 根据键获取已设置的自定义属性值
     *
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    public Object getCustomProperty(String key) {
        return this.customProperties.get(key);
    }

    /**
     * 获取所有自定义属性
     *
     * @return 包含所有自定义属性的映射
     */
    public Map<String, Object> getCustomProperties() {
        return new HashMap<>(this.customProperties);
    }

    /**
     * 设置字段描述映射
     *
     * 为实体类字段设置自定义描述信息，用于在表格描述行中显示。
     * 如果字段在映射中存在描述，则使用映射中的描述；否则使用注解中的描述。
     *
     * @param fieldDescriptions 字段名到描述的映射，key为字段名，value为描述文本
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> fieldDescription(Map<String, String> fieldDescriptions) {
        this.fieldDescriptions.putAll(fieldDescriptions);
        return this;
    }

    /**
     * 设置单个字段描述
     *
     * 为指定字段设置自定义描述信息。
     *
     * @param fieldName 字段名
     * @param description 描述文本
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> fieldDescription(String fieldName, String description) {
        this.fieldDescriptions.put(fieldName, description);
        return this;
    }

    /**
     * 获取字段描述映射
     *
     * @return 包含所有字段描述的映射
     */
    public Map<String, String> getFieldDescriptions() {
        return new HashMap<>(this.fieldDescriptions);
    }

    /**
     * 构建表格并返回工作表ID
     *
     * 根据配置的参数创建飞书表格，包括表头、样式、单元格格式和下拉选项等。
     *
     * @return 创建成功返回工作表ID
     */
    public String build() {
        // 获取所有字段映射
        Map<String, FieldProperty> allFieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);

        // 根据includeFields过滤字段映射
        Map<String, FieldProperty> fieldsMap = filterFieldsMap(allFieldsMap);

        // 生成表头
        List<String> headers = PropertyUtil.getHeaders(fieldsMap, includeFields);

        // 获取表格配置
        TableConf tableConf = PropertyUtil.getTableConf(clazz);

        // 创建飞书客户端
        FeishuClient client = FsClient.getInstance().getClient();

        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, client, spreadsheetToken);

        // 2、添加表头数据
        FsApiUtil.putValues(spreadsheetToken, FsTableUtil.getHeadTemplateBuilder(sheetId, headers, fieldsMap, tableConf, fieldDescriptions), client);

        // 3、设置表格样式
        FsApiUtil.setTableStyle(FsTableUtil.getDefaultTableStyle(sheetId, fieldsMap, tableConf), client, spreadsheetToken);

        // 4、合并单元格
        List<CustomCellService.CellRequest> mergeCell = FsTableUtil.getMergeCell(sheetId, fieldsMap);
        if (!mergeCell.isEmpty()) {
            mergeCell.forEach(cell -> FsApiUtil.mergeCells(cell, client, spreadsheetToken));
        }

        // 5、设置单元格为文本格式
        if (tableConf.isText()) {
            String column = FsTableUtil.getColumnNameByNuNumber(headers.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, client, spreadsheetToken);
        }

        // 6、设置表格下拉
        try {
            FsTableUtil.setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId, tableConf.enableDesc(), customProperties);
        } catch (Exception e) {
            Logger.getLogger(SheetBuilder.class.getName()).log(Level.SEVERE,"【表格构建器】设置表格下拉异常！sheetId:" + sheetId + ", 错误信息：{}", e.getMessage());
        }

        return sheetId;
    }

    /**
     * 根据包含字段列表过滤字段映射
     *
     * @param allFieldsMap 所有字段映射
     * @return 过滤后的字段映射
     */
    private Map<String, FieldProperty> filterFieldsMap(Map<String, FieldProperty> allFieldsMap) {
        // 如果没有指定包含字段，返回所有字段
        if (includeFields == null || includeFields.isEmpty()) {
            return allFieldsMap;
        }

        // 根据字段名过滤，保留指定的字段
        return allFieldsMap.entrySet().stream()
                .filter(entry -> {
                    String field = entry.getValue().getField();
                    field = field.substring(field.lastIndexOf(".") + 1);
                    if (field.isEmpty()) {
                        return false;
                    }
                    return includeFields.contains(StringUtil.toUnderscoreCase(field));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}