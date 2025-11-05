package cn.isliu.core.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Map方式表格配置类
 * 
 * 用于替代注解配置，支持使用Map方式操作飞书表格
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapTableConfig {

    /**
     * 标题行行号（从1开始）
     */
    private int titleRow = 1;

    /**
     * 数据起始行行号（从1开始）
     */
    private int headLine = 1;

    /**
     * 唯一键字段名列表
     */
    private Set<String> uniKeyNames = new HashSet<>();

    /**
     * 是否覆盖已存在数据
     */
    private boolean enableCover = false;

    /**
     * 是否忽略未找到的数据
     */
    private boolean ignoreNotFound = false;

    /**
     * 是否启用 Upsert 模式
     * true（默认）：根据唯一键匹配，存在则更新，不存在则追加
     * false：不匹配唯一键，所有数据直接追加到表格末尾
     */
    private boolean upsert = true;

    /**
     * 字段位置映射 (字段名 -> 列位置，如 "添加SPU" -> "A")
     */
    private Map<String, String> fieldsPositionMap = new HashMap<>();

    /**
     * 获取标题行行号
     *
     * @return 标题行行号
     */
    public int getTitleRow() {
        return titleRow;
    }

    /**
     * 设置标题行行号
     *
     * @param titleRow 标题行行号
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setTitleRow(int titleRow) {
        this.titleRow = titleRow;
        return this;
    }

    /**
     * 获取数据起始行行号
     *
     * @return 数据起始行行号
     */
    public int getHeadLine() {
        return headLine;
    }

    /**
     * 设置数据起始行行号
     *
     * @param headLine 数据起始行行号
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setHeadLine(int headLine) {
        this.headLine = headLine;
        return this;
    }

    /**
     * 获取唯一键字段名集合
     *
     * @return 唯一键字段名集合
     */
    public Set<String> getUniKeyNames() {
        return uniKeyNames;
    }

    /**
     * 设置唯一键字段名集合
     *
     * @param uniKeyNames 唯一键字段名集合
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setUniKeyNames(Set<String> uniKeyNames) {
        this.uniKeyNames = uniKeyNames;
        return this;
    }

    /**
     * 添加唯一键字段名
     *
     * @param uniKeyName 唯一键字段名
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig addUniKeyName(String uniKeyName) {
        this.uniKeyNames.add(uniKeyName);
        return this;
    }

    /**
     * 是否覆盖已存在数据
     *
     * @return true表示覆盖，false表示不覆盖
     */
    public boolean isEnableCover() {
        return enableCover;
    }

    /**
     * 设置是否覆盖已存在数据
     *
     * @param enableCover true表示覆盖，false表示不覆盖
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setEnableCover(boolean enableCover) {
        this.enableCover = enableCover;
        return this;
    }

    /**
     * 是否忽略未找到的数据
     *
     * @return true表示忽略，false表示不忽略
     */
    public boolean isIgnoreNotFound() {
        return ignoreNotFound;
    }

    /**
     * 设置是否忽略未找到的数据
     *
     * @param ignoreNotFound true表示忽略，false表示不忽略
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setIgnoreNotFound(boolean ignoreNotFound) {
        this.ignoreNotFound = ignoreNotFound;
        return this;
    }


    /**
     * 是否启用 Upsert 模式
     *
     * @return true 为 Upsert 模式，false 为纯追加模式
     */
    public boolean isUpsert() {
        return upsert;
    }

    /**
     * 设置是否启用 Upsert 模式
     *
     * true（默认）：根据唯一键匹配，存在则更新，不存在则追加
     * false：不匹配唯一键，所有数据直接追加到表格末尾
     *
     * @param upsert true 为 Upsert 模式，false 为纯追加模式
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setUpsert(boolean upsert) {
        this.upsert = upsert;
        return this;
    }

    /**
     * 获取字段位置映射
     *
     * @return 字段位置映射
     */
    public Map<String, String> getFieldsPositionMap() {
        return fieldsPositionMap;
    }

    /**
     * 设置字段位置映射
     *
     * @param fieldsPositionMap 字段位置映射
     * @return MapTableConfig实例，支持链式调用
     */
    public MapTableConfig setFieldsPositionMap(Map<String, String> fieldsPositionMap) {
        this.fieldsPositionMap = fieldsPositionMap;
        return this;
    }

    /**
     * 创建默认配置
     *
     * @return 默认配置实例
     */
    public static MapTableConfig createDefault() {
        return new MapTableConfig();
    }

    /**
     * 创建配置构建器
     *
     * @return 配置构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 配置构建器
     */
    public static class Builder {
        private final MapTableConfig config = new MapTableConfig();

        /**
         * 设置标题行行号
         *
         * @param titleRow 标题行行号
         * @return Builder实例
         */
        public Builder titleRow(int titleRow) {
            config.titleRow = titleRow;
            return this;
        }

        /**
         * 设置数据起始行行号
         *
         * @param headLine 数据起始行行号
         * @return Builder实例
         */
        public Builder headLine(int headLine) {
            config.headLine = headLine;
            return this;
        }

        /**
         * 设置唯一键字段名集合
         *
         * @param uniKeyNames 唯一键字段名集合
         * @return Builder实例
         */
        public Builder uniKeyNames(Set<String> uniKeyNames) {
            config.uniKeyNames = new HashSet<>(uniKeyNames);
            return this;
        }

        /**
         * 添加唯一键字段名
         *
         * @param uniKeyName 唯一键字段名
         * @return Builder实例
         */
        public Builder addUniKeyName(String uniKeyName) {
            config.uniKeyNames.add(uniKeyName);
            return this;
        }

        /**
         * 设置是否覆盖已存在数据
         *
         * @param enableCover true表示覆盖，false表示不覆盖
         * @return Builder实例
         */
        public Builder enableCover(boolean enableCover) {
            config.enableCover = enableCover;
            return this;
        }

        /**
         * 设置是否忽略未找到的数据
         *
         * @param ignoreNotFound true表示忽略，false表示不忽略
         * @return Builder实例
         */
        public Builder ignoreNotFound(boolean ignoreNotFound) {
            config.ignoreNotFound = ignoreNotFound;
            return this;
        }

        /**
         * 设置是否启用 Upsert 模式
         *
         * true（默认）：根据唯一键匹配，存在则更新，不存在则追加
         * false：不匹配唯一键，所有数据直接追加到表格末尾
         *
         * @param upsert true 为 Upsert 模式，false 为纯追加模式
         * @return Builder实例
         */
        public Builder upsert(boolean upsert) {
            config.upsert = upsert;
            return this;
        }

        /**
         * 设置字段位置映射
         *
         * @param fieldsPositionMap 字段位置映射
         * @return Builder实例
         */
        public Builder fieldsPositionMap(Map<String, String> fieldsPositionMap) {
            config.fieldsPositionMap = new HashMap<>(fieldsPositionMap);
            return this;
        }

        /**
         * 构建配置对象
         *
         * @return MapTableConfig实例
         */
        public MapTableConfig build() {
            return config;
        }
    }
}

