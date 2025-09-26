package cn.isliu.core.utils;

import cn.isliu.core.FileData;
import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.FileType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;
import cn.isliu.core.logging.FsLogger;
import cn.isliu.core.enums.ErrorCode;
import java.util.stream.Collectors;

/**
 * 实例生成工具类
 * 
 * 提供根据数据映射关系生成实体类实例的工具方法，
 * 支持嵌套对象和集合类型的处理
 */
public class GenerateUtil {

    // 使用统一的FsLogger替代java.util.logging.Logger
    /**
     * 根据配置和数据生成DTO对象（通用版本）
     *
     * @param fieldPathList 字段路径列表
     * @param clazz 实体类Class对象
     * @param dataMap 数据映射Map
     * @param <T> 实体类泛型
     * @return 实体类实例
     */
    public static <T> T generateInstance(List<String> fieldPathList, Class<T> clazz, Map<String, Object> dataMap) {
        T t;
        try {
            t = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建实例失败: " + clazz.getSimpleName(), e);
        }

        fieldPathList.forEach(fieldPath -> {
            Object value = dataMap.get(fieldPath);

            if (value != null) {
                try {
                    setNestedField(t, fieldPath, value);
                } catch (Exception e) {
                    FsLogger.error(ErrorCode.DATA_CONVERSION_ERROR, "【飞书助手】 获取字段值异常！参数：" + fieldPath + "，异常：" + e.getMessage(), "generateList", e);
                }
            }
        });

        return t;
    }

    /**
     * 递归设置嵌套字段值（支持List类型处理）
     *
     * @param target 目标对象
     * @param fieldPath 字段路径
     * @param value 字段值
     * @throws Exception 设置字段时可能抛出的异常
     */
    private static void setNestedField(Object target, String fieldPath, Object value)
            throws Exception {
        String[] parts = fieldPath.split("\\.");
        setNestedFieldRecursive(target, parts, 0, value);
    }

    /**
     * 递归设置嵌套字段值
     *
     * @param target 目标对象
     * @param parts 字段路径分段数组
     * @param index 当前处理的字段索引
     * @param value 字段值
     * @throws Exception 设置字段时可能抛出的异常
     */
    private static void setNestedFieldRecursive(Object target, String[] parts, int index, Object value)
            throws Exception {
        if (index >= parts.length - 1) {
            // 最后一级字段，直接设置值
            setFieldValue(target, parts[index], value);
            return;
        }

        String fieldName = parts[index];
        Field field = getDeclaredField(target.getClass(), fieldName);
        if (field == null) {
            // 尝试使用下划线转驼峰的方式查找字段
            field = getDeclaredField(target.getClass(), StringUtil.toCamelCase(fieldName));
            if (field == null) {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
        }

        field.setAccessible(true);
        Object nestedObj = field.get(target);

        // 处理List类型
        if (List.class.isAssignableFrom(field.getType())) {
            // 确保List存在
            if (nestedObj == null) {
                nestedObj = new ArrayList<>();
                field.set(target, nestedObj);
            }

            List<Object> list = (List<Object>) nestedObj;
            // 确保List中至少有一个元素
            if (list.isEmpty()) {
                Object newElement = createListElement(field);
                list.add(newElement);
            }

            // 使用List中的第一个元素
            nestedObj = list.get(0);
        }
        // 处理普通对象
        else {
            // 确保嵌套对象存在
            if (nestedObj == null) {
                // 通过反射创建嵌套对象实例
                try {
                    nestedObj = field.getType().getDeclaredConstructor().newInstance();
                    field.set(target, nestedObj);
                } catch (InstantiationException e) {
                    // 如果无法创建实例，则记录日志并跳过该字段
                    FsLogger.warn("无法创建嵌套对象实例: {} , 字段: {}", field.getType().getName(), fieldName);
                    return;
                }
            }
        }

        // 递归处理下一级字段
        setNestedFieldRecursive(nestedObj, parts, index + 1, value);
    }

    /**
     * 创建List元素实例
     */
    private static Object createListElement(Field listField) throws Exception {
        Type genericType = listField.getGenericType();
        if (genericType instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                Class<?> elementClass = (Class<?>) typeArgs[0];
                // 通过反射创建List元素实例
                try {
                    return elementClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    // 如果无法创建实例，则记录日志并返回null
                    FsLogger.warn("无法创建List元素实例: {}", elementClass.getName());
                    return null;
                }
            }
        }
        throw new InstantiationException("Cannot determine list element type for field: " + listField.getName());
    }

