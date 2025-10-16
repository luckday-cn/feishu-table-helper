package cn.isliu.core.utils;

import cn.isliu.core.config.MapTableConfig;

import java.util.*;

/**
 * Map 数据处理工具类
 * 
 * 提供Map格式数据的处理方法，包括唯一ID计算、字段位置映射等
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapDataUtil {
    
    /**
     * 计算 Map 数据的唯一ID
     *
     * 根据配置的唯一键字段计算数据的唯一标识。
     * 如果没有指定唯一键，则使用所有字段计算。
     *
     * @param data Map数据
     * @param config 表格配置
     * @return 唯一ID（SHA256哈希值）
     */
    public static String calculateUniqueId(Map<String, Object> data, MapTableConfig config) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        Set<String> uniKeyNames = config.getUniKeyNames();
        
        if (uniKeyNames == null || uniKeyNames.isEmpty()) {
            // 如果没有指定唯一键，使用所有字段计算
            String jsonStr = StringUtil.mapToJson(data);
            return StringUtil.getSHA256(jsonStr);
        }
        
        // 使用指定的唯一键字段计算
        List<Object> uniKeyValues = new ArrayList<>();
        for (String key : uniKeyNames) {
            if (data.containsKey(key)) {
                uniKeyValues.add(data.get(key));
            }
        }
        
        if (uniKeyValues.isEmpty()) {
            return null;
        }
        
        String jsonStr = StringUtil.listToJson(uniKeyValues);
        return StringUtil.getSHA256(jsonStr);
    }
    
    /**
     * 从标题行数据构建字段位置映射
     *
     * 将标题行的列位置与字段名称进行映射，用于后续数据写入时定位单元格位置。
     *
     * @param titleRowData 标题行数据，key为列位置（如"A"、"B"），value为字段名称
     * @return 字段位置映射，key为字段名称，value为列位置
     */
    public static Map<String, String> buildFieldsPositionMap(Map<String, String> titleRowData) {
        if (titleRowData == null || titleRowData.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, String> fieldsPositionMap = new HashMap<>();
        titleRowData.forEach((position, fieldName) -> {
            if (fieldName != null && !fieldName.isEmpty()) {
                fieldsPositionMap.put(fieldName, position);
            }
        });
        return fieldsPositionMap;
    }
    
    /**
     * 验证数据字段是否与表格字段匹配
     *
     * 检查数据中的字段是否在表格的字段位置映射中存在。
     *
     * @param data 数据Map
     * @param fieldsPositionMap 字段位置映射
     * @return 未匹配的字段列表
     */
    public static List<String> validateDataFields(Map<String, Object> data, Map<String, String> fieldsPositionMap) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        if (fieldsPositionMap == null || fieldsPositionMap.isEmpty()) {
            return new ArrayList<>(data.keySet());
        }
        
        List<String> unmatchedFields = new ArrayList<>();
        for (String fieldName : data.keySet()) {
            if (!fieldsPositionMap.containsKey(fieldName)) {
                unmatchedFields.add(fieldName);
            }
        }
        
        return unmatchedFields;
    }
    
    /**
     * 过滤数据字段
     *
     * 只保留在字段位置映射中存在的字段。
     *
     * @param data 原始数据Map
     * @param fieldsPositionMap 字段位置映射
     * @return 过滤后的数据Map
     */
    public static Map<String, Object> filterDataFields(Map<String, Object> data, Map<String, String> fieldsPositionMap) {
        if (data == null || data.isEmpty()) {
            return new HashMap<>();
        }
        
        if (fieldsPositionMap == null || fieldsPositionMap.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Object> filteredData = new HashMap<>();
        data.forEach((fieldName, fieldValue) -> {
            if (fieldsPositionMap.containsKey(fieldName)) {
                filteredData.put(fieldName, fieldValue);
            }
        });
        
        return filteredData;
    }
}

