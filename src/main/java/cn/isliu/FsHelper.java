package cn.isliu;

import cn.isliu.core.BaseEntity;
import cn.isliu.core.FileData;
import cn.isliu.core.FsTableData;
import cn.isliu.core.Sheet;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.builder.MapWriteBuilder;
import cn.isliu.core.builder.ReadBuilder;
import cn.isliu.core.builder.SheetBuilder;
import cn.isliu.core.builder.WriteBuilder;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.MapTableConfig;
import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.enums.FileType;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomCellService;
import cn.isliu.core.service.CustomValueService;
import cn.isliu.core.utils.*;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 飞书表格助手主入口类
 * 
 * 提供对飞书表格的创建、读取和写入操作的统一接口。
 * 通过实体类注解映射，简化对飞书表格的操作。
 */
public class FsHelper {

    /**
     * 创建飞书表格
     *
     * 根据传入的实体类结构，在指定的电子表格中创建一个新的工作表，
     * 并设置表头、样式、单元格格式和下拉选项等。
     *
     * @param sheetName 工作表名称
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象，用于解析表头和字段属性
     * @param <T> 实体类泛型
     * @return 创建成功返回工作表ID
     */
    public static <T> String create(String sheetName, String spreadsheetToken, Class<T> clazz) {
        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);
        List<String> headers = PropertyUtil.getHeaders(fieldsMap);

        TableConf tableConf = PropertyUtil.getTableConf(clazz);

