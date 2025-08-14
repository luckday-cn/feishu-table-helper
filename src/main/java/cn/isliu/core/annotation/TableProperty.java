package cn.isliu.core.annotation;

import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;

import java.lang.annotation.*;

/**
 * 表格属性注解
 * 
 * 用于标记实体类字段与飞书表格列的映射关系，
 * 支持配置列名、字段类型、枚举类、格式化处理类等属性。
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TableProperty {

    /**
     * 表格列名
     * 
     * @return 列名字符串
     */
    String value() default "";

    /**
     * 字段名
     * 
     * @return 字段名字符串
     */
    String field() default "";

    /**
     * 字段排序顺序
     * 
     * @return 排序值，数值越小越靠前
     */
    int order() default Integer.MAX_VALUE;

    /**
     * 字段类型
     * 
     * @return 字段类型枚举
     */
    TypeEnum type() default TypeEnum.TEXT;

    /**
     * 枚举类
     * 
     * 用于 SINGLE_SELECT 和 MULTI_SELECT 类型的字段
     * @return 枚举类Class对象
     */
    Class<? extends BaseEnum> enumClass() default BaseEnum.class;

    /**
     * 字段格式化处理类
     * 
     * 用于自定义字段值的处理逻辑
     * @return 字段值处理类Class对象
     */
    Class<? extends FieldValueProcess> fieldFormatClass() default FieldValueProcess.class;

    /**
     * 选项处理类
     * 
     * 用于处理下拉选项等特殊字段类型
     * @return 选项值处理类Class对象
     */
    Class<? extends OptionsValueProcess> optionsClass() default OptionsValueProcess.class;
}