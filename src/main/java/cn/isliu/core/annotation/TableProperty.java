package cn.isliu.core.annotation;

import cn.isliu.core.converters.FieldValueProcess;
import cn.isliu.core.converters.OptionsValueProcess;
import cn.isliu.core.enums.BaseEnum;
import cn.isliu.core.enums.TypeEnum;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TableProperty {

    String value() default "";

    String field() default "";

    int order() default Integer.MAX_VALUE;

    TypeEnum type() default TypeEnum.TEXT;

    Class<? extends BaseEnum> enumClass() default BaseEnum.class;

    Class<? extends FieldValueProcess> fieldFormatClass() default FieldValueProcess.class;

    Class<? extends OptionsValueProcess> optionsClass() default OptionsValueProcess.class;
}
