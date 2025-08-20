package cn.isliu.core.utils;

import cn.isliu.core.*;
import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.client.FsClient;

import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomValueService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
    public static List<FsTableData> getFsTableData(Sheet sheet, String spreadsheetToken, TableConf tableConf) {

        // 计算数据范围
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
            int endRowIndex = Math.max(startRowIndex + rowCount - 1, totalRow - 1);

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

        // 获取飞书表格数据
        TableData tableData = processSheetData(sheet, values);

        List<FsTableData> dataList = getFsTableData(tableData);
        Map<String, String> titleMap = new HashMap<>();

        dataList.stream().filter(d -> d.getRow() == (tableConf.titleRow() - 1)).findFirst()
                .ifPresent(d -> {
                    Map<String, String> map = (Map<String, String>) d.getData();
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
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取飞书表格数据
     * 
     * @param tableData 表格数据对象
     * @return 飞书表格数据列表
     */
    private static List<FsTableData> getFsTableData(TableData tableData) {
        return getFsTableData(tableData, new ArrayList<>());
    }

    /**
     * 获取飞书表格数据
     * 
     * @param tableData 表格数据对象
     * @param ignoreUniqueFields 忽略的唯一字段列表
     * @return 飞书表格数据列表
     */
    private static List<FsTableData> getFsTableData(TableData tableData, List<String> ignoreUniqueFields) {

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

    public static void setTableOptions(String spreadsheetToken, List<String> headers, Map<String, FieldProperty> fieldsMap, String sheetId, boolean enableDesc) {
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
                        OptionsValueProcess optionsValueProcess = optionsClass.getDeclaredConstructor().newInstance();
                        result = (List<String>) optionsValueProcess.process();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }

                    FsApiUtil.setOptions(sheetId, FsClient.getInstance().getClient(), spreadsheetToken, tableProperty.type() == TypeEnum.MULTI_SELECT, position + line, position + 200,
                            result);
                }
            }
        });
    }

    public static CustomValueService.ValueRequest getHeadTemplateBuilder(String sheetId, List<String> headers, 
                  Map<String, FieldProperty> fieldsMap, TableConf tableConf) {
        
        String position = FsTableUtil.getColumnNameByNuNumber(headers.size());

        CustomValueService.ValueRequest.BatchPutValuesBuilder batchPutValuesBuilder 
                = CustomValueService.ValueRequest.batchPutValues();

        // 获取父级表头
//        int maxLevel = getMaxLevel(fieldsMap);
//
//        Map<Integer, List<String>> levelListMap = groupFieldsByLevel(fieldsMap);
//        for (int i = maxLevel; i >= 1; i--) {
//            List<String> values = levelListMap.get(i);
//            batchPutValuesBuilder.addRange(sheetId + "!A" + i + ":" + position + i);
//
//        }
//
//        int titleRow = maxLevel;

        int titleRow = tableConf.titleRow();
        if (tableConf.enableDesc()) {
            int descRow = titleRow + 1;
            batchPutValuesBuilder.addRange(sheetId + "!A" + titleRow + ":" + position + descRow);
            batchPutValuesBuilder.addRow(headers.toArray());
            batchPutValuesBuilder.addRow(getDescArray(headers, fieldsMap));
        } else {
            batchPutValuesBuilder.addRange(sheetId + "!A" + titleRow + ":" + position + titleRow);
            batchPutValuesBuilder.addRow(headers.toArray());
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
        Object[] descArray = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            FieldProperty fieldProperty = fieldsMap.get(header);
            if (fieldProperty != null && fieldProperty.getTableProperty() != null) {
                String desc = fieldProperty.getTableProperty().desc();
                if (desc != null && !desc.isEmpty()) {
                    try {
                        JsonElement element = JsonParser.parseString(desc);
                        if (element.isJsonObject()) {
                            descArray[i] = element.getAsJsonObject();
                        } else if (element.isJsonArray()) {
                            descArray[i] = element.getAsJsonArray();
                        } else {
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

    public static String getDefaultTableStyle(String sheetId, int size, TableConf tableConf) {
        int row = tableConf.titleRow();
        String colorTemplate = "{\"data\": [{\"style\": {\"font\": {\"bold\": true, \"clean\": false, \"italic\": false, \"fontSize\": \"10pt/1.5\"}, \"clean\": false, \"hAlign\": 1, \"vAlign\": 1, \"backColor\": \"#000000\", \"foreColor\": \"#ffffff\", \"formatter\": \"\", \"borderType\": \"FULL_BORDER\", \"borderColor\": \"#000000\", \"textDecoration\": 0}, \"ranges\": [\"SHEET_ID!RANG\"]}]}";
        colorTemplate = colorTemplate.replace("SHEET_ID", sheetId);
        colorTemplate = colorTemplate.replace("RANG", "A" + row + ":" + FsTableUtil.getColumnNameByNuNumber(size) + row);
        colorTemplate = colorTemplate.replace("FORE_COLOR", tableConf.headFontColor())
                .replace("BACK_COLOR", tableConf.headBackColor());
        return colorTemplate;
    }

    /**
     * 根据层级分组字段属性
     * 
     * @param fieldsMap 字段属性映射
     * @return 按层级分组的映射，key为层级，value为该层级的字段名数组
     */
    public static Map<Integer, List<String>> groupFieldsByLevel(Map<String, FieldProperty> fieldsMap) {
        Map<Integer, List<String>> levelMap = new HashMap<>();
        
        for (Map.Entry<String, FieldProperty> entry : fieldsMap.entrySet()) {
            FieldProperty fieldProperty = entry.getValue();
            if (fieldProperty != null && fieldProperty.getTableProperty() != null) {
                String[] values = fieldProperty.getTableProperty().value();
                for (int i = 0; i < values.length; i++) {
                    levelMap.computeIfAbsent(i, k -> new ArrayList<>()).add(values[i]);
                }
            }
        }
        
        return levelMap;
    }

    public static void main(String[] args) {
        String str ="支持1～3个搜索";
        System.out.println(str.length());
    }
}