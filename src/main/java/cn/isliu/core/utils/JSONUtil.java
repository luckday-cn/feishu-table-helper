package cn.isliu.core.utils;

import com.google.gson.*;

import java.util.HashMap;
import java.util.Map;

public class JSONUtil {

    private static final Gson gson = new Gson();

    /**
     * 手动将HashMap转换为JsonObject，避免Gson添加额外引号
     * @param data HashMap数据
     * @return 转换后的JsonObject
     */
    public static JsonObject convertMapToJsonObject(Map<String, Object> data) {
        JsonObject jsonObject = new JsonObject();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 根据值的类型添加到JsonObject中
            if (value instanceof String) {
                // 检查字符串是否已经包含引号，如果是则去除
                String strValue = (String) value;
                if (strValue.length() >= 2 && strValue.startsWith("\"") && strValue.endsWith("\"")) {
                    strValue = strValue.substring(1, strValue.length() - 1);
                }
                jsonObject.addProperty(key, strValue);
            } else if (value instanceof Number) {
                jsonObject.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                jsonObject.addProperty(key, (Boolean) value);
            } else if (value instanceof Character) {
                jsonObject.addProperty(key, (Character) value);
            } else if (value == null) {
                jsonObject.add(key, null);
            } else {
                // 对于其他类型，使用Gson转换
                JsonElement element = gson.toJsonTree(value);
                jsonObject.add(key, element);
            }
        }
        return jsonObject;
    }

    public static boolean isValidJson(String json) {
        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
}
