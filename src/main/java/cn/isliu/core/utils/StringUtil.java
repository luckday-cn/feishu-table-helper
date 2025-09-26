package cn.isliu.core.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;

public class StringUtil {

    public static String mapToJson (Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        Object[] keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            String key = (String) keys[i];
            Object value = map.get(key);
            jsonBuilder.append("\"").append(key).append("\":");

            if (value instanceof String) {
                jsonBuilder.append("\"").append(value).append("\"");
            } else {
                jsonBuilder.append(value);
            }

            if (i < keys.length - 1) {
                jsonBuilder.append(",");
            }
        }
        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }

    public static String getSHA256(String str) {
        String uniqueId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(str.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            uniqueId = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
        return uniqueId;
    }

    /**
     * 驼峰转下划线命名
     * @param camelCaseName 驼峰命名的字符串
     * @return 下划线命名的字符串
     */
    public static String toUnderscoreCase(String camelCaseName) {
        if (camelCaseName == null || camelCaseName.isEmpty()) {
            return camelCaseName;
        }

        StringBuilder result = new StringBuilder();
        for (char c : camelCaseName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }


    /**
     * 下划线转驼峰命名
     * @param underscoreName 下划线命名的字符串
     * @return 驼峰命名的字符串
     */
    public static String toCamelCase(String underscoreName) {
        if (underscoreName == null || underscoreName.isEmpty()) {
            return underscoreName;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : underscoreName.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * 将集合转换为逗号分隔的字符串
     * @param collection 集合对象
     * @return 逗号分隔的字符串
     */
    public static String joinWithComma(Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            sb.append(obj != null ? obj.toString() : "");
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 将数组转换为逗号分隔的字符串
     * @param array 数组对象
     * @return 逗号分隔的字符串
     */
    public static String joinWithComma(Object[] array) {
        if (array == null || array.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i] != null ? array[i].toString() : "");
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

}
