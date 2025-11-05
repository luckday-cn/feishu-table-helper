package cn.isliu.core.utils;

import cn.isliu.core.*;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.client.FsClient;

import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomCellService;
import cn.isliu.core.service.CustomValueService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 飞书表格工具类
 * 
 * 提供处理飞书表格数据和格式的工具方法，
 * 包括数据处理、样式设置、选项设置等功能
 */
public class FsTableUtil {
    /**
     * 获取飞书表格数据
     *
     * 从指定的工作表中读取并处理表格数据
     *
     * @param sheet 工作表对象
     * @param spreadsheetToken 电子表格Token
     * @return 飞书表格数据列表
     */
    public static List<FsTableData> getFsTableData(Sheet sheet, String spreadsheetToken, TableConf tableConf, Map<String, FieldProperty> fieldsMap) {
        return getFsTableData(sheet, spreadsheetToken, tableConf, new ArrayList<>(), fieldsMap);
    }

    /**
     * 获取飞书表格数据（支持忽略唯一字段）
     *
     * @param sheet 工作表对象
     * @param spreadsheetToken 电子表格Token
     * @param tableConf 表格配置
     * @param ignoreUniqueFields 计算唯一标识时忽略的字段列表
     * @return 飞书表格数据列表
     */
    public static Map<String, List<FsTableData>> getGroupFsTableData(Sheet sheet, String spreadsheetToken, TableConf tableConf, List<String> ignoreUniqueFields, Map<String, FieldProperty> fieldsMap) {
        // 计算数据范围
        List<List<Object>> values = getSourceTableValues(sheet, spreadsheetToken);

        // 获取飞书表格数据
        TableData tableData = processSheetData(sheet, values);

        String[] uniKeys = tableConf.uniKeys();
        Set<String> uniKeyNames = getUniKeyNames(fieldsMap, uniKeys);

        List<FsTableData> dataList = getFsTableData(tableData, ignoreUniqueFields);
        Map<String, String> titleMap = new HashMap<>();
        Map<String, List<String>> categoryPositionMap = new HashMap<>();
        dataList.stream().filter(d -> d.getRow() == (tableConf.titleRow() - 2)).findFirst().ifPresent(d -> {
            Map<String, String> map = (Map<String, String>) d.getData();
            map.forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    categoryPositionMap.computeIfAbsent(v, k1 -> new ArrayList<>()).add(k);
                }
            });
        });

        dataList.stream().filter(d -> d.getRow() == (tableConf.titleRow() - 1)).findFirst()
                .ifPresent(d -> {
                    Map<String, String> map = (Map<String, String>) d.getData();
                    map.forEach((k, v) -> {
                        if (v != null && !v.isEmpty()) {
                            titleMap.put(k + "_" + v, v);
                        } else {
                            titleMap.put(k, v);
                        }
                    });
                });

        List<FsTableData> fsTableDataList = dataList.stream().filter(fsTableData -> fsTableData.getRow() >= (tableConf.headLine() -1)).map(item -> {
            Map<String, Object> resultMap = new HashMap<>();

            Map<String, Object> map = (Map<String, Object>) item.getData();
            map.forEach((k, v) -> {
                titleMap.forEach((k1, v1) -> {
                    if (k1.startsWith(k)) {
                        resultMap.put(k1, v);
                    }
                });
            });
            item.setData(resultMap);
            return item;
        }).collect(Collectors.toList());

        Map<String, List<FsTableData>> dataMap = new HashMap<>();
        categoryPositionMap.forEach((k1, v1) -> {
            List<FsTableData> fsList = new ArrayList<>();

            for (FsTableData fsTableData : fsTableDataList) {
                Map<String, Object> resultMap = new HashMap<>();
                Map<String, String> fieldsPositionMap = new HashMap<>();
                Map<String, Object> data = (Map<String, Object>) fsTableData.getData();
                data.forEach((k, v) -> {
                    if (k != null) {
                        String[] split = k.split("_");
                        if (v1.contains(split[0]) && split.length > 1) {
                            resultMap.put(split[1], v);
                            fieldsPositionMap.put(split[1], split[0]);
                        }
                    }
                });

                if(areAllValuesNullOrBlank(resultMap)) {
                    continue;
                }

                String jsonStr;
                if (!uniKeyNames.isEmpty()) {
                    List<Object> uniKeyValues = new ArrayList<>();
                    for (String key : uniKeyNames) {
                        if (resultMap.containsKey(key)) {
                            uniKeyValues.add(resultMap.get(key));
                        }
                    }
                    jsonStr = StringUtil.listToJson(uniKeyValues);
                } else {
                    if (!ignoreUniqueFields.isEmpty()) {
                        Map<String, Object> clone = new HashMap<>(resultMap);
                        ignoreUniqueFields.forEach(clone::remove);
                        jsonStr = StringUtil.mapToJson(clone);
                    } else {
                        jsonStr = StringUtil.mapToJson(resultMap);
                    }
                }

                FsTableData fsData = new FsTableData();
                fsData.setRow(fsTableData.getRow());
                String uniqueId = StringUtil.getSHA256(jsonStr);
                fsData.setUniqueId(uniqueId);
                fsData.setData(resultMap);
                fsData.setFieldsPositionMap(fieldsPositionMap);
                fsList.add(fsData);
            }
            dataMap.put(k1, fsList);
        });
        return dataMap;
    }

    @NotNull
    private static Set<String> getUniKeyNames(Map<String, FieldProperty> fieldsMap, String[] uniKeys) {
        Set<String> uniKeyNames = new HashSet<>();
        fieldsMap.forEach((k, v) -> {
            String field = v.getField();
            if (field != null && !field.isEmpty()) {
                if (Arrays.asList(uniKeys).contains(field)) {
                    uniKeyNames.add(k);
                }

                if (Arrays.asList(uniKeys).contains(StringUtil.toUnderscoreCase(field))) {
                    uniKeyNames.add(k);
                }
            }
        });
        return uniKeyNames;
    }

    @NotNull
    private static List<List<Object>> getSourceTableValues(Sheet sheet, String spreadsheetToken) {
        GridProperties gridProperties = sheet.getGridProperties();
        int totalRow = gridProperties.getRowCount();
        int rowCount = Math.min(totalRow, 100); // 每次读取的行数
        int colCount = gridProperties.getColumnCount();
        int startOffset = 1; // 起始偏移行号

        // 实际要读取的数据行数（减去偏移量）
        int actualRows = Math.max(0, totalRow - startOffset);
        int batchCount = (actualRows + rowCount - 1) / rowCount;

        List<List<Object>> values = new LinkedList<>();
        for (int i = 0; i < batchCount; i++) {
            int startRowIndex = startOffset + i * rowCount;
            int endRowIndex = Math.min(startRowIndex + rowCount - 1, totalRow - 1);

            // 3. 获取工作表数据
            ValuesBatch valuesBatch = FsApiUtil.getSheetData(sheet.getSheetId(), spreadsheetToken,
                    "A" + startRowIndex,
                    getColumnName(colCount - 1) + endRowIndex, FsClient.getInstance().getClient());
            if (valuesBatch != null) {
                List<ValueRange> valueRanges = valuesBatch.getValueRanges();
                for (ValueRange valueRange : valueRanges) {
                    values.addAll(valueRange.getValues());
                }
            }
        }
        return values;
    }

    /**
     * 获取飞书表格数据（支持忽略唯一字段）
     *
     * @param sheet 工作表对象
     * @param spreadsheetToken 电子表格Token
     * @param tableConf 表格配置
     * @param ignoreUniqueFields 计算唯一标识时忽略的字段列表
     * @return 飞书表格数据列表
     */
    public static List<FsTableData> getFsTableData(Sheet sheet, String spreadsheetToken, TableConf tableConf, List<String> ignoreUniqueFields, Map<String, FieldProperty> fieldsMap) {

        // 计算数据范围
        List<List<Object>> values = getSourceTableValues(sheet, spreadsheetToken);

        // 获取飞书表格数据
        TableData tableData = processSheetData(sheet, values);

        String[] uniKeys = tableConf.uniKeys();
        Set<String> uniKeyNames = getUniKeyNames(fieldsMap, uniKeys);
        List<FsTableData> dataList = getFsTableData(tableData, ignoreUniqueFields);
        Map<String, String> titleMap = new HashMap<>();
        Map<String, String> fieldsPositionMap = new HashMap<>();

        dataList.stream().filter(d -> d.getRow() == (tableConf.titleRow() - 1)).findFirst()
                .ifPresent(d -> {
                    Map<String, String> map = (Map<String, String>) d.getData();
                    map.forEach((k, v) -> {
                        if (v != null && !v.isEmpty()) {
                            fieldsPositionMap.put(v, k);
                        };
                    });
                    titleMap.putAll(map);
                });
        return dataList.stream().filter(fsTableData -> fsTableData.getRow() >= tableConf.headLine()).map(item -> {
            Map<String, Object> resultMap = new HashMap<>();

            Map<String, Object> map = (Map<String, Object>) item.getData();
            map.forEach((k, v) -> {
                String title = titleMap.get(k);
                if (title != null) {
                    resultMap.put(title, v);
                }
            });
            item.setData(resultMap);
            item.setFieldsPositionMap(fieldsPositionMap);

            String jsonStr = null;
            if (!uniKeyNames.isEmpty()) {
                List<Object> uniKeyValues = new ArrayList<>();
                for (String key : uniKeyNames) {
                    if (resultMap.containsKey(key)) {
                        uniKeyValues.add(resultMap.get(key));
                    }
                }
                jsonStr = StringUtil.listToJson(uniKeyValues);
            }

            if (jsonStr != null) {
                String uniqueId = StringUtil.getSHA256(jsonStr);
                item.setUniqueId(uniqueId);
            }

            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取飞书表格数据
     *
     * @param tableData 表格数据对象
     * @return 飞书表格数据列表
     */
    public static List<FsTableData> getFsTableData(TableData tableData) {
        return getFsTableData(tableData, new ArrayList<>());
    }

    /**
     * 获取飞书表格数据
     *
     * @param tableData 表格数据对象
     * @param ignoreUniqueFields 忽略的唯一字段列表
     * @return 飞书表格数据列表
     */
    public static List<FsTableData> getFsTableData(TableData tableData, List<String> ignoreUniqueFields) {

        List<FsTableData> fsTableList = new LinkedList<>();
        // 5. 访问补齐后的数据
        for (TableRow row : tableData.getRows()) {

            FsTableData fsTableData = new FsTableData();
            int rowIndex = 0;
            Map<String, Object> obj = new HashMap<>();
            for (Cell cell : row.getCells()) {
                obj.put(getColumnName(cell.getCol()), cell.getValue());
                rowIndex = cell.getRow();
            }

            fsTableData.setRow(rowIndex);
            fsTableData.setData(obj);

            String jsonStr;
            if (!ignoreUniqueFields.isEmpty()) {
                Map<String, Object> clone = new HashMap<>(obj);
                ignoreUniqueFields.forEach(clone::remove);
                jsonStr = StringUtil.mapToJson(clone);
            } else {
                jsonStr = StringUtil.mapToJson(obj);
            }

            String uniqueId = StringUtil.getSHA256(jsonStr);

            fsTableData.setUniqueId(uniqueId);
            fsTableList.add(fsTableData);
        }
        return fsTableList;

    }

    /**
     * 处理表格数据，将合并单元格转换为对象，并补齐合并区域的值
     */
    public static TableData processSheetData(Sheet metadata, List<List<Object>> values) {
        TableData tableData = new TableData();

        // 创建单元格网格
        int rowCount = values.size();
        int colCount = values.stream().mapToInt(List::size).max().orElse(0);
        Cell[][] grid = new Cell[rowCount][colCount];

        // 1. 初始化网格
        for (int i = 0; i < rowCount; i++) {
            List<Object> row = values.get(i);
            for (int j = 0; j < colCount; j++) {
                Object value = (j < row.size()) ? row.get(j) : null;
                grid[i][j] = new Cell(i, j, value);
            }
        }

        // 2. 标记合并区域并补齐所有合并单元格的值
        if (metadata.getMerges() != null) {
            for (Merge merge : metadata.getMerges()) {
                int startRow = merge.getStartRowIndex();
                int endRow = merge.getEndRowIndex();
                int startCol = merge.getStartColumnIndex();
                int endCol = merge.getEndColumnIndex();

                // 获取合并区域左上角的值
                Object topLeftValue = null;
                if (startRow < rowCount && startCol < colCount) {
                    topLeftValue = grid[startRow][startCol].getValue();
                }

                // 遍历合并区域
                for (int i = startRow; i <= endRow; i++) {
                    for (int j = startCol; j <= endCol; j++) {
                        if (i < rowCount && j < colCount) {
                            // 标记合并区域
                            grid[i][j].setMerge(merge);

                            // 对于合并区域内除左上角外的所有单元格
                            if (i != startRow || j != startCol) {
                                // 补齐值
                                grid[i][j].setValue(topLeftValue);
                            }
                        }
                    }
                }
            }
        }

        // 3. 构建表格数据结构
        for (int i = 0; i < rowCount; i++) {
            // 检查整行是否都为null
            boolean allNull = true;
            for (int j = 0; j < colCount; j++) {
                if (grid[i][j].getValue() != null) {
                    allNull = false;
                    break;
                }
            }

            // 如果整行都为null，跳过该行
            if (allNull) {
                continue;
            }

            TableRow tableRow = new TableRow();

            for (int j = 0; j < colCount; j++) {
                Cell cell = grid[i][j];

                // 如果是合并区域的左上角
                if (cell.getMerge() != null &&
                        cell.getRow() == cell.getMerge().getStartRowIndex() &&
                        cell.getCol() == cell.getMerge().getStartColumnIndex()) {

                    MergedCell mergedCell = new MergedCell();
                    mergedCell.setValue(cell.getValue());
                    mergedCell.setRow(cell.getRow());
                    mergedCell.setCol(cell.getCol());
                    mergedCell.setRowSpan(cell.getMerge().getRowSpan());
                    mergedCell.setColSpan(cell.getMerge().getColSpan());

                    tableRow.getCells().add(mergedCell);
                } else {
                    // 普通单元格或合并区域内的其他单元格
                    tableRow.getCells().add(cell);
                }
            }

            tableData.getRows().add(tableRow);
        }

        return tableData;
    }


    public static String getColumnName(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        while (columnIndex >= 0) {
            char c = (char) ('A' + (columnIndex % 26));
            sb.insert(0, c);
            columnIndex = (columnIndex / 26) - 1;
            if (columnIndex < 0) break;
        }
        return sb.toString();
    }

    public static String getColumnNameByNuNumber(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        while (columnNumber > 0) {
            int remainder = (columnNumber - 1) % 26;
            columnName.insert(0, (char) ('A' + remainder));
            columnNumber = (columnNumber - 1) / 26;
        }
        return columnName.toString();
    }

    public static Map<String, String> getTitlePostionMap(Sheet sheet, String spreadsheetToken, TableConf tableConf) {
        GridProperties gridProperties = sheet.getGridProperties();
        int colCount = gridProperties.getColumnCount();

        Map<String, String> resultMap = new TreeMap<>();
        ValuesBatch valuesBatch = FsApiUtil.getSheetData(sheet.getSheetId(), spreadsheetToken,
                "A" + tableConf.titleRow(),
                getColumnName(colCount - 1) + tableConf.titleRow(), FsClient.getInstance().getClient());
        if (valuesBatch != null) {
            List<ValueRange> valueRanges = valuesBatch.getValueRanges();
            if (valueRanges != null && !valueRanges.isEmpty()) {
                List<Object> values = valueRanges.get(0).getValues().get(0);
                if (values != null && !values.isEmpty()) {
                    for (int i = 0; i < values.size(); i++) {
                        Object valObj = values.get(i);
                        if (valObj == null) {
                            continue;
                        }
                        String value = (String) valObj;
                        resultMap.put(value.trim(), getColumnName(i));
                    }
                }
            }
        }

        return resultMap;
    }

    public static void setTableOptions(String spreadsheetToken, String sheetId,  Class<?> clazz) {
        setTableOptions(spreadsheetToken, sheetId, clazz, null, null);
    }

    public static void setTableOptions(String spreadsheetToken, String sheetId,  Class<?> clazz,
                                       Map<String, Object> customProperties, List<String> includeFields) {
        Map<String, FieldProperty> fieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);
        Map<String, FieldProperty> currFieldsMap;
        if (includeFields != null && !includeFields.isEmpty()) {
            currFieldsMap = fieldsMap.entrySet().stream()
                    .filter(entry -> {
                        String field = entry.getValue().getField();
                        field = field.substring(field.lastIndexOf(".") + 1);
                        if (field.isEmpty()) {
                            return false;
                        }
                        return includeFields.contains(StringUtil.toUnderscoreCase(field));
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            currFieldsMap = fieldsMap;
        }

        List<String> headers = PropertyUtil.getHeaders(currFieldsMap, includeFields);

        TableConf tableConf = PropertyUtil.getTableConf(clazz);
        setTableOptions(spreadsheetToken, headers, currFieldsMap, sheetId, tableConf.enableDesc(), customProperties);
    }

    public static void setTableOptions(String spreadsheetToken, List<String> headers, Map<String, FieldProperty> fieldsMap,
                                       String sheetId, boolean enableDesc, Map<String, Object> customProperties) {
        // 读取sheet数据，查询有现有行数（超出会报错）
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, FsClient.getInstance().getClient(), spreadsheetToken);
        int rowCount = sheet.getGridProperties().getRowCount();

        List<Object> list = Arrays.asList(headers.toArray());
        int line = getMaxLevel(fieldsMap) + (enableDesc ? 2 : 1);
        fieldsMap.forEach((field, fieldProperty) -> {
            TableProperty tableProperty = fieldProperty.getTableProperty();
            String position = "";
            if (tableProperty != null) {
                for (int i = 0; i < list.size(); i++) {
                    Object obj = list.get(i);
                    if (obj.toString().equals(field)) {
                        position = FsTableUtil.getColumnNameByNuNumber(i + 1);
                    }
                }

                if (tableProperty.enumClass() != BaseEnum.class) {
                    FsApiUtil.setOptions(sheetId, FsClient.getInstance().getClient(), spreadsheetToken, tableProperty.type() == TypeEnum.MULTI_SELECT, position + line, position + 200,
                            Arrays.stream(tableProperty.enumClass().getEnumConstants()).map(BaseEnum::getDesc).collect(Collectors.toList()));
                }

                if (tableProperty.optionsClass() != OptionsValueProcess.class) {
                    List<String> result;
                    Class<? extends OptionsValueProcess> optionsClass = tableProperty.optionsClass();
                    try {
                        Map<String, Object> properties = new HashMap<>();
                        if (customProperties == null) {
                            properties.put("_field", fieldProperty);
                        } else {
                            customProperties.put("_field", fieldProperty);
                        }
                        OptionsValueProcess optionsValueProcess = optionsClass.getDeclaredConstructor().newInstance();
                        result = (List<String>) optionsValueProcess.process(customProperties == null ? properties : customProperties);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }

                    if (result != null && !result.isEmpty()) {
                        FsApiUtil.setOptions(sheetId, FsClient.getInstance().getClient(), spreadsheetToken,
                                tableProperty.type() == TypeEnum.MULTI_SELECT,
                                position + line, position + rowCount, result);
                    }
                }
            }
        });
    }

    public static void setTableOptions(String spreadsheetToken, String[] headers, Map<String, FieldProperty> fieldsMap,
                                       String sheetId, boolean enableDesc, Map<String, Object> customProperties) {

        int line = getMaxLevel(fieldsMap) + (enableDesc ? 3 : 2);
        fieldsMap.forEach((field, fieldProperty) -> {
            TableProperty tableProperty = fieldProperty.getTableProperty();
            if (tableProperty != null) {
                List<String> positions = new ArrayList<>();
                for (String obj : headers) {
                    if (obj != null && obj.startsWith(field)) {
                        String[] split = obj.split("_");
                        if (split.length > 1) {
                            positions.add(split[1]);
                        }
                    }
                }

                if (!positions.isEmpty()) {
                    positions.forEach(position -> {
                        if (tableProperty.enumClass() != BaseEnum.class) {
                            FsApiUtil.setOptions(sheetId, FsClient.getInstance().getClient(), spreadsheetToken, tableProperty.type() == TypeEnum.MULTI_SELECT, position + line, position + 200,
                                    Arrays.stream(tableProperty.enumClass().getEnumConstants()).map(BaseEnum::getDesc).collect(Collectors.toList()));
                        }

                        if (tableProperty.optionsClass() != OptionsValueProcess.class) {
                            List<String> result;
                            Class<? extends OptionsValueProcess> optionsClass = tableProperty.optionsClass();
                            try {
                                Map<String, Object> properties = new HashMap<>();
                                if (customProperties == null) {
                                    properties.put("_field", fieldProperty);
                                } else {
                                    customProperties.put("_field", fieldProperty);
                                }
                                OptionsValueProcess optionsValueProcess = optionsClass.getDeclaredConstructor().newInstance();
                                result = (List<String>) optionsValueProcess.process(customProperties == null ? properties : customProperties);
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }

                            if (result != null && !result.isEmpty()) {
                                FsApiUtil.setOptions(sheetId, FsClient.getInstance().getClient(), spreadsheetToken, tableProperty.type() == TypeEnum.MULTI_SELECT, position + line, position + 200,
                                        result);
                            }
                        }
                    });
                }
            }
        });
    }

    public static void setTableOptions(String spreadsheetToken, List<String> headers, Map<String, FieldProperty> fieldsMap, String sheetId, boolean enableDesc) {
        setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId, enableDesc, null);
    }

    public static CustomValueService.ValueRequest getHeadTemplateBuilder(String sheetId, List<String> headers,
                                                                         Map<String, FieldProperty> fieldsMap, TableConf tableConf) {
        return getHeadTemplateBuilder(sheetId, headers, fieldsMap, tableConf, null);
    }

    public static CustomValueService.ValueRequest getHeadTemplateBuilder(String sheetId, List<String> headers, List<String> headerList, Map<String, FieldProperty> fieldsMap,
                                                                         TableConf tableConf, Map<String, String> fieldDescriptions, List<String> groupFields) {
        String position = FsTableUtil.getColumnNameByNuNumber(headerList.size());

        CustomValueService.ValueRequest.BatchPutValuesBuilder batchPutValuesBuilder
                = CustomValueService.ValueRequest.batchPutValues();

        int row = tableConf.titleRow();
        if (tableConf.enableDesc()) {
            batchPutValuesBuilder.addRange(sheetId + "!A" + (row - 1) + ":" + position + (row + 1));
            batchPutValuesBuilder.addRow(getGroupArray(headers, headerList.size(), groupFields));
            batchPutValuesBuilder.addRow(headerList.toArray());
            batchPutValuesBuilder.addRow(getDescArray(headerList, fieldsMap, fieldDescriptions));
        } else {
            batchPutValuesBuilder.addRange(sheetId + "!A" + (row - 1) + ":" + position + row);
            batchPutValuesBuilder.addRow(getGroupArray(headers, headerList.size(), groupFields));
            batchPutValuesBuilder.addRow(headerList.toArray());
        }
        return batchPutValuesBuilder.build();
    }

    private static Object[] getGroupArray(List<String> headers, int size, List<String> groupFields) {
        Object[] groupArray = new Object[size];
        int index = 0;

        for (int i = 0; i < groupFields.size(); i++) {
            if (i > 0) {
                // 在非第一个groupField前添加null分隔符
                groupArray[index++] = null;
            }

            String groupField = groupFields.get(i);
            // 为当前groupField填充headers长度的数据
            for (int j = 0; j < headers.size(); j++) {
                groupArray[index++] = groupField;
            }
        }

        return groupArray;
    }

    /**
     * 根据headers和groupFields生成带表格列标识的数据
     *
     * @param headers 表头列表
     * @param groupFields 分组字段列表
     * @return 带表格列标识的数据数组
     */
    public static String[] generateHeaderWithColumnIdentifiers(List<String> headers, List<String> groupFields) {
        // 计算结果数组大小
        // 每个groupField需要headers.size()个位置，加上(groupFields.size()-1)个null分隔符
        int size = groupFields.size() * headers.size() + (groupFields.size() - 1);
        String[] result = new String[size];

        int index = 0;

        for (int i = 0; i < groupFields.size(); i++) {
            // 在非第一个groupField前添加null分隔符
            if (i > 0) {
                result[index++] = null;
            }

            // 为当前groupField填充headers长度的数据
            for (int j = 0; j < headers.size(); j++) {
                // 获取对应的列标识（A, B, C, ...）
                String columnIdentifier = getColumnName(index);
                // 拼接header和列标识
                result[index++] = headers.get(j) + "_" + columnIdentifier;
            }
        }

        return result;
    }


    public static CustomValueService.ValueRequest getHeadTemplateBuilder(String sheetId, List<String> headers,
                                                                         Map<String, FieldProperty> fieldsMap, TableConf tableConf, Map<String, String> fieldDescriptions) {

        String position = FsTableUtil.getColumnNameByNuNumber(headers.size());

        CustomValueService.ValueRequest.BatchPutValuesBuilder batchPutValuesBuilder
                = CustomValueService.ValueRequest.batchPutValues();

        // 获取父级表头
        int maxLevel = getMaxLevel(fieldsMap);

        if (maxLevel == 1) {
            int titleRow = tableConf.titleRow();
            if (tableConf.enableDesc()) {
                int descRow = titleRow + 1;
                batchPutValuesBuilder.addRange(sheetId + "!A" + titleRow + ":" + position + descRow);
                batchPutValuesBuilder.addRow(headers.toArray());
                batchPutValuesBuilder.addRow(getDescArray(headers, fieldsMap, fieldDescriptions));
            } else {
                batchPutValuesBuilder.addRange(sheetId + "!A" + titleRow + ":" + position + titleRow);
                batchPutValuesBuilder.addRow(headers.toArray());
            }
        } else {

            // 多层级表头：构建层级结构并处理合并单元格
            List<List<HeaderCell>> hierarchicalHeaders = buildHierarchicalHeaders(fieldsMap);

            // 处理每一行表头
            for (int rowIndex = 0; rowIndex < hierarchicalHeaders.size(); rowIndex++) {
                List<HeaderCell> headerRow = hierarchicalHeaders.get(rowIndex);
                List<Object> rowValues = new ArrayList<>();

                // 将HeaderCell转换为字符串值，并处理合并单元格
                for (HeaderCell cell : headerRow) {
                    rowValues.add(cell.getValue());
                    // 对于合并单元格，添加空值占位符
                    for (int span = 1; span < cell.getColSpan(); span++) {
                        rowValues.add(""); // 合并单元格的占位符
                    }
                }

                int actualRow = rowIndex + 1; // 从第1行开始
                batchPutValuesBuilder.addRange(sheetId + "!A" + actualRow + ":" + position + actualRow);
                batchPutValuesBuilder.addRow(rowValues.toArray());
            }

            // 如果启用了描述，在最后一行添加描述
            if (tableConf.enableDesc()) {
                int descRow = maxLevel + 1;
                batchPutValuesBuilder.addRange(sheetId + "!A" + descRow + ":" + position + descRow);
                batchPutValuesBuilder.addRow(getDescArray(headers, fieldsMap, fieldDescriptions));
            }
        }

        return batchPutValuesBuilder.build();
    }

    private static int getMaxLevel(Map<String, FieldProperty> fieldsMap) {
        AtomicInteger maxLevel = new AtomicInteger(1);
        fieldsMap.forEach((field, fieldProperty) -> {
            TableProperty tableProperty = fieldProperty.getTableProperty();
            String[] value = tableProperty.value();
            if (value.length > maxLevel.get()) {
                maxLevel.set(value.length);
            }
        });
        return maxLevel.get();
    }

    private static Object[] getDescArray(List<String> headers, Map<String, FieldProperty> fieldsMap) {
        return getDescArray(headers, fieldsMap, null);
    }

    private static Object[] getDescArray(List<String> headers, Map<String, FieldProperty> fieldsMap, Map<String, String> fieldDescriptions) {
        Object[] descArray = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            FieldProperty fieldProperty = fieldsMap.get(header);
            if (fieldProperty != null && fieldProperty.getTableProperty() != null) {
                String desc = null;

                // 优先从字段描述映射中获取描述
                if (fieldDescriptions != null && !fieldDescriptions.isEmpty()) {
                    // 从字段路径中提取字段名（最后一个.后面的部分）
                    String fieldPath = fieldProperty.getField();
                    String fieldName = fieldPath.substring(fieldPath.lastIndexOf(".") + 1);
                    desc = fieldDescriptions.get(fieldName);
                    if (desc == null) {
                        desc = fieldDescriptions.get(StringUtil.toUnderscoreCase(fieldName));
                    }
                }

                // 如果映射中没有找到，则从注解中获取
                if (desc == null || desc.isEmpty()) {
                    desc = fieldProperty.getTableProperty().desc();
                }

                if (desc != null && !desc.isEmpty()) {
                    try {
                        JsonElement element = JsonParser.parseString(desc);
                        if (element.isJsonObject()) {
                            descArray[i] = element.getAsJsonObject();
                        } else if (element.isJsonArray()) {
                            descArray[i] = element.getAsJsonArray();
                        } else {
//                            desc = addLineBreaksPer8Chars(desc);
                            descArray[i] = desc;
                        }
                    } catch (JsonSyntaxException e) {
                        descArray[i] = desc;
                    }
                } else {
                    descArray[i] = null;
                }
            } else {
                descArray[i] = null;
            }
        }
        return descArray;
    }

    public static String getDefaultTableStyle(String sheetId, int size, Map<String, FieldProperty> fieldsMap, TableConf tableConf) {
        int maxLevel = getMaxLevel(fieldsMap);
        String colorTemplate = "{\"data\": [{\"style\": {\"font\": {\"bold\": true, \"clean\": false, \"italic\": false, \"fontSize\": \"10pt/1.5\"}, \"clean\": false, \"hAlign\": 1, \"vAlign\": 1, \"backColor\": \"#000000\", \"foreColor\": \"#ffffff\", \"formatter\": \"\", \"borderType\": \"FULL_BORDER\", \"borderColor\": \"#000000\", \"textDecoration\": 0}, \"ranges\": [\"SHEET_ID!RANG\"]}]}";
        colorTemplate = colorTemplate.replace("SHEET_ID", sheetId);
        colorTemplate = colorTemplate.replace("RANG", "A1:" + FsTableUtil.getColumnNameByNuNumber(size) + maxLevel);
        colorTemplate = colorTemplate.replace("FORE_COLOR", tableConf.headFontColor())
                .replace("BACK_COLOR", tableConf.headBackColor());
        return colorTemplate;
    }

    public static CustomCellService.StyleCellsBatchBuilder getDefaultTableStyle(String sheetId, Map<String, FieldProperty> fieldsMap, TableConf tableConf) {
        int maxLevel = getMaxLevel(fieldsMap);
        CustomCellService.StyleCellsBatchBuilder styleCellsBatchBuilder = CustomCellService.CellRequest.styleCellsBatch()
                .addRange(sheetId, "A1", FsTableUtil.getColumnNameByNuNumber(fieldsMap.size()) + maxLevel)
                .backColor(tableConf.headBackColor())
                .foreColor(tableConf.headFontColor());

        return styleCellsBatchBuilder;
    }

    public static CustomCellService.StyleCellsBatchBuilder getDefaultTableStyle(String sheetId, String[] position, TableConf tableConf) {
        CustomCellService.StyleCellsBatchBuilder styleCellsBatchBuilder = CustomCellService.CellRequest.styleCellsBatch()
                .addRange(sheetId, position[0] + 1, position[1] + 2)
                .backColor(tableConf.headBackColor())
                .foreColor(tableConf.headFontColor());

        return styleCellsBatchBuilder;
    }

    public static CustomCellService.StyleCellsBatchBuilder getDefaultTableStyle(String sheetId, String[] position,
                  String backColor, String foreColor) {

        CustomCellService.StyleCellsBatchBuilder styleCellsBatchBuilder = CustomCellService.CellRequest.styleCellsBatch()
                .addRange(sheetId, position[0], position[1])
                .backColor(backColor)
                .foreColor(foreColor);
        return styleCellsBatchBuilder;
    }

    public static Map<String, String[]> calculateGroupPositions(List<String> headers, List<String> groupFields) {
        Map<String, String[]> positions = new HashMap<>();
        int index = 0;

        for (int i = 0; i < groupFields.size(); i++) {
            String groupField = groupFields.get(i);
            // 计算开始位置
            String startPosition = getColumnName(index);
            // 计算结束位置
            index += headers.size() - 1;
            String endPosition = getColumnName(index);

            positions.put(groupField, new String[]{startPosition, endPosition});

            // 如果不是最后一个groupField，跳过null分隔符
            if (i < groupFields.size() - 1) {
                index += 2; // 跳过当前末尾位置和null分隔符
            } else {
                index += 1; // 只跳过当前末尾位置
            }
        }

        return positions;
    }

    public static List<String> getGroupHeaders(List<String> groupFieldList, List<String> headers) {
        List<String> headerList = new ArrayList<>();

        groupFieldList.forEach(groupField -> {
            if (!headerList.isEmpty()) {
                headerList.add(null);
            }
            headerList.addAll(headers);
        });
        return headerList;
    }


    /**
     * 根据层级分组字段属性，并按order排序
     *
     * @param fieldsMap 字段属性映射
     * @return 按层级分组的映射，key为层级，value为该层级的字段名数组（已按order排序）
     */
    public static Map<Integer, List<String>> groupFieldsByLevel(Map<String, FieldProperty> fieldsMap) {
        Map<Integer, List<String>> levelMap = new HashMap<>();

        // 按order排序的字段条目
        List<Map.Entry<String, FieldProperty>> sortedEntries = fieldsMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getTableProperty() != null)
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getTableProperty().order()))
                .collect(Collectors.toList());

        for (Map.Entry<String, FieldProperty> entry : sortedEntries) {
            FieldProperty fieldProperty = entry.getValue();
            String[] values = fieldProperty.getTableProperty().value();
            for (int i = 0; i < values.length; i++) {
                levelMap.computeIfAbsent(i, k -> new ArrayList<>()).add(values[i]);
            }
        }

        return levelMap;
    }

    /**
     * 构建多层级表头结构，支持按层级排序和合并
     * 根据需求实现层级分组和order排序：
     * 1. 按全局order排序，但确保同一分组的字段相邻
     * 2. 同一分组内的字段能够正确合并
     *
     * @param fieldsMap 字段属性映射
     * @return 多层级表头结构，外层为行，内层为列
     */
    public static List<List<HeaderCell>> buildHierarchicalHeaders(Map<String, FieldProperty> fieldsMap) {
        int maxLevel = getMaxLevel(fieldsMap);
        List<List<HeaderCell>> headerRows = new ArrayList<>();

        // 初始化每行的表头列表
        for (int i = 0; i < maxLevel; i++) {
            headerRows.add(new ArrayList<>());
        }

        // 获取排序后的字段列表，按照特殊规则排序：
        // 1. 相同第一层级的字段必须相邻
        // 2. 在满足条件1的情况下，尽可能按order排序
        List<Map.Entry<String, FieldProperty>> sortedFields = getSortedFieldsWithGrouping(fieldsMap);

        // 按排序后的顺序处理每个字段
        for (Map.Entry<String, FieldProperty> entry : sortedFields) {
            String[] values = entry.getValue().getTableProperty().value();

            // 统一处理：所有字段都对齐到maxLevel层级
            // 核心思路：最后一个值总是出现在最后一行，前面的值依次向上排列

            for (int level = 0; level < maxLevel; level++) {
                List<HeaderCell> currentRow = headerRows.get(level);
                HeaderCell headerCell = new HeaderCell();
                headerCell.setLevel(level);
                headerCell.setColSpan(1);

                // 计算当前层级应该显示的值
                String currentValue = "";

                if (values.length == 1) {
                    // 单层级字段：只在最后一行显示
                    if (level == maxLevel - 1) {
                        currentValue = values[0];
                    }
                } else {
                    // 多层级字段：需要对齐到maxLevel
                    // 计算从当前层级到值数组的映射
                    int valueIndex = level - (maxLevel - values.length);
                    if (valueIndex >= 0 && valueIndex < values.length) {
                        currentValue = values[valueIndex];
                    }
                }

                headerCell.setValue(currentValue);
                currentRow.add(headerCell);
            }
        }

        return headerRows;
    }

    /**
     * 获取排序后的字段列表，基于最子级字段排序的新规则
     * 核心规则：
     * 1. 根据最子级字段的order进行主排序
     * 2. 相同父级字段形成分组，组内按子级order排序
     * 3. 分组按组内最小order值参与全局排序
     * 4. 三级及以上层级遵循约定大于配置，要求order连续
     *
     * @param fieldsMap 字段属性映射
     * @return 排序后的字段列表
     */
    private static List<Map.Entry<String, FieldProperty>> getSortedFieldsWithGrouping(Map<String, FieldProperty> fieldsMap) {
        int maxLevel = getMaxLevel(fieldsMap);

        // 统一的分组排序逻辑，适用于所有层级
        // 按层级路径分组
        Map<String, List<Map.Entry<String, FieldProperty>>> groupedFields = groupFieldsByFirstLevel(fieldsMap);

        // 创建分组信息列表
        List<GroupInfo> allGroups = new ArrayList<>();

        for (Map.Entry<String, List<Map.Entry<String, FieldProperty>>> groupEntry : groupedFields.entrySet()) {
            List<Map.Entry<String, FieldProperty>> fieldsInGroup = groupEntry.getValue();

            // 在组内按order排序（基于最子级字段）
            fieldsInGroup.sort(Comparator.comparingInt(entry -> entry.getValue().getTableProperty().order()));

            // 验证组内order连续性（仅对需要合并的分组进行检查，且仅在三级及以上时检查）
            if (maxLevel >= 3 && fieldsInGroup.size() > 1 && !"default".equals(groupEntry.getKey())) {
                validateOrderContinuity(groupEntry.getKey(), fieldsInGroup);
            }

            // 计算组的最小order（用于组间排序）
            int minOrder = fieldsInGroup.stream()
                    .mapToInt(entry -> entry.getValue().getTableProperty().order())
                    .min()
                    .orElse(Integer.MAX_VALUE);

            allGroups.add(new GroupInfo(groupEntry.getKey(), minOrder, fieldsInGroup));
        }

        // 新的排序逻辑：分组作为整体参与全局order排序
        // 创建排序单元列表（每个单元可能是单个字段或一个分组）
        List<SortUnit> sortUnits = new ArrayList<>();

        for (GroupInfo group : allGroups) {
            if ("default".equals(group.getGroupKey())) {
                // 单层级字段：每个字段都是独立的排序单元
                for (Map.Entry<String, FieldProperty> field : group.getFields()) {
                    int order = field.getValue().getTableProperty().order();
                    sortUnits.add(new SortUnit(order, Arrays.asList(field), false));
                }
            } else {
                // 多层级分组：整个分组作为一个排序单元，使用最小order
                sortUnits.add(new SortUnit(group.getMinOrder(), group.getFields(), true));
            }
        }

        // 按order排序所有排序单元（实现真正的全局排序）
        sortUnits.sort(Comparator.comparingInt(SortUnit::getOrder));

        // 展开为字段列表
        List<Map.Entry<String, FieldProperty>> result = new ArrayList<>();
        for (SortUnit unit : sortUnits) {
            result.addAll(unit.getFields());
        }

        return result;
    }

    //
    public static List<CustomCellService.CellRequest> getMergeCell(String sheetId, Collection<String[]> positions) {
        List<CustomCellService.CellRequest> mergeRequests = new ArrayList<>();
        for (String[] position : positions) {
            if (position.length == 2) {
                CustomCellService.CellRequest mergeRequest = CustomCellService.CellRequest.mergeCells()
                        .sheetId(sheetId)
                        .startPosition(position[0] + 1)
                        .endPosition(position[1] + 1)
                        .build();
                mergeRequests.add(mergeRequest);
            }
        }
        return mergeRequests;
    }

    public static List<CustomCellService.CellRequest> getMergeCell(String sheetId, Map<String, FieldProperty> fieldsMap) {
        List<CustomCellService.CellRequest> mergeRequests = new ArrayList<>();

        // 构建层级表头结构
        List<List<HeaderCell>> headerRows = buildHierarchicalHeaders(fieldsMap);

        // 遍历每一行，查找需要合并的单元格
        for (int rowIndex = 0; rowIndex < headerRows.size(); rowIndex++) {
            List<HeaderCell> headerRow = headerRows.get(rowIndex);

            // 查找连续的相同值区域
            int colIndex = 0;
            for (int i = 0; i < headerRow.size(); i++) {
                HeaderCell currentCell = headerRow.get(i);
                String currentValue = currentCell.getValue();

                // 跳过空值，空值不需要合并
                if (currentValue == null || currentValue.trim().isEmpty()) {
                    colIndex++;
                    continue;
                }

                // 查找相同值的连续区域
                int startCol = colIndex;
                int endCol = startCol;

                // 向后查找相同值
                for (int j = i + 1; j < headerRow.size(); j++) {
                    HeaderCell nextCell = headerRow.get(j);
                    if (currentValue.equals(nextCell.getValue())) {
                        endCol++;
                        i++; // 跳过已经处理的单元格
                    } else {
                        break;
                    }
                }

                // 如果跨越多列，则需要合并
                if (endCol > startCol) {
                    String startPosition = getColumnName(startCol) + (rowIndex + 1);
                    String endPosition = getColumnName(endCol) + (rowIndex + 1);

                    CustomCellService.CellRequest mergeRequest = CustomCellService.CellRequest.mergeCells()
                            .sheetId(sheetId)
                            .startPosition(startPosition)
                            .endPosition(endPosition)
                            .build();

                    mergeRequests.add(mergeRequest);
                }

                colIndex = endCol + 1;
            }
        }

        return mergeRequests;
    }

    public static TableConf getTableConf(Class<?> zClass) {
        return zClass.getAnnotation(TableConf.class);
    }

    /**
     * 分组信息类，用于辅助排序
     */
    private static class GroupInfo {
        private final String groupKey;
        private final int minOrder;
        private final List<Map.Entry<String, FieldProperty>> fields;
        private final int groupDepth;

        public GroupInfo(String groupKey, int minOrder, List<Map.Entry<String, FieldProperty>> fields) {
            this(groupKey, minOrder, fields, 1);
        }

        public GroupInfo(String groupKey, int minOrder, List<Map.Entry<String, FieldProperty>> fields, int groupDepth) {
            this.groupKey = groupKey;
            this.minOrder = minOrder;
            this.fields = fields;
            this.groupDepth = groupDepth;
        }

        public String getGroupKey() { return groupKey; }
        public int getMinOrder() { return minOrder; }
        public List<Map.Entry<String, FieldProperty>> getFields() { return fields; }
        public int getGroupDepth() { return groupDepth; }
    }

    /**
     * 排序项类，用于全局排序
     */
    private static class SortItem {
        private final int order;
        private final List<Map.Entry<String, FieldProperty>> fields;
        private final boolean isGroup;

        public SortItem(int order, List<Map.Entry<String, FieldProperty>> fields, boolean isGroup) {
            this.order = order;
            this.fields = fields;
            this.isGroup = isGroup;
        }

        public int getOrder() { return order; }
        public List<Map.Entry<String, FieldProperty>> getFields() { return fields; }
        public boolean isGroup() { return isGroup; }
    }

    /**
     * 排序单元类，用于分组整体排序
     * 一个排序单元可以是单个字段或一个完整的分组
     */
    private static class SortUnit {
        private final int order;
        private final List<Map.Entry<String, FieldProperty>> fields;
        private final boolean isGroup;

        public SortUnit(int order, List<Map.Entry<String, FieldProperty>> fields, boolean isGroup) {
            this.order = order;
            this.fields = fields;
            this.isGroup = isGroup;
        }

        public int getOrder() { return order; }
        public List<Map.Entry<String, FieldProperty>> getFields() { return fields; }
        public boolean isGroup() { return isGroup; }
    }

    /**
     * 按层级路径分组字段
     * 根据需求：
     * 1. 单层级字段（如"部门"）放入"default"分组
     * 2. 多层级字段按完整的层级路径分组（除最后一级）
     * 例如：["ID", "员工信息", "姓名"] → 分组key为 "ID|员工信息"
     *
     * @param fieldsMap 字段属性映射
     * @return 按层级路径分组的字段映射
     */
    private static Map<String, List<Map.Entry<String, FieldProperty>>> groupFieldsByFirstLevel(Map<String, FieldProperty> fieldsMap) {
        Map<String, List<Map.Entry<String, FieldProperty>>> groupedFields = new LinkedHashMap<>();

        for (Map.Entry<String, FieldProperty> entry : fieldsMap.entrySet()) {
            FieldProperty fieldProperty = entry.getValue();
            if (fieldProperty != null && fieldProperty.getTableProperty() != null) {
                String[] values = fieldProperty.getTableProperty().value();

                String groupKey;
                if (values.length == 1) {
                    // 单层级字段放入默认分组
                    groupKey = "default";
                } else {
                    // 多层级字段按完整路径分组（除最后一级）
                    StringBuilder pathBuilder = new StringBuilder();
                    for (int i = 0; i < values.length - 1; i++) {
                        if (i > 0) pathBuilder.append("|");
                        pathBuilder.append(values[i]);
                    }
                    groupKey = pathBuilder.toString();
                }

                groupedFields.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(entry);
            }
        }

        return groupedFields;
    }

    /**
     * 验证组内字段order的连续性
     * 三级及以上层级要求同一分组内的字段order必须连续
     *
     * @param groupKey 分组key
     * @param fieldsInGroup 分组内的字段列表（已按order排序）
     */
    private static void validateOrderContinuity(String groupKey, List<Map.Entry<String, FieldProperty>> fieldsInGroup) {
        if (fieldsInGroup.size() <= 1) {
            return; // 单个字段无需验证
        }

        for (int i = 1; i < fieldsInGroup.size(); i++) {
            int prevOrder = fieldsInGroup.get(i - 1).getValue().getTableProperty().order();
            int currentOrder = fieldsInGroup.get(i).getValue().getTableProperty().order();

            if (currentOrder != prevOrder + 1) {
                String prevFieldName = fieldsInGroup.get(i - 1).getKey();
                String currentFieldName = fieldsInGroup.get(i).getKey();

                throw new IllegalArgumentException(
                        String.format("分组 '%s' 中的字段order不连续: %s(order=%d) 和 %s(order=%d). " +
                                        "三级及以上层级要求同一分组内的order必须连续。",
                                groupKey, prevFieldName, prevOrder, currentFieldName, currentOrder)
                );
            }
        }
    }

    /**
     * 表头单元格类，用于支持合并单元格
     */
    public static class HeaderCell {
        private String value;
        private int level;
        private int colSpan = 1;
        private int rowSpan = 1;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public int getColSpan() { return colSpan; }
        public void setColSpan(int colSpan) { this.colSpan = colSpan; }

        public int getRowSpan() { return rowSpan; }
        public void setRowSpan(int rowSpan) { this.rowSpan = rowSpan; }
    }

    /**
     * 按指定字符数给文本添加换行符
     *
     * @param text 需要处理的文本
     * @param charsPerLine 每行字符数
     * @return 添加换行符后的文本
     */
    public static String addLineBreaks(String text, int charsPerLine) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i += charsPerLine) {
            if (i > 0) {
                result.append("\n");
            }
            int endIndex = Math.min(i + charsPerLine, text.length());
            result.append(text.substring(i, endIndex));
        }
        return result.toString();
    }

    /**
     * 每8个字符添加一个换行符（默认方法）
     *
     * @param text 需要处理的文本
     * @return 添加换行符后的文本
     */
    public static String addLineBreaksPer8Chars(String text) {
        return addLineBreaks(text, 8);
    }

    public static boolean areAllValuesNullOrBlank(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return true;
        }

        for (Object value : map.values()) {
            if (value != null) {
                if (value instanceof String) {
                    if (!((String) value).isEmpty()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}