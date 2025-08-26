package cn.isliu.core.annotation;

import java.lang.annotation.*;

/**
 * 表格配置注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TableConf {
    /**
     * 表头行数
     *
     * @return 表头行数
     */
    int headLine() default 1;

    /**
     * 标题行数
     *
     * @return 标题行数
     */
    int titleRow() default 1;

    /**
     * 是否覆盖已存在数据
     *
     * @return 是否覆盖
     */
    boolean enableCover() default false;

    /**
     * 是否设置表格为纯文本
     *
     * @return 是否设置表格为纯文本
     */
    boolean isText() default false;

    /**
     * 是否开启字段描述
     *
     * @return 是否开启字段描述
     */
    boolean enableDesc() default false;

    /**
     * 字体颜色
     *
     * @return 字体颜色
     */
    String headFontColor() default "#000000";

    /**
     * 背景颜色
     *
     * @return 背景颜色
     */
    String headBackColor() default "#cccccc";
}