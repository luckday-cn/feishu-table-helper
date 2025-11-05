package cn.isliu.core.builder;

import cn.isliu.core.*;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.config.MapTableConfig;
import cn.isliu.core.enums.ErrorCode;
import cn.isliu.core.enums.FileType;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.service.CustomValueService;
import cn.isliu.core.utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.isliu.core.utils.FsTableUtil.*;

/**
 * Map 数据写入构建器
 * 
 * 提供链式调用方式写入 Map 格式的飞书表格数据
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapWriteBuilder {
    
    private final String sheetId;
    private final String spreadsheetToken;
    private final List<Map<String, Object>> dataList;
    private MapTableConfig config;
    
    /**
     * 构造函数
     *
     * @param sheetId 工作表ID
     * @param spreadsheetToken 电子表格Token
     * @param dataList 要写入的Map数据列表
     */
    public MapWriteBuilder(String sheetId, String spreadsheetToken, List<Map<String, Object>> dataList) {
        this.sheetId = sheetId;
        this.spreadsheetToken = spreadsheetToken;
        this.dataList = dataList;
        this.config = MapTableConfig.createDefault();
    }
    
    /**
     * 设置表格配置
     *
     * @param config Map表格配置
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder config(MapTableConfig config) {
        this.config = config;
        return this;
    }
    
    /**
     * 设置标题行
     *
     * @param titleRow 标题行行号
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder titleRow(int titleRow) {
        this.config.setTitleRow(titleRow);
        return this;
    }
    
    /**
     * 设置数据起始行
     *
     * @param headLine 数据起始行号
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder headLine(int headLine) {
        this.config.setHeadLine(headLine);
        return this;
    }
    
    /**
     * 设置唯一键字段
     *
     * @param uniKeyNames 唯一键字段名集合
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder uniKeyNames(Set<String> uniKeyNames) {
        this.config.setUniKeyNames(uniKeyNames);
        return this;
    }
    
    /**
     * 添加唯一键字段
     *
     * @param uniKeyName 唯一键字段名
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder addUniKeyName(String uniKeyName) {
        this.config.addUniKeyName(uniKeyName);
        return this;
    }
    
    /**
     * 设置是否覆盖已存在数据
     *
     * @param enableCover 是否覆盖
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder enableCover(boolean enableCover) {
        this.config.setEnableCover(enableCover);
        return this;
    }
    
    /**
     * 设置是否忽略未找到的数据
     *
     * @param ignoreNotFound 是否忽略
     * @return MapWriteBuilder实例
     */
    public MapWriteBuilder ignoreNotFound(boolean ignoreNotFound) {
        this.config.setIgnoreNotFound(ignoreNotFound);
        return this;
    }
    
    /**
     * 执行数据写入
     *
     * @return 写入操作结果
     */
    public Object build() {
        if (dataList.isEmpty()) {
            FsLogger.warn("【Map写入】数据列表为空，跳过写入操作");
            return null;
        }
        
        FeishuClient client = FsClient.getInstance().getClient();
        Sheet sheet = FsApiUtil.getSheetMetadata(sheetId, client, spreadsheetToken);
        
        // 读取表格数据以获取字段位置映射和现有数据
        Map<String, String> titlePostionMap = readFieldsPositionMap(sheet, client);
        config.setFieldsPositionMap(titlePostionMap);
        
        // 读取现有数据用于匹配和更新
        Map<String, Integer> currTableRowMap = readExistingData(sheet, client, titlePostionMap);
        
        // 计算下一个可用行号
        int nextAvailableRow = calculateNextAvailableRow(currTableRowMap, config.getHeadLine());
        
        // 初始化批量插入对象
        CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder = 
            CustomValueService.ValueRequest.batchPutValues();
        
        List<FileData> fileDataList = new ArrayList<>();
        AtomicInteger rowCount = new AtomicInteger(nextAvailableRow);
        
        // 处理每条数据
        for (Map<String, Object> data : dataList) {
            String uniqueId = MapDataUtil.calculateUniqueId(data, config);
            
            AtomicReference<Integer> rowNum = new AtomicReference<>(currTableRowMap.get(uniqueId));
            
            if (uniqueId != null && rowNum.get() != null) {
                // 更新现有行
                rowNum.set(rowNum.get() + 1);
                processDataRow(data, titlePostionMap, rowNum.get(), resultValuesBuilder, 
                    fileDataList, config.isEnableCover());
            } else if (!config.isIgnoreNotFound()) {
                // 插入新行
                int newRow = rowCount.incrementAndGet();
                processDataRow(data, titlePostionMap, newRow, resultValuesBuilder, 
                    fileDataList, config.isEnableCover());
            }
        }
        
        // 检查是否需要扩展行数
        ensureSufficientRows(sheet, rowCount.get(), client);
        
        // 上传文件
        uploadFiles(fileDataList, client);
        
        // 批量写入数据
        return batchWriteValues(resultValuesBuilder, client);
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
     * 读取现有数据
     */
    private Map<String, Integer> readExistingData(Sheet sheet, FeishuClient client, Map<String, String> titlePostionMap) {
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
        List<FsTableData> dataList = getFsTableData(tableData, new ArrayList<>());
        
        // 获取标题映射
        Map<String, String> titleMap = new HashMap<>();
        dataList.stream()
            .filter(d -> d.getRow() == (titleRow - 1))
            .findFirst()
            .ifPresent(d -> {
                Map<String, String> map = (Map<String, String>) d.getData();
                titleMap.putAll(map);
            });
        
        // 转换为带字段名的数据，并计算唯一ID
        return dataList.stream()
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
                
                String uniqueId = MapDataUtil.calculateUniqueId(resultMap, config);
                item.setUniqueId(uniqueId);
                item.setFieldsPositionMap(titlePostionMap);
                return item;
            })
            .filter(item -> item.getUniqueId() != null)
            .collect(Collectors.toMap(FsTableData::getUniqueId, FsTableData::getRow, (v1, v2) -> v1));
    }
    
    /**
     * 计算下一个可用行号
     */
    private int calculateNextAvailableRow(Map<String, Integer> currTableRowMap, int headLine) {
        if (currTableRowMap.isEmpty()) {
            return headLine;
        }
        
        return currTableRowMap.values().stream()
            .max(Integer::compareTo)
            .map(maxRow -> maxRow + 1)
            .orElse(headLine);
    }
    
    /**
     * 处理单行数据
     */
    private void processDataRow(Map<String, Object> data, Map<String, String> titlePostionMap,
                                 int rowNum, CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder,
                                 List<FileData> fileDataList, boolean enableCover) {
        data.forEach((field, fieldValue) -> {
            String position = titlePostionMap.get(field);
            
            if (position == null || position.isEmpty()) {
                return;
            }
            
            // 处理文件数据
            if (fieldValue instanceof FileData) {
                FileData fileData = (FileData) fieldValue;
                String fileType = fileData.getFileType();
                if (fileType.equals(FileType.IMAGE.getType())) {
                    fileData.setSheetId(sheetId);
                    fileData.setSpreadsheetToken(spreadsheetToken);
                    fileData.setPosition(position + rowNum);
                    fileDataList.add(fileData);
                }
            }
            
            // 添加到批量写入
            if (enableCover || fieldValue != null) {
                resultValuesBuilder.addRange(sheetId, position + rowNum, position + rowNum)
                    .addRow(GenerateUtil.getRowData(fieldValue));
            }
        });
    }
    
    /**
     * 确保行数足够
     */
    private void ensureSufficientRows(Sheet sheet, int requiredRows, FeishuClient client) {
        int rowTotal = sheet.getGridProperties().getRowCount();
        if (requiredRows >= rowTotal) {
            FsApiUtil.addRowColumns(sheetId, spreadsheetToken, "ROWS", Math.abs(requiredRows - rowTotal), client);
        }
    }
    
    /**
     * 上传文件
     */
    private void uploadFiles(List<FileData> fileDataList, FeishuClient client) {
        fileDataList.forEach(fileData -> {
            try {
                FsApiUtil.imageUpload(
                    fileData.getImageData(),
                    fileData.getFileName(),
                    fileData.getPosition(),
                    fileData.getSheetId(),
                    fileData.getSpreadsheetToken(),
                    client
                );
            } catch (Exception e) {
                FsLogger.error(ErrorCode.BUSINESS_LOGIC_ERROR, 
                    "【飞书表格】Map写入-文件上传异常! " + fileData.getFileUrl());
            }
        });
    }
    
    /**
     * 批量写入数据
     */
    private Object batchWriteValues(CustomValueService.ValueRequest.BatchPutValuesBuilder resultValuesBuilder,
                                    FeishuClient client) {
        CustomValueService.ValueRequest build = resultValuesBuilder.build();
        CustomValueService.ValueBatchUpdatePutRequest batchPutValues = build.getBatchPutValues();
        List<CustomValueService.ValueRangeItem> valueRanges = batchPutValues.getValueRanges();
        
        if (valueRanges != null && !valueRanges.isEmpty()) {
            return FsApiUtil.batchPutValues(sheetId, spreadsheetToken, build, client);
        }
        
        FsLogger.warn("【Map写入】没有数据需要写入");
        return null;
    }
}

