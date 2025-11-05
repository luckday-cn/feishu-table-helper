package cn.isliu.core.builder;

import cn.isliu.core.FileData;
import cn.isliu.core.FsTableData;
import cn.isliu.core.Sheet;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.enums.FileType;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomValueService;
import cn.isliu.core.utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 数据写入构建器
 * 
 * 提供链式调用方式写入飞书表格数据，支持忽略唯一字段等高级功能。
 */
public class WriteBuilder<T> {

    private final String sheetId;
    private final String spreadsheetToken;
    private final List<T> dataList;
    private List<String> ignoreUniqueFields;
    private Class<?> clazz;
    private boolean ignoreNotFound;
    private String groupField;

    /**
     * 构造函数
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList 要写入的数据列表
     */
    public WriteBuilder(String sheetId, String spreadsheetToken, List<T> dataList) {
        this.sheetId = sheetId;
        this.spreadsheetToken = spreadsheetToken;
        this.dataList = dataList;
        this.clazz = null;
        this.ignoreNotFound = false;
        this.groupField = null;
    }

    /**
     * 设置计算唯一标识时忽略的字段列表
     *
     * 指定在计算数据行唯一标识时要忽略的字段名称列表。
     * 这些字段的值变化不会影响数据行的唯一性判断。
     *
     * @param fields 要忽略的字段名称列表
     * @return WriteBuilder实例，支持链式调用
     */
    public WriteBuilder<T> ignoreUniqueFields(List<String> fields) {
        this.ignoreUniqueFields = new ArrayList<>(fields);
        return this;
    }

    /**
     * 设置用于解析注解的实体类
     *
     * 指定用于解析@TableProperty注解的实体类。这个类可以与数据列表的类型不同，
     * 主要用于获取字段映射关系和表格配置信息。
     *
     * @param clazz 用于解析注解的实体类
     * @return WriteBuilder实例，支持链式调用
     */
    public WriteBuilder<T> clazz(Class<?> clazz) {
        this.clazz = clazz;
        return this;
    }

    /**
     * 设置是否忽略未找到的数据
     *
     * 指定在写入数据时是否忽略未找到的字段。如果设置为true，
     * 当数据中包含表格中不存在的字段时，写入操作将继续执行，
     * 而不会抛出异常。默认值为false。
     *
     * @param ignoreNotFound 是否忽略未找到的字段，默认值为false
     * @return WriteBuilder实例，支持链式调用
     */
    public WriteBuilder<T> ignoreNotFound(boolean ignoreNotFound) {
        this.ignoreNotFound = ignoreNotFound;
        return this;
    }

    /**
     * 设置分组字段
     *
     * 配置分组字段，用于处理数据行分组。
     * 当数据行存在分组字段时，将按照分组字段进行分组，并分别处理每个分组。
     *
     * @param groupField 分组字段名称
     * @return WriteBuilder实例，支持链式调用
     */
    public WriteBuilder<T> groupField(String groupField) {
        this.groupField = groupField;
        return this;
    }

    /**
     * 执行数据写入并返回操作结果
     *
     * 根据配置的参数将数据写入到飞书表格中，支持新增和更新操作。
     *
     * @return 写入操作结果
     */
    public Object build() {
        if (dataList.isEmpty()) {
            return null;
        }

        Class<?> aClass = clazz;
        Map<String, FieldProperty> fieldsMap;

        Map<String, String> fieldMap = new HashMap<>();
        Class<?> sourceClass = dataList.get(0).getClass();
        if (aClass != null && aClass.equals(sourceClass)) {
            fieldsMap = PropertyUtil.getTablePropertyFieldsMap(aClass);
        } else {
            fieldsMap = PropertyUtil.getTablePropertyFieldsMap(sourceClass);
        }

        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);

        TableConf tableConf = aClass != null ? PropertyUtil.getTableConf(aClass) : PropertyUtil.getTableConf(sourceClass);
        Map<String, String> titlePostionMap = FsTableUtil.getTitlePostionMap(sheet, spreadsheetToken, tableConf);
        Set<String> keys = titlePostionMap.keySet();

        fieldsMap.forEach((field, fieldProperty) -> {
            if (keys.contains(field)) {
                fieldMap.put(field, fieldProperty.getField());
            }
        });

        // 处理忽略字段名称映射
        List<String> processedIgnoreFields = processIgnoreFields(fieldsMap);

