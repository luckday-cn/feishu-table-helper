package cn.isliu.core.converters;

public interface OptionsValueProcess<T, R> {

    T process(R r);

}
