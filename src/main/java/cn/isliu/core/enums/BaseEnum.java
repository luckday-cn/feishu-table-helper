package cn.isliu.core.enums;

import java.util.Arrays;

public interface BaseEnum {

    /**
     * 获取枚举代码
     */
    String getCode();
    
    /**
     * 获取枚举描述
     */
    String getDesc();

    /**
     * 根据描述获取枚举实例
     *
     * @param enumClass 枚举类
     * @param desc      描述
     * @param <T>       枚举类型
     * @return 枚举实例
     */
    static <T extends BaseEnum> T getByDesc(Class<T> enumClass, Object desc) {
        if (desc == null) {
            return null;
        }

        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getDesc().equals(desc.toString()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据代码获取枚举实例
     *
     * @param enumClass 枚举类
     * @param code      代码
     * @param <T>       枚举类型
     * @return 枚举实例
     */
    static <T extends BaseEnum> T  getByCode(Class<T> enumClass, Object code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getCode().equals(code.toString()))
                .findFirst()
                .orElse(null);
    }
}