        // 使用支持忽略字段的方法获取表格数据
        List<FsTableData> fsTableDataList;
        if (groupField == null) {
            fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken, tableConf, processedIgnoreFields, fieldsMap);
        } else {
            Map<String, List<FsTableData>> groupFsTableData = FsTableUtil.getGroupFsTableData(sheet, spreadsheetToken, tableConf, processedIgnoreFields, fieldsMap);
            fsTableDataList = groupFsTableData.get(groupField);
        }

        if (!fsTableDataList.isEmpty()) {
            Map<String, String> fieldsPositionMap = fsTableDataList.get(0).getFieldsPositionMap();
            if (fieldsPositionMap != null) {
                titlePostionMap = fieldsPositionMap;
            }
        }

        Map<String, Integer> currTableRowMap = fsTableDataList.stream()
                .filter(fsTableData -> fsTableData.getRow() >= tableConf.headLine())
                .collect(Collectors.toMap(
                        FsTableData::getUniqueId,
                        FsTableData::getRow,
                        (existing, replacement) -> existing));

        final Integer[] row = {tableConf.headLine()};
        fsTableDataList.forEach(fsTableData -> {
            if ((fsTableData.getRow() + 1) > row[0]) {
                row[0] = fsTableData.getRow() + 1;
            }
        });

        // 初始化批量插入对象
        CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder = CustomValueService.ValueRequest.batchPutValues();

        List<FileData> fileDataList = new ArrayList<>();

        AtomicInteger rowCount = new AtomicInteger(row[0]);

        for (T data : dataList) {
            Map<String, Object> values = GenerateUtil.getFieldValue(data, fieldMap);

            // 计算唯一标识：如果data类型与aClass相同，使用忽略字段逻辑；否则直接从data获取uniqueId
            String uniqueId;
            if (data.getClass().equals(aClass)) {
                // 类型相同，使用忽略字段逻辑计算唯一标识
                uniqueId = calculateUniqueIdWithIgnoreFields(data, processedIgnoreFields, tableConf);
            } else {
                uniqueId = GenerateUtil.getUniqueId(data, tableConf);
            }

            AtomicReference<Integer> rowNum = new AtomicReference<>(currTableRowMap.get(uniqueId));
            if (uniqueId != null && rowNum.get() != null) {
                rowNum.set(rowNum.get() + 1);
                Map<String, String> finalTitlePostionMap = titlePostionMap;
                values.forEach((field, fieldValue) -> {
                    String position = finalTitlePostionMap.get(field);

                    if (position == null || position.isEmpty()) {
                        return;
                    }

                    if (fieldValue instanceof FileData) {
                        FileData fileData = (FileData) fieldValue;
                        String fileType = fileData.getFileType();
                        if (fileType.equals(FileType.IMAGE.getType())) {
                            fileData.setSheetId(sheetId);
                            fileData.setSpreadsheetToken(spreadsheetToken);
                            fileData.setPosition(position + rowNum.get());
                            fileDataList.add(fileData);
                        }
                    }
                    if (tableConf.enableCover() || fieldValue != null) {
                        resultValuesBuilder.addRange(sheetId, position + rowNum.get(), position + rowNum.get())
                                .addRow(GenerateUtil.getRowData(fieldValue));
                    }
                });
            } else if (!ignoreNotFound) {
                int rowCou = rowCount.incrementAndGet();
                Map<String, String> finalTitlePostionMap1 = titlePostionMap;
                values.forEach((field, fieldValue) -> {
                    String position = finalTitlePostionMap1.get(field);

                    if (position == null || position.isEmpty()) {
                        return;
                    }

                    if (fieldValue instanceof FileData) {
                        FileData fileData = (FileData) fieldValue;
                        fileData.setSheetId(sheetId);
                        fileData.setSpreadsheetToken(spreadsheetToken);
                        fileData.setPosition(position + rowCou);
                        fileDataList.add(fileData);
                    }

                    if (tableConf.enableCover() || fieldValue != null) {
                        resultValuesBuilder.addRange(sheetId, position + rowCou, position + rowCou)
                                .addRow(GenerateUtil.getRowData(fieldValue));
                    }
                });
            }
        }

        int rowTotal = sheet.getGridProperties().getRowCount();
        int rowNum = rowCount.get();
        if (rowNum >= rowTotal) {
            FsApiUtil.addRowColumns(sheetId, spreadsheetToken, "ROWS", Math.abs(rowTotal - rowNum), client);
        }

        fileDataList.forEach(fileData -> {
            try {
                FsApiUtil.imageUpload(fileData.getImageData(), fileData.getFileName(), fileData.getPosition(), fileData.getSheetId(), fileData.getSpreadsheetToken(), client);
            } catch (Exception e) {
                FsLogger.error(ErrorCode.BUSINESS_LOGIC_ERROR, "【飞书表格】 文件上传-文件上传异常! " + fileData.getFileUrl());
            }
        });

        CustomValueService.ValueRequest build = resultValuesBuilder.build();
        CustomValueService.ValueBatchUpdatePutRequest batchPutValues = build.getBatchPutValues();
        List<CustomValueService.ValueRangeItem> valueRanges = batchPutValues.getValueRanges();
        if (valueRanges != null && !valueRanges.isEmpty()) {
            return FsApiUtil.batchPutValues(sheetId, spreadsheetToken, resultValuesBuilder.build(), client);
        }
        return null;
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

    /**
     * 计算考虑忽略字段的唯一标识
     *
     * 根据忽略字段列表计算数据的唯一标识
     *
     * @param data 数据对象
     * @param processedIgnoreFields 处理后的忽略字段列表
     * @param tableConf 用于解析注解的表格配置
     * @return 唯一标识
     */
    private String calculateUniqueIdWithIgnoreFields(T data, List<String> processedIgnoreFields, TableConf tableConf) {
        try {
            // 获取所有字段值
            Map<String, Object> allFieldValues = GenerateUtil.getFieldValue(data, new HashMap<>());

            // 如果不需要忽略字段，使用原有逻辑
            if (tableConf.uniKeys().length > 0 || processedIgnoreFields.isEmpty()) {
                return GenerateUtil.getUniqueId(data, tableConf);
            }

            // 移除忽略字段后计算唯一标识
            Map<String, Object> filteredValues = new HashMap<>(allFieldValues);
            processedIgnoreFields.forEach(filteredValues::remove);

            // 将过滤后的值转换为JSON字符串并计算SHA256
            String jsonStr = StringUtil.mapToJson(filteredValues);
            return StringUtil.getSHA256(jsonStr);

        } catch (Exception e) {
            // 如果计算失败，回退到原有逻辑
            return GenerateUtil.getUniqueId(data, tableConf);
        }
    }
}
