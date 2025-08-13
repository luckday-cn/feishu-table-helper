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

public class FsHelper {

    public static <T> Boolean create(String sheetName, String spreadsheetToken, Class<T> clazz) {
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
        return true;
    }


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