        FeishuClient client = FsClient.getInstance().getClient();
        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, client, spreadsheetToken);

        // 2 添加表头数据
        FsApiUtil.putValues(spreadsheetToken, FsTableUtil.getHeadTemplateBuilder(sheetId, headers, fieldsMap, tableConf), client);

        // 3 设置表格样式
        FsApiUtil.setTableStyle(FsTableUtil.getDefaultTableStyle(sheetId, fieldsMap, tableConf), client, spreadsheetToken);

        // 4 合并单元格
        List<CustomCellService.CellRequest> mergeCell = FsTableUtil.getMergeCell(sheetId, fieldsMap);
        if (!mergeCell.isEmpty()) {
            mergeCell.forEach(cell ->  FsApiUtil.mergeCells(cell, client, spreadsheetToken));
        }

        // 4 设置单元格为文本格式
        if (tableConf.isText()) {
            String column = FsTableUtil.getColumnNameByNuNumber(headers.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, client, spreadsheetToken);
        }

        try {
            // 5 设置表格下拉
            FsTableUtil.setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId, tableConf.enableDesc());
        } catch (Exception e) {
            Logger.getLogger(SheetBuilder.class.getName()).log(Level.SEVERE,"【表格构建器】设置表格下拉异常！sheetId:" + sheetId + ", 错误信息：{}", e.getMessage());
        }
        return sheetId;
    }

    /**
     * 创建飞书表格构建器
     *
     * 返回一个表格构建器实例，支持链式调用和高级配置选项，
     * 如字段过滤等功能。
     *
     * @param sheetName 工作表名称
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象，用于解析表头和字段属性
     * @param <T> 实体类泛型
     * @return SheetBuilder实例，支持链式调用
     */
    public static <T> SheetBuilder<T> createBuilder(String sheetName, String spreadsheetToken, Class<T> clazz) {
        return new SheetBuilder<>(sheetName, spreadsheetToken, clazz);
    }


    /**
     * 从飞书表格中读取数据
     *
     * 根据指定的工作表ID和电子表格Token，读取表格数据并映射到实体类对象列表中。
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象，用于数据映射
     * @param <T> 实体类泛型
     * @return 映射后的实体类对象列表
     */
    public static <T> List<T> read(String sheetId, String spreadsheetToken, Class<T> clazz) {
        List<T> results = new ArrayList<>();
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        TableConf tableConf = PropertyUtil.getTableConf(clazz);

        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);
        List<FsTableData> fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken, tableConf, fieldsMap);

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
        return  results;
    }

    /**
     * 创建飞书表格数据读取构建器
     *
     * 返回一个数据读取构建器实例，支持链式调用和高级配置选项，
     * 如忽略唯一字段等功能。
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象，用于数据映射
     * @param <T> 实体类泛型
     * @return ReadBuilder实例，支持链式调用
     */
    public static <T> ReadBuilder<T> readBuilder(String sheetId, String spreadsheetToken, Class<T> clazz) {
        return new ReadBuilder<>(sheetId, spreadsheetToken, clazz);
    }

    /**
     * 将数据写入飞书表格
     *
     * 将实体类对象列表写入到指定的飞书表格中，支持新增和更新操作。
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList 实体类对象列表
     * @param <T> 实体类泛型
     * @return 写入操作结果
     */
    public static <T> Object write(String sheetId, String spreadsheetToken, List<T> dataList) {
        if (dataList.isEmpty()) {
            return null;
        }

        Class<?> aClass = dataList.get(0).getClass();
        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(aClass);
        TableConf tableConf = PropertyUtil.getTableConf(aClass);

        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        List<FsTableData> fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken, tableConf, fieldsMap);
        Map<String, Integer> currTableRowMap = fsTableDataList.stream().collect(Collectors.toMap(FsTableData::getUniqueId, FsTableData::getRow));

        final Integer[] row = {tableConf.headLine()};
        fsTableDataList.forEach(fsTableData -> {
            if (fsTableData.getRow() > row[0]) {
                row[0] = fsTableData.getRow();
            }
        });

        Map<String, String> titlePostionMap = FsTableUtil.getTitlePostionMap(sheet, spreadsheetToken, tableConf);
        Set<String> keys = titlePostionMap.keySet();

        Map<String, String> fieldMap = new HashMap<>();
        fieldsMap.forEach((field, fieldProperty) -> {
            if (keys.contains(field)) {
                fieldMap.put(field, fieldProperty.getField());
            }
        });

        // 初始化批量插入对象
        CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder = CustomValueService.ValueRequest.batchPutValues();

        List<FileData> fileDataList = new ArrayList<>();

        AtomicInteger rowCount = new AtomicInteger(row[0]);

        for (T data : dataList) {
            Map<String, Object> values = GenerateUtil.getFieldValue(data, fieldMap);

            String uniqueId =  GenerateUtil.getUniqueId(data, tableConf);

            AtomicReference<Integer> rowNum = new AtomicReference<>(currTableRowMap.get(uniqueId));
            if (uniqueId != null && rowNum.get() != null) {
                rowNum.set(rowNum.get() + 1);
                values.forEach((field, fieldValue) -> {
                    String position = titlePostionMap.get(field);

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
            } else {
                int rowCou = rowCount.incrementAndGet();
                values.forEach((field, fieldValue) -> {
                    String position = titlePostionMap.get(field);

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
            FsApiUtil.addRowColumns(sheetId, spreadsheetToken, "ROWS", rowTotal - rowNum, client);
        }

        Object resp = FsApiUtil.batchPutValues(sheetId, spreadsheetToken, resultValuesBuilder.build(), client);

        fileDataList.forEach(fileData -> {
            try {
                FsApiUtil.imageUpload(fileData.getImageData(), fileData.getFileName(), fileData.getPosition(), fileData.getSheetId(), fileData.getSpreadsheetToken(), client);
            } catch (Exception e) {
                FsLogger.error(ErrorCode.BUSINESS_LOGIC_ERROR, "【飞书表格】 文件上传-文件上传异常! " + fileData.getFileUrl());
            }
        });

        return resp;
    }

    /**
     * 创建飞书表格数据写入构建器
     *
     * 返回一个数据写入构建器实例，支持链式调用和高级配置选项，
     * 如忽略唯一字段等功能。
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList 要写入的数据列表
     * @param <T> 实体类泛型
     * @return WriteBuilder实例，支持链式调用
     */
    public static <T> WriteBuilder<T> writeBuilder(String sheetId, String spreadsheetToken, List<T> dataList) {
        return new WriteBuilder<>(sheetId, spreadsheetToken, dataList);
    }

    /**
     * 将 Map 数据写入飞书表格
     *
     * 直接使用 Map 格式的数据写入飞书表格，无需定义实体类和注解。
     * 适用于动态字段、临时数据写入等场景。
     *
     * 使用示例：
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; dataList = new ArrayList&lt;&gt;();
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("字段名1", "值1");
     * data.put("字段名2", "值2");
     * dataList.add(data);
     *
     * MapTableConfig config = MapTableConfig.builder()
     *     .titleRow(2)
     *     .headLine(3)
     *     .addUniKeyName("字段名1")
     *     .enableCover(true)
     *     .build();
     *
     * FsHelper.writeMap(sheetId, spreadsheetToken, dataList, config);
     * </pre>
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList Map数据列表，每个Map的key为字段名，value为字段值
     * @param config 表格配置，包含标题行、数据起始行、唯一键等配置信息
     * @return 写入操作结果
     */
    public static Object writeMap(String sheetId, String spreadsheetToken,
                                  List<Map<String, Object>> dataList, MapTableConfig config) {
        return new MapWriteBuilder(sheetId, spreadsheetToken, dataList)
                .config(config)
                .build();
    }

    /**
     * 创建 Map 数据写入构建器
     *
     * 返回一个 Map 数据写入构建器实例，支持链式调用和灵活配置。
     * 相比直接使用 writeMap 方法，构建器方式提供了更灵活的配置方式。
     *
     * 使用示例：
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; dataList = new ArrayList&lt;&gt;();
     * Map&lt;String, Object&gt; data = new HashMap&lt;&gt;();
     * data.put("字段名1", "值1");
     * data.put("字段名2", "值2");
     * dataList.add(data);
     *
     * FsHelper.writeMapBuilder(sheetId, spreadsheetToken, dataList)
     *     .titleRow(2)
     *     .headLine(3)
     *     .addUniKeyName("字段名1")
     *     .enableCover(true)
     *     .ignoreNotFound(false)
     *     .build();
     * </pre>
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList 要写入的Map数据列表，每个Map的key为字段名，value为字段值
     * @return MapWriteBuilder实例，支持链式调用
     */
    public static MapWriteBuilder writeMapBuilder(String sheetId, String spreadsheetToken,
                                                  List<Map<String, Object>> dataList) {
        return new MapWriteBuilder(sheetId, spreadsheetToken, dataList);
    }
}