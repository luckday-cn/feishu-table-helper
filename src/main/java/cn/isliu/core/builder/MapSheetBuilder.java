package cn.isliu.core.builder;

import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.MapFieldDefinition;
import cn.isliu.core.config.MapSheetConfig;
import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomCellService;
import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.FsTableUtil;
import cn.isliu.core.utils.MapOptionsUtil;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Map 表格构建器
 * 
 * 提供链式调用方式创建飞书表格，使用配置对象而不是注解
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapSheetBuilder {

    private final String sheetName;
    private final String spreadsheetToken;
    private MapSheetConfig config;

    /**
     * 构造函数
     *
     * @param sheetName 工作表名称
     * @param spreadsheetToken 电子表格Token
     */
    public MapSheetBuilder(String sheetName, String spreadsheetToken) {
        this.sheetName = sheetName;
        this.spreadsheetToken = spreadsheetToken;
        this.config = MapSheetConfig.createDefault();
    }

    /**
     * 设置表格配置
     *
     * @param config Map表格配置
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder config(MapSheetConfig config) {
        this.config = config;
        return this;
    }

    /**
     * 设置标题行
     *
     * @param titleRow 标题行行号
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder titleRow(int titleRow) {
        this.config.setTitleRow(titleRow);
        return this;
    }

    /**
     * 设置数据起始行
     *
     * @param headLine 数据起始行号
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder headLine(int headLine) {
        this.config.setHeadLine(headLine);
        return this;
    }

    /**
     * 设置唯一键字段
     *
     * @param uniKeyNames 唯一键字段名集合
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder uniKeyNames(Set<String> uniKeyNames) {
        this.config.setUniKeyNames(uniKeyNames);
        return this;
    }

    /**
     * 添加唯一键字段
     *
     * @param uniKeyName 唯一键字段名
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder addUniKeyName(String uniKeyName) {
        this.config.addUniKeyName(uniKeyName);
        return this;
    }

    /**
     * 设置字段列表
     *
     * @param fields 字段定义列表
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder fields(List<MapFieldDefinition> fields) {
        this.config.setFields(new ArrayList<>(fields));
        return this;
    }

    /**
     * 添加单个字段
     *
     * @param field 字段定义
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder addField(MapFieldDefinition field) {
        this.config.addField(field);
        return this;
    }

    /**
     * 批量添加字段
     *
     * @param fields 字段定义列表
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder addFields(List<MapFieldDefinition> fields) {
        this.config.addFields(fields);
        return this;
    }

    /**
     * 批量添加字段（可变参数）
     *
     * @param fields 字段定义可变参数
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder addFields(MapFieldDefinition... fields) {
        this.config.addFields(fields);
        return this;
    }

    /**
     * 设置表头样式
     *
     * @param fontColor 字体颜色
     * @param backColor 背景颜色
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder headStyle(String fontColor, String backColor) {
        this.config.setHeadFontColor(fontColor);
        this.config.setHeadBackColor(backColor);
        return this;
    }

    /**
     * 设置是否为纯文本格式
     *
     * @param isText 是否设置为纯文本
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder isText(boolean isText) {
        this.config.setText(isText);
        return this;
    }

    /**
     * 设置是否启用字段描述
     *
     * @param enableDesc 是否启用
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder enableDesc(boolean enableDesc) {
        this.config.setEnableDesc(enableDesc);
        return this;
    }

    /**
     * 设置分组字段
     *
     * @param groupFields 分组字段可变参数
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder groupFields(String... groupFields) {
        this.config.setGroupFields(Arrays.asList(groupFields));
        return this;
    }

    /**
     * 添加自定义属性
     *
     * @param key 属性键
     * @param value 属性值
     * @return MapSheetBuilder实例
     */
    public MapSheetBuilder addCustomProperty(String key, Object value) {
        this.config.addCustomProperty(key, value);
        return this;
    }

    /**
     * 构建表格并返回工作表ID
     *
     * @return 创建成功返回工作表ID
     */
    public String build() {
        // 检查字段列表
        if (config.getFields().isEmpty()) {
            throw new IllegalArgumentException("字段定义列表不能为空");
        }

        // 判断是否为分组表格
        if (config.getGroupFields() != null && !config.getGroupFields().isEmpty()) {
            return buildGroupSheet();
        } else {
            return buildNormalSheet();
        }
    }

    /**
     * 构建普通表格
     */
    private String buildNormalSheet() {
        // 转换字段定义为 FieldProperty
        Map<String, FieldProperty> fieldsMap = convertToFieldsMap(config.getFields());

        // 生成表头
        List<String> headers = config.getFields().stream()
                .sorted(Comparator.comparingInt(MapFieldDefinition::getOrder))
                .map(MapFieldDefinition::getFieldName)
                .collect(Collectors.toList());

        // 创建 TableConf
        TableConf tableConf = createTableConf();

        // 创建飞书客户端
        FeishuClient client = FsClient.getInstance().getClient();

        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, client, spreadsheetToken);

        // 2、添加表头数据
        Map<String, String> fieldDescriptions = buildFieldDescriptions();
        FsApiUtil.putValues(spreadsheetToken,
                FsTableUtil.getHeadTemplateBuilder(sheetId, headers, fieldsMap, tableConf, fieldDescriptions),
                client);

        // 3、设置单元格为文本格式
        if (config.isText()) {
            String column = FsTableUtil.getColumnNameByNuNumber(headers.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, client, spreadsheetToken);
        }

        // 4、设置表格样式
        FsApiUtil.setTableStyle(
                FsTableUtil.getDefaultTableStyle(sheetId, fieldsMap, tableConf),
                client, spreadsheetToken);

        // 5、合并单元格
        List<CustomCellService.CellRequest> mergeCell = FsTableUtil.getMergeCell(sheetId, fieldsMap);
        if (!mergeCell.isEmpty()) {
            mergeCell.forEach(cell -> FsApiUtil.mergeCells(cell, client, spreadsheetToken));
        }

        // 6、设置表格下拉
        try {
            // 准备自定义属性，包含字段的 options 配置
            Map<String, Object> customProps = prepareCustomProperties(fieldsMap);
            FsTableUtil.setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId,
                    config.isEnableDesc(), customProps);
        } catch (Exception e) {
            Logger.getLogger(MapSheetBuilder.class.getName()).log(Level.SEVERE,
                    "【Map表格构建器】设置表格下拉异常！sheetId:" + sheetId + ", 错误信息：{}", e.getMessage());
        }

        return sheetId;
    }

    /**
     * 构建分组表格
     */
    private String buildGroupSheet() {
        // 转换字段定义为 FieldProperty
        Map<String, FieldProperty> fieldsMap = convertToFieldsMap(config.getFields());

        // 生成表头
        List<String> headers = config.getFields().stream()
                .sorted(Comparator.comparingInt(MapFieldDefinition::getOrder))
                .map(MapFieldDefinition::getFieldName)
                .collect(Collectors.toList());

        // 创建 TableConf
        TableConf tableConf = createTableConf();

        // 创建飞书客户端
        FeishuClient client = FsClient.getInstance().getClient();

        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, client, spreadsheetToken);

        // 2、添加表头数据（分组模式）
        List<String> groupFieldList = config.getGroupFields();
        List<String> headerList = FsTableUtil.getGroupHeaders(groupFieldList, headers);
        Map<String, String> fieldDescriptions = buildFieldDescriptions();
        FsApiUtil.putValues(spreadsheetToken,
                FsTableUtil.getHeadTemplateBuilder(sheetId, headers, headerList, fieldsMap,
                        tableConf, fieldDescriptions, groupFieldList),
                client);

        // 3、设置单元格为文本格式
        if (config.isText()) {
            String column = FsTableUtil.getColumnNameByNuNumber(headerList.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, client, spreadsheetToken);
        }

        // 4、设置表格样式（分组模式）
        Map<String, String[]> positions = FsTableUtil.calculateGroupPositions(headers, groupFieldList);
        positions.forEach((key, value) ->
                FsApiUtil.setTableStyle(FsTableUtil.getDefaultTableStyle(sheetId, value, tableConf),
                        client, spreadsheetToken));

        // 5、合并单元格
        List<CustomCellService.CellRequest> mergeCell = FsTableUtil.getMergeCell(sheetId, positions.values());
        if (!mergeCell.isEmpty()) {
            mergeCell.forEach(cell -> FsApiUtil.mergeCells(cell, client, spreadsheetToken));
        }

        // 6、设置表格下拉
        try {
            String[] headerWithColumnIdentifiers = FsTableUtil.generateHeaderWithColumnIdentifiers(headers, groupFieldList);
            // 准备自定义属性，包含字段的 options 配置
            Map<String, Object> customProps = prepareCustomProperties(fieldsMap);
            FsTableUtil.setTableOptions(spreadsheetToken, headerWithColumnIdentifiers, fieldsMap,
                    sheetId, config.isEnableDesc(), customProps);
        } catch (Exception e) {
            Logger.getLogger(MapSheetBuilder.class.getName()).log(Level.SEVERE,
                    "【Map表格构建器】设置表格下拉异常！sheetId:" + sheetId + ", 错误信息：{}", e.getMessage());
        }

        return sheetId;
    }

    /**
     * 将 MapFieldDefinition 列表转换为 FieldProperty Map
     */
    private Map<String, FieldProperty> convertToFieldsMap(List<MapFieldDefinition> fields) {
        Map<String, FieldProperty> fieldsMap = new LinkedHashMap<>();

        for (MapFieldDefinition field : fields) {
            TableProperty tableProperty = createTableProperty(field);
            FieldProperty fieldProperty = new FieldProperty(field.getFieldName(), tableProperty);
            fieldsMap.put(field.getFieldName(), fieldProperty);
        }

        return fieldsMap;
    }

    /**
     * 根据 MapFieldDefinition 创建 TableProperty 注解实例
     */
    private TableProperty createTableProperty(MapFieldDefinition field) {
        return new TableProperty() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TableProperty.class;
            }

            @Override
            public String[] value() {
                return new String[]{field.getFieldName()};
            }

            @Override
            public String desc() {
                return field.getDescription() != null ? field.getDescription() : "";
            }

            @Override
            public String field() {
                return field.getFieldName();
            }

            @Override
            public int order() {
                return field.getOrder();
            }

            @Override
            public TypeEnum type() {
                return field.getType() != null ? field.getType() : TypeEnum.TEXT;
            }

            @Override
            public Class<? extends BaseEnum> enumClass() {
                // 优先级1：如果配置了 enumClass，直接返回
                if (field.getEnumClass() != null && field.getEnumClass() != BaseEnum.class) {
                    return field.getEnumClass();
                }

                // 优先级2：如果没有配置 enumClass 但配置了 options，创建动态枚举类
                // 注意：这里返回 BaseEnum.class，实际的 options 通过 optionsClass 处理
                return BaseEnum.class;
            }

            @Override
            public Class<? extends FieldValueProcess> fieldFormatClass() {
                return FieldValueProcess.class;
            }

            @Override
            public Class<? extends OptionsValueProcess> optionsClass() {
                // 优先级1：如果配置了 optionsClass，直接返回
                if (field.getOptionsClass() != null && field.getOptionsClass() != OptionsValueProcess.class) {
                    return field.getOptionsClass();
                }

                // 优先级2：如果配置了 options 但没有 optionsClass，创建动态的处理类
                if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                    return MapOptionsUtil.createDynamicOptionsClass(field.getOptions());
                }

                // 优先级3：返回默认值
                return OptionsValueProcess.class;
            }
        };
    }

    /**
     * 创建 TableConf 注解实例
     */
    private TableConf createTableConf() {
        return new TableConf() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TableConf.class;
            }

            @Override
            public String[] uniKeys() {
                Set<String> uniKeyNames = config.getUniKeyNames();
                return uniKeyNames != null ? uniKeyNames.toArray(new String[0]) : new String[0];
            }

            @Override
            public int headLine() {
                return config.getHeadLine();
            }

            @Override
            public int titleRow() {
                return config.getTitleRow();
            }

            @Override
            public boolean enableCover() {
                return config.isEnableCover();
            }

            @Override
            public boolean isText() {
                return config.isText();
            }

            @Override
            public boolean enableDesc() {
                return config.isEnableDesc();
            }

            @Override
            public String headFontColor() {
                return config.getHeadFontColor();
            }

            @Override
            public String headBackColor() {
                return config.getHeadBackColor();
            }

            @Override
            public boolean upsert() {
                // MapSheetConfig 继承自 MapTableConfig，支持 upsert 配置
                return config.isUpsert();
            }
        };
    }

    /**
     * 构建字段描述映射
     */
    private Map<String, String> buildFieldDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        for (MapFieldDefinition field : config.getFields()) {
            if (field.getDescription() != null && !field.getDescription().isEmpty()) {
                descriptions.put(field.getFieldName(), field.getDescription());
            }
        }
        return descriptions;
    }

    /**
     * 准备自定义属性
     *
     * 将字段配置的 options 放入 customProperties，供 DynamicOptionsProcess 使用
     */
    private Map<String, Object> prepareCustomProperties(Map<String, FieldProperty> fieldsMap) {
        Map<String, Object> customProps = new HashMap<>();

        // 复制原有的自定义属性
        if (config.getCustomProperties() != null) {
            customProps.putAll(config.getCustomProperties());
        }

        // 为每个配置了 options 的字段添加选项到 customProperties
        for (MapFieldDefinition field : config.getFields()) {
            if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                // 使用字段名作为 key 前缀，避免冲突
                customProps.put("_dynamicOptions_" + field.getFieldName(), field.getOptions());
            }
        }

        return customProps;
    }
}
