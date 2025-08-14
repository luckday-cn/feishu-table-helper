package cn.isliu;

import cn.isliu.core.BaseEntity;
import cn.isliu.core.FsTableData;
import cn.isliu.core.Sheet;
import cn.isliu.core.config.FsConfig;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomValueService;
import cn.isliu.core.utils.*;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, FsClientUtil.getFeishuClient(), spreadsheetToken);

        // 2 添加表头数据
        FsApiUtil.putValues(spreadsheetToken, FsTableUtil.getHeadTemplateBuilder(sheetId, headers), FsClientUtil.getFeishuClient());

        // 3 设置表格样式
        FsApiUtil.setTableStyle(FsTableUtil.getDefaultTableStyle(sheetId, headers.size()), sheetId, FsClientUtil.getFeishuClient(), spreadsheetToken);

        // 4 设置单元格为文本格式
        if (FsConfig.CELL_TEXT) {
            String column = FsTableUtil.getColumnNameByNuNumber(headers.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, FsClientUtil.getFeishuClient(), spreadsheetToken);
        }

        // 5 设置表格下拉
        FsTableUtil.setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId);
        return sheetId;
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
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, FsClientUtil.getFeishuClient(), spreadsheetToken);
        List<FsTableData> fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken);

        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);
        List<String> fieldPathList = fieldsMap.values().stream().map(FieldProperty::getField).collect(Collectors.toList());

        fsTableDataList.forEach(tableData -> {
            Object data = tableData.getData();
            if (data instanceof HashMap) {
                JsonObject jsonObject = JSONUtil.convertHashMapToJsonObject((HashMap<String, Object>) data);
                Map<String, Object> dataMap = ConvertFieldUtil.convertPositionToField(jsonObject, fieldsMap);
                T t = GenerateUtil.generateInstance(fieldPathList, clazz, dataMap);
                if (t instanceof BaseEntity) {
                    ((BaseEntity) t).setUniqueId(tableData.getUniqueId());
                }
                results.add(t);
            }
        });
        return  results;
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

        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, FsClientUtil.getFeishuClient(), spreadsheetToken);
        List<FsTableData> fsTableDataList = FsTableUtil.getFsTableData(sheet, spreadsheetToken);
        Map<String, Integer> currTableRowMap = fsTableDataList.stream().collect(Collectors.toMap(FsTableData::getUniqueId, FsTableData::getRow));

        final Integer[] row = {0};
        fsTableDataList.forEach(fsTableData -> {
            if (fsTableData.getRow() > row[0]) {
                row[0] = fsTableData.getRow();
            }
        });

        Map<String, String> titlePostionMap = FsTableUtil.getTitlePostionMap(sheet, spreadsheetToken);

        Map<String, String> fieldMap = new HashMap<>();
        fieldsMap.forEach((field, fieldProperty) -> fieldMap.put(field, fieldProperty.getField()));

        // 初始化批量插入对象
        CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder = CustomValueService.ValueRequest.batchPutValues();

        AtomicInteger rowCount = new AtomicInteger(row[0] + 1);

        for (T data : dataList) {
            Map<String, Object> values = GenerateUtil.getFieldValue(data, fieldMap);

            String uniqueId =  GenerateUtil.getUniqueId(data);

            AtomicReference<Integer> rowNum = new AtomicReference<>(currTableRowMap.get(uniqueId));
            if (uniqueId != null && rowNum.get() != null) {
                rowNum.set(rowNum.get() + 1);
                values.forEach((field, fieldValue) -> {
                    if (!FsConfig.isCover && fieldValue == null) {
                        return;
                    }

                    String position = titlePostionMap.get(field);
                    resultValuesBuilder.addRange(sheetId, position + rowNum.get(), position + rowNum.get())
                            .addRow(fieldValue instanceof List ? GenerateUtil.getFieldValueList(fieldValue) : fieldValue);
                });
            } else {
                int rowCou = rowCount.incrementAndGet();
                values.forEach((field, fieldValue) -> {
                    if (!FsConfig.isCover && fieldValue == null) {
                        return;
                    }

                    String position = titlePostionMap.get(field);
                    resultValuesBuilder.addRange(sheetId, position + rowCou, position + rowCou)
                            .addRow(fieldValue instanceof List ? GenerateUtil.getFieldValueList(fieldValue) : fieldValue);

                });
            }
        }

        int rowTotal = sheet.getGridProperties().getRowCount();
        int rowNum = rowCount.get();
        if (rowNum > rowTotal) {
            FsApiUtil.addRowColumns(sheetId, spreadsheetToken, "ROWS", rowTotal - rowNum, FsClientUtil.getFeishuClient());
        }

        return FsApiUtil.batchPutValues(sheetId, spreadsheetToken, resultValuesBuilder.build(), FsClientUtil.getFeishuClient());
    }
}