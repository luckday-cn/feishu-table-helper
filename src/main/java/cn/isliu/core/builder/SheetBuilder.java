package cn.isliu.core.builder;

import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.client.FeishuClient;
import cn.isliu.core.client.FsClient;
import cn.isliu.core.pojo.FieldProperty;
import cn.isliu.core.service.CustomCellService;
import cn.isliu.core.utils.FsApiUtil;
import cn.isliu.core.utils.FsTableUtil;
import cn.isliu.core.utils.PropertyUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表格构建器
 * 
 * 提供链式调用方式创建飞书表格，支持字段过滤等高级功能。
 */
public class SheetBuilder<T> {
    
    private final String sheetName;
    private final String spreadsheetToken;
    private final Class<T> clazz;
    private List<String> includeFields;
    
    /**
     * 构造函数
     * 
     * @param sheetName 工作表名称
     * @param spreadsheetToken 电子表格Token
     * @param clazz 实体类Class对象
     */
    public SheetBuilder(String sheetName, String spreadsheetToken, Class<T> clazz) {
        this.sheetName = sheetName;
        this.spreadsheetToken = spreadsheetToken;
        this.clazz = clazz;
    }
    
    /**
     * 设置包含的字段列表
     * 
     * 指定要包含在表格中的字段名称列表。如果不设置，则包含所有带有@TableProperty注解的字段。
     * 
     * @param fields 要包含的字段名称列表
     * @return SheetBuilder实例，支持链式调用
     */
    public SheetBuilder<T> includeColumnField(List<String> fields) {
        this.includeFields = new ArrayList<>(fields);
        return this;
    }
    
    /**
     * 构建表格并返回工作表ID
     * 
     * 根据配置的参数创建飞书表格，包括表头、样式、单元格格式和下拉选项等。
     * 
     * @return 创建成功返回工作表ID
     */
    public String build() {
        // 获取所有字段映射
        Map<String, FieldProperty> allFieldsMap = PropertyUtil.getTablePropertyFieldsMap(clazz);
        
        // 根据includeFields过滤字段映射
        Map<String, FieldProperty> fieldsMap = filterFieldsMap(allFieldsMap);
        
        // 生成表头
        List<String> headers = PropertyUtil.getHeaders(fieldsMap);
        
        // 获取表格配置
        TableConf tableConf = PropertyUtil.getTableConf(clazz);
        
        // 创建飞书客户端
        FeishuClient client = FsClient.getInstance().getClient();
        
        // 1、创建sheet
        String sheetId = FsApiUtil.createSheet(sheetName, client, spreadsheetToken);
        
        // 2、添加表头数据
        FsApiUtil.putValues(spreadsheetToken, FsTableUtil.getHeadTemplateBuilder(sheetId, headers, fieldsMap, tableConf), client);
        
        // 3、设置表格样式
        FsApiUtil.setTableStyle(FsTableUtil.getDefaultTableStyle(sheetId, fieldsMap, tableConf), client, spreadsheetToken);
        
        // 4、合并单元格
        List<CustomCellService.CellRequest> mergeCell = FsTableUtil.getMergeCell(sheetId, fieldsMap);
        if (!mergeCell.isEmpty()) {
            mergeCell.forEach(cell -> FsApiUtil.mergeCells(cell, client, spreadsheetToken));
        }
        
        // 5、设置单元格为文本格式
        if (tableConf.isText()) {
            String column = FsTableUtil.getColumnNameByNuNumber(headers.size());
            FsApiUtil.setCellType(sheetId, "@", "A1", column + 200, client, spreadsheetToken);
        }
        
        // 6、设置表格下拉
        FsTableUtil.setTableOptions(spreadsheetToken, headers, fieldsMap, sheetId, tableConf.enableDesc());
        
        return sheetId;
    }
    
    /**
     * 根据包含字段列表过滤字段映射
     * 
     * @param allFieldsMap 所有字段映射
     * @return 过滤后的字段映射
     */
    private Map<String, FieldProperty> filterFieldsMap(Map<String, FieldProperty> allFieldsMap) {
        // 如果没有指定包含字段，返回所有字段
        if (includeFields == null || includeFields.isEmpty()) {
            return allFieldsMap;
        }
        
        // 根据字段名过滤，保留指定的字段
        return allFieldsMap.entrySet().stream()
                .filter(entry -> includeFields.contains(entry.getValue().getField()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
