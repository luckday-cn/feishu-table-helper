package cn.isliu.core.config;

import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;

import java.util.*;

/**
 * Map方式字段定义类
 * 
 * 用于替代 @TableProperty 注解，定义单个字段的所有属性
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapFieldDefinition {
    
    /**
     * 字段名称（对应表格列名）
     */
    private String fieldName;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 字段排序顺序，数值越小越靠前
     */
    private int order;
    
    /**
     * 字段类型
     */
    private TypeEnum type = TypeEnum.TEXT;
    
    /**
     * 下拉选项列表（当type为单选/多选时使用）
     */
    private List<String> options;
    
    /**
     * 选项映射（code -> label）
     */
    private Map<String, String> optionsMap;
    
    /**
     * 枚举类（可选，用于从枚举类生成选项）
     */
    private Class<? extends BaseEnum> enumClass;
    
    /**
     * 选项处理类（可选，用于自定义选项处理逻辑）
     */
    private Class<? extends OptionsValueProcess> optionsClass;
    
    /**
     * 是否必填
     */
    private boolean required = false;
    
    /**
     * 默认值
     */
    private String defaultValue;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Map<String, String> getOptionsMap() {
        return optionsMap;
    }

    public void setOptionsMap(Map<String, String> optionsMap) {
        this.optionsMap = optionsMap;
    }

    public Class<? extends BaseEnum> getEnumClass() {
        return enumClass;
    }

    public void setEnumClass(Class<? extends BaseEnum> enumClass) {
        this.enumClass = enumClass;
    }

    public Class<? extends OptionsValueProcess> getOptionsClass() {
        return optionsClass;
    }

    public void setOptionsClass(Class<? extends OptionsValueProcess> optionsClass) {
        this.optionsClass = optionsClass;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MapFieldDefinition that = (MapFieldDefinition) o;
        return order == that.order && required == that.required && Objects.equals(fieldName, that.fieldName) && Objects.equals(description, that.description) && type == that.type && Objects.equals(options, that.options) && Objects.equals(optionsMap, that.optionsMap) && Objects.equals(enumClass, that.enumClass) && Objects.equals(optionsClass, that.optionsClass) && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, description, order, type, options, optionsMap, enumClass, optionsClass, required, defaultValue);
    }

    @Override
    public String toString() {
        return "MapFieldDefinition{" +
                "fieldName='" + fieldName + '\'' +
                ", description='" + description + '\'' +
                ", order=" + order +
                ", type=" + type +
                ", options=" + options +
                ", optionsMap=" + optionsMap +
                ", enumClass=" + enumClass +
                ", optionsClass=" + optionsClass +
                ", required=" + required +
                ", defaultValue='" + defaultValue + '\'' +
                '}';
    }

    /**
     * 创建构建器
     *
     * @return Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 快速创建文本字段
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition text(String fieldName, int order) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.TEXT)
            .build();
    }
    
    /**
     * 快速创建文本字段（带描述）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param description 字段描述
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition text(String fieldName, int order, String description) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.TEXT)
            .description(description)
            .build();
    }
    
    /**
     * 快速创建单选字段
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition singleSelect(String fieldName, int order, String... options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.SINGLE_SELECT)
            .options(Arrays.asList(options))
            .build();
    }
    
    /**
     * 快速创建单选字段（带选项列表）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition singleSelect(String fieldName, int order, List<String> options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.SINGLE_SELECT)
            .options(options)
            .build();
    }
    
    /**
     * 快速创建单选字段（带描述）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param description 字段描述
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition singleSelectWithDesc(String fieldName, int order, String description, List<String> options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.SINGLE_SELECT)
            .description(description)
            .options(options)
            .build();
    }
    
    /**
     * 快速创建多选字段
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition multiSelect(String fieldName, int order, String... options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.MULTI_SELECT)
            .options(Arrays.asList(options))
            .build();
    }
    
    /**
     * 快速创建多选字段（带选项列表）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition multiSelect(String fieldName, int order, List<String> options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.MULTI_SELECT)
            .options(options)
            .build();
    }
    
    /**
     * 快速创建多选字段（带描述）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param description 字段描述
     * @param options 选项列表
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition multiSelectWithDesc(String fieldName, int order, String description, List<String> options) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.MULTI_SELECT)
            .description(description)
            .options(options)
            .build();
    }
    
    /**
     * 快速创建单选字段（使用枚举类）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param enumClass 枚举类
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition singleSelectWithEnum(String fieldName, int order, Class<? extends BaseEnum> enumClass) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.SINGLE_SELECT)
            .enumClass(enumClass)
            .build();
    }
    
    /**
     * 快速创建单选字段（使用枚举类，带描述）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param description 字段描述
     * @param enumClass 枚举类
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition singleSelectWithEnum(String fieldName, int order, String description, Class<? extends BaseEnum> enumClass) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.SINGLE_SELECT)
            .description(description)
            .enumClass(enumClass)
            .build();
    }
    
    /**
     * 快速创建多选字段（使用枚举类）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param enumClass 枚举类
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition multiSelectWithEnum(String fieldName, int order, Class<? extends BaseEnum> enumClass) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.MULTI_SELECT)
            .enumClass(enumClass)
            .build();
    }
    
    /**
     * 快速创建多选字段（使用枚举类，带描述）
     *
     * @param fieldName 字段名称
     * @param order 排序顺序
     * @param description 字段描述
     * @param enumClass 枚举类
     * @return MapFieldDefinition实例
     */
    public static MapFieldDefinition multiSelectWithEnum(String fieldName, int order, String description, Class<? extends BaseEnum> enumClass) {
        return builder()
            .fieldName(fieldName)
            .order(order)
            .type(TypeEnum.MULTI_SELECT)
            .description(description)
            .enumClass(enumClass)
            .build();
    }
    
    /**
     * 从 Map 批量创建字段定义
     *
     * @param fieldMap key为字段名，value为字段类型字符串
     * @return 字段定义列表
     */
    public static List<MapFieldDefinition> fromMap(Map<String, String> fieldMap) {
        List<MapFieldDefinition> fields = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            TypeEnum type = parseType(entry.getValue());
            fields.add(builder()
                .fieldName(entry.getKey())
                .order(order++)
                .type(type)
                .build());
        }
        return fields;
    }
    
    /**
     * 从字段名列表快速创建文本字段
     *
     * @param fieldNames 字段名称列表
     * @return 字段定义列表
     */
    public static List<MapFieldDefinition> fromFieldNames(List<String> fieldNames) {
        List<MapFieldDefinition> fields = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            fields.add(text(fieldNames.get(i), i));
        }
        return fields;
    }
    
    /**
     * 解析类型字符串
     */
    private static TypeEnum parseType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return TypeEnum.TEXT;
        }
        
        try {
            return TypeEnum.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TypeEnum.TEXT;
        }
    }
    
    /**
     * 字段定义构建器
     */
    public static class Builder {
        private final MapFieldDefinition definition = new MapFieldDefinition();
        
        public Builder fieldName(String fieldName) {
            definition.fieldName = fieldName;
            return this;
        }
        
        public Builder description(String description) {
            definition.description = description;
            return this;
        }
        
        public Builder order(int order) {
            definition.order = order;
            return this;
        }
        
        public Builder type(TypeEnum type) {
            definition.type = type;
            return this;
        }
        
        public Builder options(List<String> options) {
            if (options == null || options.isEmpty()) {
                return this;
            }
            definition.options = new ArrayList<>(options);
            return this;
        }
        
        public Builder options(String... options) {
            if (options == null || options.length == 0) {
                return this;
            }
            definition.options = Arrays.asList(options);
            return this;
        }
        
        public Builder optionsMap(Map<String, String> optionsMap) {
            definition.optionsMap = new HashMap<>(optionsMap);
            return this;
        }
        
        public Builder enumClass(Class<? extends BaseEnum> enumClass) {
            definition.enumClass = enumClass;
            return this;
        }
        
        public Builder optionsClass(Class<? extends OptionsValueProcess> optionsClass) {
            definition.optionsClass = optionsClass;
            return this;
        }
        
        public Builder required(boolean required) {
            definition.required = required;
            return this;
        }
        
        public Builder defaultValue(String defaultValue) {
            definition.defaultValue = defaultValue;
            return this;
        }
        
        public MapFieldDefinition build() {
            // 验证必填字段
            if (definition.fieldName == null || definition.fieldName.isEmpty()) {
                throw new IllegalArgumentException("字段名称不能为空");
            }
            
            return definition;
        }
    }
}

