package cn.isliu.core.utils;

import cn.isliu.core.annotation.TableProperty;
import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;
import cn.isliu.core.pojo.FieldProperty;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.lang.reflect.InvocationTargetException;

/**
 * 字段转换工具类
 * 
 * 提供将飞书表格数据转换为实体类字段值的工具方法，
 * 支持不同字段类型的转换处理
 */
public class ConvertFieldUtil {
    private static final Logger log = Logger.getLogger(ConvertFieldUtil.class.getName());
    private static final Gson gson = new Gson();

    /**
     * 将位置键转换为字段名
     * 
     * 根据字段属性映射关系，将表格中的位置键（如"A1"）转换为实体类字段名
     * 
     * @param jsonObject 包含位置键值对的JSON对象
     * @param fieldsMap 字段属性映射关系Map
     * @return 转换后的字段名值映射Map
     */
    public static Map<String, Object> convertPositionToField(JsonObject jsonObject, Map<String, FieldProperty> fieldsMap) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String positionKey = entry.getKey();
            FieldProperty fieldProperty = fieldsMap.get(positionKey);
            if (fieldProperty == null) continue;
            String fieldKey = fieldProperty.getField();
            TableProperty tableProperty = fieldProperty.getTableProperty();


            Object value = getValueByFieldRule(tableProperty, entry.getValue());
            if (fieldKey != null) {
                // 根据配置获取值
                result.put(fieldKey, value);
            } else {
                // 未找到对应配置项时保持原键（可选）
                result.put(positionKey, value);
            }
        }

        return result;
    }

    /**
     * 根据字段规则获取值
     * 
     * 根据字段类型和配置规则处理字段值
     * 
     * @param tableProperty 表格属性注解
     * @param value 原始值
     * @return 处理后的值
     */
    private static Object getValueByFieldRule(TableProperty tableProperty, JsonElement value) {
        if (tableProperty == null || value == null || value.isJsonNull()) {
            return null;
        }
        Object result = null;

        TypeEnum typeEnum = tableProperty.type();

        if (Objects.nonNull(typeEnum)) {
            switch (typeEnum) {
                case TEXT:
                case NUMBER:
                case DATE:
                    // 直接获取值，避免额外的引号
                    result = getJsonValue(value);
                    break;

                case SINGLE_SELECT:
                    List<String> arr = parseStrToArr(value);
                    result = conversionValue(tableProperty, arr.get(0));
                    break;

                case MULTI_TEXT:
                    result = parseStrToArr(value);
                    break;

                case MULTI_SELECT:
                    List<String> values = parseStrToArr(value);
                    result = values.stream()
                            .map(v -> conversionValue(tableProperty, v)).collect(Collectors.toList());
                    break;

                case TEXT_URL:
                    result = getTextUrl(value);
                    break;

                case TEXT_FILE:
                    result = conversionValue(tableProperty, getJsonValue(value));
                    break;
            }
        }

        return result;
    }

    /**
     * 获取文本链接
     * 
     * 从JSON元素中提取文本链接信息
     * 
     * @param value JSON元素
     * @return 文本链接列表
     */
    private static Object getTextUrl(JsonElement value) {
        if (value instanceof JsonArray) {
            List<String> fileUrls = new ArrayList<>();
            JsonArray arr = (JsonArray) value;
            for (int i = 0; i < arr.size(); i++) {
                JsonElement jsonElement = arr.get(i);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    String url = getUrlByTextFile(jsonObject);
                    if (!url.isEmpty()) {
                        fileUrls.add(url);
                    }
                }
            }
            return String.join(",", fileUrls);
        } else if (value instanceof JsonObject) {
            JsonObject jsb = (JsonObject) value;
            return getUrlByTextFile(jsb);
        }
        return value;
    }

    /**
     * 从JsonElement中提取合适的值，避免额外的引号
     */
    private static Object getJsonValue(JsonElement value) {
        if (value.isJsonPrimitive()) {
            // 检查是否为字符串
            if (value.getAsJsonPrimitive().isString()) {
                String strValue = value.getAsString();
                // 检查字符串是否以引号开始和结束，如果是则去除引号
                if (strValue.length() >= 2 &&
                        ((strValue.startsWith("\"") && strValue.endsWith("\"")) ||
                                (strValue.startsWith("'") && strValue.endsWith("'")))) {
                    return strValue.substring(1, strValue.length() - 1);
                }
                return strValue;
            } else if (value.getAsJsonPrimitive().isNumber()) {
                return value.getAsNumber();
            } else if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean();
            }
        }
        return value;
    }

    private static String getUrlByTextFile(JsonObject jsb) {
        String url = "";
        String cellType = jsb.get("type").getAsString();

        if (cellType.equals("url")) {
            String link = jsb.get("link").getAsString();
            if (link == null) {
                url = jsb.get("text").getAsString();
            } else {
                url = link;
            }
        }
        return url;
    }

    public static List<String> parseStrToArr(JsonElement value) {
        String result = "";
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isString()) {
                result = value.getAsString();
                // 检查字符串是否以引号开始和结束，如果是则去除引号
                if (result.length() >= 2 &&
                        ((result.startsWith("\"") && result.endsWith("\"")) ||
                                (result.startsWith("'") && result.endsWith("'")))) {
                    result = result.substring(1, result.length() - 1);
                }
            } else {
                result = value.toString();
            }
        } else if (value.isJsonArray()) {
            // 处理数组类型
            result = value.toString();
        } else {
            result = value.toString();
        }

        String[] split = result.split(",");
        return new ArrayList<>(Arrays.asList(split));
    }

    private static Object conversionValue(TableProperty tableProperty, Object value) {
        Object result = value;
        if (value != null) {
            Class<? extends BaseEnum> enumClass = tableProperty.enumClass();
            if (enumClass != null && enumClass != BaseEnum.class) {
                BaseEnum baseEnum = BaseEnum.getByDesc(enumClass, value);
                if (baseEnum != null) {
                    result = baseEnum.getCode();
                }
            }

            Class<? extends FieldValueProcess> fieldFormatClass = tableProperty.fieldFormatClass();
            if (fieldFormatClass != null && !fieldFormatClass.isInterface()) {
                try {
                    // 使用更安全的实例化方式
                    FieldValueProcess fieldValueProcess = fieldFormatClass.getDeclaredConstructor().newInstance();
                    result = fieldValueProcess.process(result);
                } catch (InstantiationException e) {
                    log.log(Level.SEVERE, "无法实例化字段格式化类: " + fieldFormatClass.getName(), e);
                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE, "无法访问字段格式化类的构造函数: " + fieldFormatClass.getName(), e);
                } catch (NoSuchMethodException e) {
                    log.log(Level.SEVERE, "字段格式化类缺少无参构造函数: " + fieldFormatClass.getName(), e);
                } catch (InvocationTargetException e) {
                    log.log(Level.SEVERE, "字段格式化类构造函数调用异常: " + fieldFormatClass.getName(), e);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "创建字段格式化类实例时发生未知异常: " + fieldFormatClass.getName(), e);
                }
            }
        }
        return result;
    }

    public static Object reverseValueConversion(TableProperty tableProperty, Object value) {
        Object result = value;
        if (value != null && tableProperty != null) {
            Class<? extends BaseEnum> enumClass = tableProperty.enumClass();
            if (enumClass != null && enumClass != BaseEnum.class) {
                BaseEnum baseEnum = BaseEnum.getByCode(enumClass, value);
                if (baseEnum != null) {
                    result = baseEnum.getDesc();
                }
            }

            Class<? extends FieldValueProcess> fieldFormatClass = tableProperty.fieldFormatClass();
            if (fieldFormatClass != null && !fieldFormatClass.isInterface()) {
                try {
                    FieldValueProcess fieldValueProcess = fieldFormatClass.newInstance();
                    result = fieldValueProcess.reverseProcess(result);
                } catch (InstantiationException | IllegalAccessException e) {
                    log.log(Level.FINE, "format value error", e);
                }
            }
        }
        return result;
    }
}