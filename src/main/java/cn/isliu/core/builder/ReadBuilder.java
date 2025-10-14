package cn.isliu.core.builder;

import cn.isliu.core.BaseEntity;
import cn.isliu.core.FsTableData;
import cn.isliu.core.Sheet;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.utils.*;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据读取构建器
 * 
 * 提供链式调用方式读取飞书表格数据，支持忽略唯一字段等高级功能。
 */
public class ReadBuilder<T> {

    private final String sheetId;
    private final String spreadsheetToken;
    private final Class<T> clazz;
    private List<String> ignoreUniqueFields;

    /**
     * 构造函数
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象
     */
    public ReadBuilder(String sheetId, String spreadsheetToken, Class<T> clazz) {
        this.sheetId = sheetId;
        this.spreadsheetToken = spreadsheetToken;
        this.clazz = clazz;
    }

    /**
     * 设置计算唯一标识时忽略的字段列表
     *
     * 指定在计算数据行唯一标识时要忽略的字段名称列表。
     * 这些字段的值变化不会影响数据行的唯一性判断。
     *
     * @param fields 要忽略的字段名称列表
     * @return ReadBuilder实例，支持链式调用
     */
    public ReadBuilder<T> ignoreUniqueFields(List<String> fields) {
        this.ignoreUniqueFields = new ArrayList<>(fields);
        return this;
    }

    /**
     * 执行数据读取并返回实体类对象列表
     *
     * 根据配置的参数从飞书表格中读取数据并映射到实体类对象列表中。
     *
     * @return 映射后的实体类对象列表
     */
    public List<T> build() {
        List<T> results = new ArrayList<>();
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        TableConf tableConf = PropertyUtil.getTableConf(clazz);

        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);

        // 处理忽略字段名称映射
        List<String> processedIgnoreFields = processIgnoreFields(fieldsMap);

        // 使用支持忽略字段的方法获取表格数据
        List<FsTableData> fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken, tableConf, processedIgnoreFields, fieldsMap);

        List<String> fieldPathList = fieldsMap.values().stream().map(FieldProperty::getField).collect(Collectors.toList());

        fsTableDataList.forEach(tableData -> {
            Object data = tableData.getData();
            if (data instanceof HashMap) {
                Map<String, Object> rowData = (HashMap<String, Object>) data;
                JsonObject jsonObject = JSONUtil.convertMapToJsonObject(rowData);
                Map<String, Object> dataMap = ConvertFieldUtil.convertPositionToField(jsonObject, fieldsMap);
                T t = GenerateUtil.generateInstance(fieldPathList, clazz, dataMap);
                if (t instanceof BaseEntity) {
                    BaseEntity baseEntity = (BaseEntity) t;
                    baseEntity.setUniqueId(tableData.getUniqueId());
                    baseEntity.setRow(tableData.getRow());
                    baseEntity.setRowData(rowData);
                }
                results.add(t);
            }
        });
        return results;
    }

    public Map<String, List<T>> groupBuild() {
        Map<String, List<T>> results = new HashMap<>();
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        TableConf tableConf = PropertyUtil.getTableConf(clazz);

        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);

        // 处理忽略字段名称映射
        List<String> processedIgnoreFields = processIgnoreFields(fieldsMap);

        // 使用支持忽略字段的方法获取表格数据
        Map<String, List<FsTableData>> fsTableDataMap = FsTableUtil.getGroupFsTableData(sheet, spreadsheetToken, tableConf, processedIgnoreFields, fieldsMap);

        List<String> fieldPathList = fieldsMap.values().stream().map(FieldProperty::getField).collect(Collectors.toList());

        fsTableDataMap.forEach((key, fsTableDataList) -> {
            List<T> groupResults = new ArrayList<>();
            fsTableDataList.stream().filter(tableData -> tableData.getRow() >= tableConf.headLine()).forEach(tableData -> {
                Object data = tableData.getData();
                if (data instanceof HashMap) {
                    Map<String, Object> rowData = (HashMap<String, Object>) data;
                    JsonObject jsonObject = JSONUtil.convertMapToJsonObject(rowData);
                    Map<String, Object> dataMap = ConvertFieldUtil.convertPositionToField(jsonObject, fieldsMap);
                    T t = GenerateUtil.generateInstance(fieldPathList, clazz, dataMap);
                    if (t instanceof BaseEntity) {
                        BaseEntity baseEntity = (BaseEntity) t;
                        baseEntity.setUniqueId(tableData.getUniqueId());
                        baseEntity.setRow(tableData.getRow());
                        baseEntity.setRowData(rowData);
                    }
                    groupResults.add(t);
                }
            });
            results.put(key, groupResults);
        });

        return results;
    }

    /**
     * 处理忽略字段名称映射
     *
     * 将实体字段名称映射为表格列名称
     *
     * @param fieldsMap 字段映射
     * @return 处理后的忽略字段列表
     */
    private List<String> processIgnoreFields(Map<String, FieldProperty> fieldsMap) {
        if (ignoreUniqueFields == null || ignoreUniqueFields.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> processedFields = new ArrayList<>();

        // 遍历字段映射，找到对应的表格列名
        for (Map.Entry<String, FieldProperty> entry : fieldsMap.entrySet()) {
            String fieldName = entry.getValue().getField();
            // 获取字段的最后一部分名称（去掉嵌套路径）
            String simpleFieldName = fieldName.substring(fieldName.lastIndexOf(".") + 1);

            // 如果忽略字段列表中包含此字段名，则添加对应的表格列名
            if (ignoreUniqueFields.contains(simpleFieldName)) {
                String tableColumnName = entry.getKey(); // 表格列名（注解中的value值）
                processedFields.add(tableColumnName);
            }
        }

        return processedFields;
    }
}