    /**
     * 设置字段值（支持基本类型转换）
     */
    private static void setFieldValue(Object target, String fieldName, Object value)
            throws Exception {
        Field field = getDeclaredField(target.getClass(), fieldName);
        if (field == null) {
            // 尝试使用下划线转驼峰的方式查找字段
            field = getDeclaredField(target.getClass(), StringUtil.toCamelCase(fieldName));
            if (field == null) {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
        }

        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        // 简单类型转换
        if (value != null && value != "") {
            if (fieldType == String.class) {
                field.set(target, convertStrValue(value));
            } else if (fieldType == Integer.class || fieldType == int.class) {
                String val = convertValue(value);
                BigDecimal bd = new BigDecimal(val);
                bd = bd.setScale(0, BigDecimal.ROUND_DOWN);
                String result = bd.toPlainString();
                field.set(target, Integer.parseInt(result));
            } else if (fieldType == Double.class || fieldType == double.class) {
                field.set(target, Double.parseDouble(convertValue(value)));
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                field.set(target, Boolean.parseBoolean(convertValue(value)));
            } else if (fieldType == Long.class || fieldType == long.class) {
                String stringValue = convertValue(value);
                try {
                    field.set(target, Long.parseLong(stringValue));
                } catch (NumberFormatException e) {
                    try {
                        field.set(target, ((Double) Double.parseDouble(stringValue)).longValue());
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("无法将值 '" + stringValue + "' 转换为 long 或科学计数法表示的数值", ex);
                    }
                }

            } else if (fieldType == List.class) {
                // 获取泛型类型
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType)) {
                    throw new IllegalArgumentException("无法获取字段的泛型信息：" + fieldName);
                }

                Type elementType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (!(elementType instanceof Class)) {
                    throw new IllegalArgumentException("不支持非Class类型的泛型：" + elementType);
                }

                Class<?> elementClass = (Class<?>) elementType;

                List<Object> convertedList = new ArrayList<>();

                if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        convertedList.add(convertValue(item, elementClass));
                    }
                } else {
                    // 单个值转为单元素List，并做类型转换
                    convertedList.add(convertValue(value, elementClass));
                }

