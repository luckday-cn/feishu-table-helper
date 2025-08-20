package cn.isliu.core.utils;

import cn.isliu.core.annotation.TableConf;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;
import cn.isliu.core.pojo.FieldProperty;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 属性工具类
 *
 * 提供处理实体类属性和注解的相关工具方法，
 * 主要用于解析@TableProperty注解并构建字段映射关系
 */
public class PropertyUtil {

    /**
     * 获取类及其嵌套类上@TableProperty注解的字段映射关系
     * 
     * 此方法是入口方法，用于获取一个类及其所有嵌套类中，
     * 被@TableProperty注解标记的字段的映射关系。
     * 注解中的值作为key，FieldProperty对象作为value返回。
     *
     * 对于嵌套属性，使用'.'连接符来表示层级关系。
     * 该方法会过滤掉有子级的字段，只返回最底层的字段映射。
     * 
     * @param clazz 要处理的类
     * @return 包含所有@TableProperty注解字段映射关系的Map，嵌套属性使用'.'连接
     */
    public static Map<String, FieldProperty> getTablePropertyFieldsMap(Class<?> clazz) {
        Map<String, FieldProperty> allFields = new TreeMap<>();
        Map<String, String> fieldsWithChildren = new TreeMap<>();
        getTablePropertyFieldsMapRecursive(clazz, allFields, "", "", new HashMap<>(), fieldsWithChildren, false);

        // 过滤掉有子级的字段
        Map<String, FieldProperty> result = new HashMap<>();
        for (Map.Entry<String, FieldProperty> entry : allFields.entrySet()) {
            if (!fieldsWithChildren.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 递归获取类及其嵌套类上@TableProperty注解的字段映射关系
     *
     * 这是一个递归方法，用于深入处理类的继承结构和嵌套结构，
     * 收集所有被@TableProperty注解标记的字段信息。
     *
     * 方法会处理循环引用问题，并限制递归深度，防止栈溢出。
     * 
     * @param clazz 当前处理的类
     * @param result 存储结果的Map
     * @param keyPrefix key的前缀（使用注解中的值构建）
     * @param valuePrefix value的前缀（使用字段实际名称构建）
     * @param depthMap 记录每个类的递归深度，用于检测循环引用
     * @param fieldsWithChildren 记录有子级的字段集合
     * @param parentHasAnnotation 父节点是否有注解
     */
    private static void getTablePropertyFieldsMapRecursive(Class<?> clazz, Map<String, FieldProperty> result, String keyPrefix, String valuePrefix, Map<Class<?>, Integer> depthMap, Map<String, String> fieldsWithChildren, boolean parentHasAnnotation) {
        // 检查类是否在允许的包范围内
        if (!isTargetPackageClass(clazz)) {
            return;
        }
        
        // 检测循环引用，限制递归深度
        Integer currentDepth = depthMap.getOrDefault(clazz, 0);
        if (currentDepth > 5) { // 限制最大递归深度为5
            // 遇到可能的循环引用时，只添加当前字段路径
            if (!keyPrefix.isEmpty()) {
                TableProperty dummyAnnotation = createDummyTableProperty(valuePrefix);
                result.put(keyPrefix, new FieldProperty(valuePrefix, dummyAnnotation));
            }
            return;
        }

        // 增加当前类的递归深度
        depthMap.put(clazz, currentDepth + 1);

        try {
            // 获取所有声明的字段
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                // 检查字段是否有@TableProperty注解
                if (field.isAnnotationPresent(TableProperty.class)) {
                    TableProperty tableProperty = field.getAnnotation(TableProperty.class);
                    String[] values = tableProperty.value();
                    String value = values[values.length - 1];
                    String[] propertyValues = value.split("\\."); // 支持多个值
                    String propertyValue = (propertyValues.length > 0 && !propertyValues[0].isEmpty()) ? propertyValues[0] : field.getName();

                    processFieldForMap(field, propertyValue, keyPrefix, valuePrefix, result, depthMap, fieldsWithChildren, parentHasAnnotation);
                } else {
                    // 即使字段没有@TableProperty注解，也要递归处理复杂类型字段
                    // 这样可以确保子节点有注解的字段也能被获取到
                    processFieldWithoutAnnotation(field, keyPrefix, valuePrefix, result, depthMap, fieldsWithChildren, parentHasAnnotation);
                }
            }
        } finally {
            // 减少当前类的递归深度
            if (currentDepth == 0) {
                depthMap.remove(clazz);
            } else {
                depthMap.put(clazz, currentDepth);
            }
        }
    }

    /**
     * 判断类是否在目标包范围内
     * 只处理用户定义的类，避免处理系统类如java.lang.ref.ReferenceQueue
     *
     * @param clazz 要检查的类
     * @return 是否为目标包下的类
     */
    private static boolean isTargetPackageClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        String className = clazz.getName();
        // 只处理用户自定义的类，排除系统类
        return !className.startsWith("java.") && 
               !className.startsWith("javax.") && 
               !className.startsWith("sun.") && 
               !className.startsWith("com.sun.") &&
               !className.startsWith("jdk.");
    }

    /**
     * 创建一个虚拟的TableProperty注解实例
     *
     * @param value 注解值
     * @return TableProperty实例
     */
    private static TableProperty createDummyTableProperty(String value) {
        return new TableProperty() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TableProperty.class;
            }

            @Override
            public String[] value() {
                return new String[]{value};
            }

            @Override
            public String desc() {
                return "";
            }

            @Override
            public String field() {
                return "";
            }

            @Override
            public int order() {
                return Integer.MAX_VALUE;
            }

            @Override
            public TypeEnum type() {
                return TypeEnum.TEXT;
            }

            @Override
            public Class<? extends BaseEnum> enumClass() {
                return BaseEnum.class;
            }

            @Override
            public Class<? extends FieldValueProcess> fieldFormatClass() {
                return FieldValueProcess.class;
            }

            @Override
            public Class<? extends OptionsValueProcess> optionsClass() {
                return OptionsValueProcess.class;
            }
        };
    }

    /**
     * 处理带有@TableProperty注解的字段
     *
     * @param field 当前处理的字段
     * @param fieldPropertyName 字段名称（注解中的值）
     * @param keyPrefix key的前缀（使用注解中的值构建）
     * @param valuePrefix value的前缀（使用字段实际名称构建）
     * @param result 存储结果的Map
     * @param depthMap 记录每个类的递归深度，用于检测循环引用
     * @param fieldsWithChildren 有子级的字段集合
     * @param parentHasAnnotation 父节点是否有注解
     */
    private static void processFieldForMap(Field field, String fieldPropertyName, String keyPrefix, String valuePrefix, Map<String, FieldProperty> result, Map<Class<?>, Integer> depthMap, Map<String, String> fieldsWithChildren, boolean parentHasAnnotation) {
        // Key使用注解值构建（显示给用户的名称）
        String fullKey = keyPrefix.isEmpty() ? fieldPropertyName : keyPrefix + "." + fieldPropertyName;
        // Value使用字段实际名称构建（实际的代码字段路径）
        String fullValue = valuePrefix.isEmpty() ? field.getName() : valuePrefix + "." + field.getName();
        TableProperty tableProperty = field.getAnnotation(TableProperty.class);

        // 如果字段是复杂类型，则递归处理
        Class<?> fieldType = field.getType();
        if (isComplexType(fieldType)) {
            // 用于收集子字段
            Map<String, FieldProperty> subFields = new HashMap<>();
            Map<String, String> subFieldsWithChildren = new HashMap<>();

            if (Collection.class.isAssignableFrom(fieldType)) {
                // 处理集合类型，获取泛型参数类型
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                        Class<?> elementType = (Class<?>) actualTypeArguments[0];
                        // 对于集合中的元素类型，递归处理其@TableProperty注解
                        // 父节点有注解，所以子节点需要拼接
                        getTablePropertyFieldsMapRecursive(elementType, subFields, fullKey, fullValue, depthMap, subFieldsWithChildren, true);
                    }
                }
            } else {
                // 处理普通复杂类型
                // 父节点有注解，所以子节点需要拼接
                getTablePropertyFieldsMapRecursive(fieldType, subFields, fullKey, fullValue, depthMap, subFieldsWithChildren, true);
            }

            // 将子字段添加到结果中
            result.putAll(subFields);

            // 如果有子字段，则标记当前字段为有子级（需要排除）
            if (!subFields.isEmpty()) {
                fieldsWithChildren.put(fullKey, fullValue);
                // 同时将子字段中需要排除的也添加到当前需要排除的集合中
                fieldsWithChildren.putAll(subFieldsWithChildren);
            } else {
                // 如果没有子字段（如循环引用字段），则直接添加该字段
                result.put(fullKey, new FieldProperty(fullValue, tableProperty));
            }
        } else {
            // 简单类型直接添加
            result.put(fullKey, new FieldProperty(fullValue, tableProperty));
        }
    }

    /**
     * 处理没有@TableProperty注解的字段
     * 但仍需要递归处理复杂类型字段以确保子节点有注解的字段能被获取到
     *
     * @param field 当前处理的字段
     * @param keyPrefix key的前缀（使用注解中的值构建）
     * @param valuePrefix value的前缀（使用字段实际名称构建）
     * @param result 存储结果的Map
     * @param depthMap 记录每个类的递归深度，用于检测循环引用
     * @param fieldsWithChildren 有子级的字段集合
     * @param parentHasAnnotation 父节点是否有注解
     */
    private static void processFieldWithoutAnnotation(Field field, String keyPrefix, String valuePrefix, Map<String, FieldProperty> result, Map<Class<?>, Integer> depthMap, Map<String, String> fieldsWithChildren, boolean parentHasAnnotation) {
        // 即使字段没有注解，也要处理复杂类型字段
        Class<?> fieldType = field.getType();
        if (isComplexType(fieldType)) {
            // 构建新的前缀
            String newKeyPrefix;
            String newValuePrefix = valuePrefix.isEmpty() ? field.getName() : valuePrefix + "." + field.getName();
            
            // 关键修改：如果父节点没有注解，则不拼接父节点字段名
            if (parentHasAnnotation) {
                // 父节点有注解，需要拼接
                newKeyPrefix = keyPrefix.isEmpty() ? field.getName() : keyPrefix + "." + field.getName();
            } else {
                // 父节点没有注解，不拼接，保持空字符串或使用子节点自己的key
                newKeyPrefix = "";
            }

            // 用于收集子字段
            Map<String, FieldProperty> subFields = new HashMap<>();
            Map<String, String> subFieldsWithChildren = new HashMap<>();

            if (Collection.class.isAssignableFrom(fieldType)) {
                // 处理集合类型，获取泛型参数类型
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                        Class<?> elementType = (Class<?>) actualTypeArguments[0];
                        // 对于集合中的元素类型，递归处理其@TableProperty注解
                        // 父节点没有注解，所以子节点不需要拼接
                        getTablePropertyFieldsMapRecursive(elementType, subFields, newKeyPrefix, newValuePrefix, depthMap, subFieldsWithChildren, false);
                    }
                }
            } else {
                // 处理普通复杂类型
                // 父节点没有注解，所以子节点不需要拼接
                getTablePropertyFieldsMapRecursive(fieldType, subFields, newKeyPrefix, newValuePrefix, depthMap, subFieldsWithChildren, false);
            }

            // 将子字段添加到结果中
            result.putAll(subFields);

            // 如果有子字段，则标记当前字段路径为有子级（需要排除）
            if (!subFields.isEmpty()) {
                // 注意：这里我们不使用注解值作为key，因为当前字段没有注解
                String currentPath = keyPrefix.isEmpty() ? field.getName() : keyPrefix + "." + field.getName();
                fieldsWithChildren.put(currentPath, newValuePrefix);
                // 同时将子字段中需要排除的也添加到当前需要排除的集合中
                fieldsWithChildren.putAll(subFieldsWithChildren);
            }
        }
        // 简单类型且没有注解的字段不需要处理
    }

    /**
     * 判断是否为复杂类型（非基本类型、包装类型或String）
     *
     * 此方法用于判断一个类是否为复杂类型，即需要进一步处理的类型。
     * 复杂类型通常包含嵌套字段，需要递归处理其内部结构。
     *
     * @param clazz 要判断的类
     * @return 是否为复杂类型
     */
    private static boolean isComplexType(Class<?> clazz) {
        // 基本类型
        if (clazz.isPrimitive()) {
            return false;
        }

        // 常见的包装类型和String类型
        return !(clazz.equals(String.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(java.util.Date.class) ||
                clazz.equals(java.time.LocalDate.class) ||
                clazz.equals(java.time.LocalDateTime.class));
    }

    /**
     * 从字段属性映射中提取表头列表
     *
     * 此方法根据字段的@TableProperty注解中的order属性对字段进行排序，
     * 返回按顺序排列的表头列表，用于数据展示时的列顺序。
     *
     * @param fieldsMap 字段属性映射
     * @return 按顺序排列的表头列表
     */
    @NotNull
    public static List<String> getHeaders(Map<String, FieldProperty> fieldsMap) {
        return fieldsMap.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().getTableProperty().order()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static <T> TableConf getTableConf(Class<T> clazz) {
        TableConf tableConf;
        if (clazz.isAnnotationPresent(TableConf.class)) {
            tableConf = clazz.getAnnotation(TableConf.class);
        } else {
            tableConf = new TableConf() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return TableConf.class;
                }

                @Override
                public int headLine() {
                    return 1;
                }

                @Override
                public int titleRow() {
                    return 1;
                }

                @Override
                public boolean enableCover() {
                    return false;
                }

                @Override
                public boolean isText() {
                    return false;
                }
                @Override
                public boolean enableDesc() {
                    return false;
                }

                @Override
                public String headFontColor() {
                    return "#ffffff";
                }

                @Override
                public String headBackColor() {
                    return "#000000";
                }
            };
        }
        return tableConf;
    }
}