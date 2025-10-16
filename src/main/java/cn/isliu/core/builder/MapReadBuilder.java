package cn.isliu.core.builder;

import cn.isliu.core.*;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.MapTableConfig;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.MapDataUtil;

import java.util.*;
import java.util.stream.Collectors;

import static cn.isliu.core.utils.FsTableUtil.*;

/**
 * Map 数据读取构建器
 * 
 * 提供链式调用方式从飞书表格读取数据并转换为 Map 格式
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapReadBuilder {
    
    private final String sheetId;
    private final String spreadsheetToken;
    private MapTableConfig config;
    
    /**
     * 构造函数
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     */
    public MapReadBuilder(String sheetId, String spreadsheetToken) {
        this.sheetId = sheetId;
        this.spreadsheetToken = spreadsheetToken;
        this.config = MapTableConfig.createDefault();
    }
    
    /**
     * 设置表格配置
     *
     * @param config Map表格配置
     * @return MapReadBuilder实例
     */
    public MapReadBuilder config(MapTableConfig config) {
        this.config = config;
        return this;
    }
    
    /**
     * 设置标题行
     *
     * @param titleRow 标题行行号
     * @return MapReadBuilder实例
     */
    public MapReadBuilder titleRow(int titleRow) {
        this.config.setTitleRow(titleRow);
        return this;
    }
    
    /**
     * 设置数据起始行
     *
     * @param headLine 数据起始行号
     * @return MapReadBuilder实例
     */
    public MapReadBuilder headLine(int headLine) {
        this.config.setHeadLine(headLine);
        return this;
    }
    
    /**
     * 设置唯一键字段
     *
     * @param uniKeyNames 唯一键字段名集合
     * @return MapReadBuilder实例
     */
    public MapReadBuilder uniKeyNames(Set<String> uniKeyNames) {
        this.config.setUniKeyNames(uniKeyNames);
        return this;
    }
    
    /**
     * 添加唯一键字段
     *
     * @param uniKeyName 唯一键字段名
     * @return MapReadBuilder实例
     */
    public MapReadBuilder addUniKeyName(String uniKeyName) {
        this.config.addUniKeyName(uniKeyName);
        return this;
    }
    
    /**
     * 执行数据读取
     *
     * 注意：
     * - 如果某行数据的所有业务字段值都为null或空字符串，该行数据将被自动过滤
     * - 返回的Map中包含 _uniqueId 和 _rowNumber 两个特殊字段
     *
     * @return Map数据列表
     */
    public List<Map<String, Object>> build() {
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        
        // 读取字段位置映射
        Map<String, String> titlePostionMap = readFieldsPositionMap(sheet, client);
        config.setFieldsPositionMap(titlePostionMap);
        
        // 读取表格数据并转换为Map格式
        List<Map<String, Object>> dataList = readTableData(sheet, client, titlePostionMap);
        
        FsLogger.info("【Map读取】成功读取 {} 条数据", dataList.size());
        return dataList;
    }
    
    /**
     * 执行分组数据读取
     *
     * 注意：
     * - 如果某个分组下某行数据的所有字段值都为null或空字符串，该行数据将被自动过滤
     * - 返回的Map中包含 _uniqueId 和 _rowNumber 两个特殊字段
     *
     * @return 按分组字段组织的Map数据
     */
    public Map<String, List<Map<String, Object>>> groupBuild() {
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        
        // 读取字段位置映射
        Map<String, String> titlePostionMap = readFieldsPositionMap(sheet, client);
        config.setFieldsPositionMap(titlePostionMap);
        
        // 读取分组表格数据
        Map<String, List<Map<String, Object>>> groupedData = readGroupedTableData(sheet, client, titlePostionMap);
        
        int totalCount = groupedData.values().stream().mapToInt(List::size).sum();
        FsLogger.info("【Map读取】成功读取 {} 个分组，共 {} 条数据", groupedData.size(), totalCount);
        return groupedData;
    }
    
    /**
     * 读取字段位置映射
     */
    private Map<String, String> readFieldsPositionMap(Sheet sheet, FeishuClient client) {
        int titleRow = config.getTitleRow();
        int colCount = sheet.getGridProperties().getColumnCount();
        
        // 读取标题行数据
        ValuesBatch valuesBatch = FsApiUtil.getSheetData(
            sheet.getSheetId(), spreadsheetToken,
            "A" + titleRow,
            getColumnName(colCount - 1) + titleRow,
            client
        );
        
        Map<String, String> fieldsPositionMap = new HashMap<>();
        
        if (valuesBatch != null && valuesBatch.getValueRanges() != null) {
            for (ValueRange valueRange : valuesBatch.getValueRanges()) {
                if (valueRange.getValues() != null && !valueRange.getValues().isEmpty()) {
                    List<Object> titleRowValues = valueRange.getValues().get(0);
                    for (int i = 0; i < titleRowValues.size(); i++) {
                        Object value = titleRowValues.get(i);
                        if (value != null) {
                            String fieldName = value.toString();
                            String columnPosition = getColumnName(i);
                            fieldsPositionMap.put(fieldName, columnPosition);
                        }
                    }
                }
            }
        }
        
        return fieldsPositionMap;
    }
    
    /**
     * 读取表格数据并转换为Map格式
     */
    private List<Map<String, Object>> readTableData(Sheet sheet, FeishuClient client, Map<String, String> titlePostionMap) {
        int headLine = config.getHeadLine();
        int titleRow = config.getTitleRow();
        int totalRow = sheet.getGridProperties().getRowCount();
        int colCount = sheet.getGridProperties().getColumnCount();
        int startOffset = 1;
        
        // 批量读取数据
        int rowCountPerBatch = Math.min(totalRow, 100);
        int actualRows = Math.max(0, totalRow - startOffset);
        int batchCount = (actualRows + rowCountPerBatch - 1) / rowCountPerBatch;
        
        List<List<Object>> values = new LinkedList<>();
        for (int i = 0; i < batchCount; i++) {
            int startRowIndex = startOffset + i * rowCountPerBatch;
            int endRowIndex = Math.min(startRowIndex + rowCountPerBatch - 1, totalRow - 1);
            
            ValuesBatch valuesBatch = FsApiUtil.getSheetData(
                sheet.getSheetId(), spreadsheetToken,
                "A" + startRowIndex,
                getColumnName(colCount - 1) + endRowIndex,
                client
            );
            
            if (valuesBatch != null && valuesBatch.getValueRanges() != null) {
                for (ValueRange valueRange : valuesBatch.getValueRanges()) {
                    if (valueRange.getValues() != null) {
                        values.addAll(valueRange.getValues());
                    }
                }
            }
        }
        
        // 处理表格数据
        TableData tableData = processSheetData(sheet, values);
        List<FsTableData> fsTableDataList = getFsTableData(tableData, new ArrayList<>());
        
        // 获取标题映射
        Map<String, String> titleMap = new HashMap<>();
        fsTableDataList.stream()
            .filter(d -> d.getRow() == (titleRow - 1))
            .findFirst()
            .ifPresent(d -> {
                Map<String, String> map = (Map<String, String>) d.getData();
                titleMap.putAll(map);
            });
        
        // 转换为带字段名的Map数据
        return fsTableDataList.stream()
            .filter(fsTableData -> fsTableData.getRow() >= headLine)
            .map(item -> {
                Map<String, Object> resultMap = new HashMap<>();
                Map<String, Object> map = (Map<String, Object>) item.getData();
                
                map.forEach((k, v) -> {
                    String title = titleMap.get(k);
                    if (title != null) {
                        resultMap.put(title, v);
                    }
                });
                
                // 计算并设置唯一ID
                String uniqueId = MapDataUtil.calculateUniqueId(resultMap, config);
                if (uniqueId != null) {
                    resultMap.put("_uniqueId", uniqueId);
                }
                
                // 设置行号
                resultMap.put("_rowNumber", item.getRow() + 1);
                
                return resultMap;
            })
            .filter(map -> {
                // 过滤条件1：Map不能为空
                if (map.isEmpty()) {
                    return false;
                }
                
                // 过滤条件2：除了 _uniqueId 和 _rowNumber 外还有其他数据
                if (map.size() <= 2) {
                    return false;
                }
                
                // 过滤条件3：检查是否所有业务字段的值都为null或空字符串
                boolean hasNonNullValue = false;
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 跳过特殊字段
                    if (key.equals("_uniqueId") || key.equals("_rowNumber")) {
                        continue;
                    }
                    
                    // 检查是否有非null且非空字符串的值
                    if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                        hasNonNullValue = true;
                        break;
                    }
                }
                
                return hasNonNullValue;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 读取分组表格数据
     */
    private Map<String, List<Map<String, Object>>> readGroupedTableData(Sheet sheet, FeishuClient client, Map<String, String> titlePostionMap) {
        int headLine = config.getHeadLine();
        int titleRow = config.getTitleRow();
        int totalRow = sheet.getGridProperties().getRowCount();
        int colCount = sheet.getGridProperties().getColumnCount();
        int startOffset = 1;
        
        // 批量读取数据
        int rowCountPerBatch = Math.min(totalRow, 100);
        int actualRows = Math.max(0, totalRow - startOffset);
        int batchCount = (actualRows + rowCountPerBatch - 1) / rowCountPerBatch;
        
        List<List<Object>> values = new LinkedList<>();
        for (int i = 0; i < batchCount; i++) {
            int startRowIndex = startOffset + i * rowCountPerBatch;
            int endRowIndex = Math.min(startRowIndex + rowCountPerBatch - 1, totalRow - 1);
            
            ValuesBatch valuesBatch = FsApiUtil.getSheetData(
                sheet.getSheetId(), spreadsheetToken,
                "A" + startRowIndex,
                getColumnName(colCount - 1) + endRowIndex,
                client
            );
            
            if (valuesBatch != null && valuesBatch.getValueRanges() != null) {
                for (ValueRange valueRange : valuesBatch.getValueRanges()) {
                    if (valueRange.getValues() != null) {
                        values.addAll(valueRange.getValues());
                    }
                }
            }
        }
        
        // 处理表格数据
        TableData tableData = processSheetData(sheet, values);
        List<FsTableData> fsTableDataList = getFsTableData(tableData, new ArrayList<>());
        
        // 获取分组行和标题行
        Map<String, String> categoryMap = new HashMap<>();
        Map<String, List<String>> categoryPositionMap = new HashMap<>();
        
        // 读取分组行（titleRow - 1）
        fsTableDataList.stream()
            .filter(d -> d.getRow() == (titleRow - 2))
            .findFirst()
            .ifPresent(d -> {
                Map<String, String> map = (Map<String, String>) d.getData();
                map.forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        categoryMap.put(k, v);
                        categoryPositionMap.computeIfAbsent(v, k1 -> new ArrayList<>()).add(k);
                    }
                });
            });
        
        // 读取标题行
        Map<String, String> titleMap = new HashMap<>();
        fsTableDataList.stream()
            .filter(d -> d.getRow() == (titleRow - 1))
            .findFirst()
            .ifPresent(d -> {
                Map<String, String> map = (Map<String, String>) d.getData();
                map.forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        String category = categoryMap.get(k);
                        if (category != null && !category.isEmpty()) {
                            titleMap.put(k, v);
                        } else {
                            titleMap.put(k, v);
                        }
                    }
                });
            });
        
        // 按分组组织数据
        Map<String, List<Map<String, Object>>> groupedResult = new LinkedHashMap<>();
        
        for (Map.Entry<String, List<String>> entry : categoryPositionMap.entrySet()) {
            String groupName = entry.getKey();
            List<String> positions = entry.getValue();
            
            List<Map<String, Object>> groupData = fsTableDataList.stream()
                .filter(fsTableData -> fsTableData.getRow() >= headLine)
                .map(item -> {
                    Map<String, Object> resultMap = new HashMap<>();
                    Map<String, Object> map = (Map<String, Object>) item.getData();
                    
                    // 只提取该分组的字段
                    positions.forEach(pos -> {
                        String title = titleMap.get(pos);
                        if (title != null && map.containsKey(pos)) {
                            resultMap.put(title, map.get(pos));
                        }
                    });
                    
                    // 计算并设置唯一ID
                    String uniqueId = MapDataUtil.calculateUniqueId(resultMap, config);
                    if (uniqueId != null) {
                        resultMap.put("_uniqueId", uniqueId);
                    }
                    
                    // 设置行号
                    resultMap.put("_rowNumber", item.getRow() + 1);
                    
                    return resultMap;
                })
                .filter(map -> {
                    // 过滤条件1：除了 _uniqueId 和 _rowNumber 外还有其他数据
                    if (map.size() <= 2) {
                        return false;
                    }
                    
                    // 过滤条件2：检查是否所有业务字段的值都为null
                    boolean hasNonNullValue = false;
                    for (Map.Entry<String, Object> nullEntry : map.entrySet()) {
                        String key = nullEntry.getKey();
                        Object value = nullEntry.getValue();
                        
                        // 跳过特殊字段
                        if (key.equals("_uniqueId") || key.equals("_rowNumber")) {
                            continue;
                        }
                        
                        // 检查是否有非null且非空字符串的值
                        if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                            hasNonNullValue = true;
                            break;
                        }
                    }
                    
                    return hasNonNullValue;
                })
                .collect(Collectors.toList());
            
            groupedResult.put(groupName, groupData);
        }
        
        return groupedResult;
    }
}