                field.set(target, convertedList);
            }  // 新增：枚举类型支持
            else if (BaseEnum.class.isAssignableFrom(fieldType)) {
                if (value instanceof String) {
                    field.set(target, parseEnum((Class<? extends BaseEnum>) fieldType, (String) value));
                }
            }
            else {
                // 其他类型直接设置
                field.set(target, value);
            }
        } else {
            field.set(target, null);
        }
    }

    /**
     * 获取字段（包括父类）
     */
    private static Field getDeclaredField(Class<?> clazz, String fieldName) {
        // 首先尝试直接查找字段（驼峰命名）
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // 继续在父类中查找
            }
        }

        // 如果直接查找失败，尝试使用下划线转驼峰的方式查找字段
        String camelCaseFieldName = StringUtil.toCamelCase(fieldName);
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(camelCaseFieldName);
            } catch (NoSuchFieldException e) {
                // 继续在父类中查找
            }
        }

        return null;
    }

    private static String convertStrValue(Object value) {
        String result = "";
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            result = bigDecimal.stripTrailingZeros().toPlainString();
        } else if (value instanceof Double) {
            String stringValue = convertValue(value);
            try {
                result = String.valueOf(Long.parseLong(stringValue));
            } catch (NumberFormatException e) {
                try {
                    result = String.valueOf(((Double) Double.parseDouble(stringValue)).longValue());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("无法将值 '" + stringValue + "' 转换为 long 或科学计数法表示的数值", ex);
                }
            }
        } else {
            result = String.valueOf(value);
        }
        return result.trim();
    }

    private static String convertValue(Object value) {
        String result = "";
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            result = bigDecimal.stripTrailingZeros().toPlainString();
        } else {
            result = String.valueOf(value);
        }
        return result.trim();
    }

    private static Object convertValue(Object value, Class<?> targetType) throws Exception {
        if (value == null) return null;

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString().trim();
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value.toString().trim());
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value.toString().trim());
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value.toString().trim());
        } else if (BaseEnum.class.isAssignableFrom(targetType)) {
            if (value instanceof String) {
                return parseEnum((Class<? extends BaseEnum>) targetType, (String) value);
            } else {
                throw new IllegalArgumentException("枚举类型字段只接受字符串类型的值");
            }
        } else {
            throw new IllegalArgumentException("不支持的List元素类型转换：" + targetType.getName());
        }
    }

    public static <T extends BaseEnum> T parseEnum(Class<T> enumClass, String value) {
        String val = value.trim();
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getCode().equals(val) || e.getDesc().equals(val))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching enum for value: " + val));
    }

    /**
     * 根据实体类以及字段映射关系，获取该字段的值 field 为："field.field2.field3"
     *
     * @param target 实体类对象
     * @param fieldMap 字段映射关系，key为注解值，value为实际字段路径
     * @return 字段值
     */
    public static Map<String, Object> getFieldValue(Object target, Map<String, String> fieldMap) {
        // 遍历字段映射关系，获取字段值
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            String fieldPath = entry.getValue();

            try {
                Object value = getNestedFieldValue(target, fieldPath);
                if (value != null) {
                    result.put(fieldName, value);
                }
            } catch (Exception e) {
                FsLogger.warn("获取字段值异常，字段路径：{}", fieldPath);
            }
        }
        return result;
    }

    /**
     * 递归获取嵌套字段值（支持List类型处理）
     */
    public static Object getNestedFieldValue(Object target, String fieldPath) throws Exception {
        String[] parts = fieldPath.split("\\.");
        return getNestedFieldValueRecursive(target, parts, 0);
    }

    private static Object getNestedFieldValueRecursive(Object target, String[] parts, int index) throws Exception {
        if (target == null) {
            return null;
        }

        if (index >= parts.length - 1) {
            // 最后一级字段，直接获取值
            return getFieldValue(target, parts[index]);
        }

        String fieldName = parts[index];
        Field field = getDeclaredField(target.getClass(), fieldName);
        if (field == null) {
            // 尝试使用下划线转驼峰的方式查找字段
            field = getDeclaredField(target.getClass(), StringUtil.toCamelCase(fieldName));
            if (field == null) {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
        }

        field.setAccessible(true);
        Object nestedObj = field.get(target);

        // 处理List类型
        if (nestedObj instanceof List) {
            List<?> list = (List<?>) nestedObj;
            if (!list.isEmpty()) {
                // 使用List中的第一个元素
                nestedObj = list.get(0);
            } else {
                return null;
            }
        }

        // 递归处理下一级
        return getNestedFieldValueRecursive(nestedObj, parts, index + 1);
    }

    /**
     * 获取字段值
     */
    private static Object getFieldValue(Object target, String fieldName) throws Exception {
        Field field = getDeclaredField(target.getClass(), fieldName);
        if (field == null) {
            // 尝试使用下划线转驼峰的方式查找字段
            field = getDeclaredField(target.getClass(), StringUtil.toCamelCase(fieldName));
            if (field == null) {
                throw new NoSuchFieldException("Field not found: " + fieldName);
            }
        }

        field.setAccessible(true);
        Object newObject = field.get(target);
        // 处理List类型
        if (newObject instanceof List) {
            List<?> list = (List<?>) newObject;
            if (!list.isEmpty()) {
                Field finalField = field;
                newObject = list.stream().map(obj -> ConvertFieldUtil.reverseValueConversion(finalField.getAnnotation(TableProperty.class), obj))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            newObject =  ConvertFieldUtil.reverseValueConversion(field.getAnnotation(TableProperty.class), newObject);
        }
        return newObject;
    }

    public static Object getRowData(Object fieldValue) {
        if (fieldValue instanceof FileData) {
            FileData fileData = (FileData) fieldValue;
            if (Objects.equals(fileData.getFileType(), FileType.IMAGE.getType())) {
                return null;
            } else if (Objects.equals(fileData.getFileType(), FileType.FILE.getType())) {
                return fileData.getFileUrl();
            }
        }

        if (fieldValue instanceof List) {
            Map<String, Object> params = new HashMap<>();
            params.put("values", fieldValue);
            params.put("type", "multipleValue");
            return params;
        }
        return fieldValue;
    }

    public static <T> String getUniqueId(T data) {
        String uniqueId = null;
        try {
            Object uniqueIdObj = GenerateUtil.getNestedFieldValue(data, "uniqueId");
            if (uniqueIdObj != null) {
                uniqueId = uniqueIdObj.toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return uniqueId;
    }

}