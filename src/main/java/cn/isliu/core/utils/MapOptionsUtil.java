package cn.isliu.core.utils;

import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.pojo.FieldProperty;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Map 选项工具类
 * 
 * 提供选项提取、转换和处理的工具方法
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapOptionsUtil {
    
    /**
     * 从枚举类提取选项列表
     * 
     * 提取枚举类中所有枚举常量的描述（desc）作为下拉选项
     *
     * @param enumClass 枚举类
     * @return 选项列表（枚举的desc值）
     */
    public static List<String> extractOptionsFromEnum(Class<? extends BaseEnum> enumClass) {
        if (enumClass == null || enumClass == BaseEnum.class) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(enumClass.getEnumConstants())
            .map(BaseEnum::getDesc)
            .collect(Collectors.toList());
    }
    
    /**
     * 从枚举类提取选项映射
     * 
     * 提取枚举类中所有枚举常量的 code -> desc 映射
     *
     * @param enumClass 枚举类
     * @return 选项映射（code -> desc）
     */
    public static Map<String, String> extractOptionsMapFromEnum(Class<? extends BaseEnum> enumClass) {
        if (enumClass == null || enumClass == BaseEnum.class) {
            return new HashMap<>();
        }
        
        Map<String, String> optionsMap = new LinkedHashMap<>();
        Arrays.stream(enumClass.getEnumConstants())
            .forEach(e -> optionsMap.put(e.getCode(), e.getDesc()));
        return optionsMap;
    }
    
    /**
     * 创建动态的 OptionsValueProcess 类
     * 
     * 用于处理直接配置的 options 列表，将其包装为 OptionsValueProcess
     *
     * @param options 选项列表
     * @return 动态创建的 OptionsValueProcess 类
     */
    public static Class<? extends OptionsValueProcess> createDynamicOptionsClass(final List<String> options) {
        if (options == null || options.isEmpty()) {
            return OptionsValueProcess.class;
        }
        
        // 返回一个匿名内部类
        // 注意：由于 Java 的限制，我们不能真正动态创建类
        // 这里返回一个包装器类，在 MapSheetBuilder 中特殊处理
        return DynamicOptionsProcess.class;
    }
    
    /**
     * 动态选项处理类（内部使用）
     * 
     * 从 customProperties 中读取预先配置的选项列表
     * 在 MapSheetBuilder 中，会将字段的 options 放入 customProperties
     */
    public static class DynamicOptionsProcess implements OptionsValueProcess {
        @Override
        public List<String> process(Object value) {
            if (value instanceof Map) {
                Map<String, Object> properties = (Map<String, Object>) value;
                
                // 从 _field 获取当前字段信息
                Object fieldObj = properties.get("_field");
                if (fieldObj instanceof FieldProperty) {
                    FieldProperty fieldProperty =
                        (FieldProperty) fieldObj;
                    
                    // 获取字段名
                    String[] fieldNames = fieldProperty.getTableProperty().value();
                    if (fieldNames != null && fieldNames.length > 0) {
                        String fieldName = fieldNames[0];
                        
                        // 从 customProperties 中获取该字段的 options
                        Object optionsObj = properties.get("_dynamicOptions_" + fieldName);
                        if (optionsObj instanceof List) {
                            return (List<String>) optionsObj;
                        }
                    }
                }
            }
            
            return new ArrayList<>();
        }
    }
    
    /**
     * 判断是否为动态选项处理类
     *
     * @param optionsClass 选项处理类
     * @return 是否为动态选项处理类
     */
    public static boolean isDynamicOptionsClass(Class<? extends OptionsValueProcess> optionsClass) {
        return optionsClass != null && optionsClass == DynamicOptionsProcess.class;
    }
}

