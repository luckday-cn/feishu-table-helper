package cn.isliu.core.converters;

public interface FieldValueProcess<T> {

    T process(Object value);

    /**
     * 反向处理，将枚举值转换为原始值
     */
     Object reverseProcess(Object value);
}
