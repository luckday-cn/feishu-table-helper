package cn.isliu.core.config;

import java.util.*;

/**
 * Map方式表格创建配置类
 * 
 * 继承 MapTableConfig，专门用于创建飞书表格
 * 相比父类增加了字段定义、样式配置等创建表格所需的属性
 * 
 * @author Ls
 * @since 2025-10-16
 */
public class MapSheetConfig extends MapTableConfig {

    /**
     * 字段定义列表
     */
    private List<MapFieldDefinition> fields = new ArrayList<>();

    /**
     * 表头字体颜色（十六进制，如 #ffffff）
     */
    private String headFontColor = "#000000";

    /**
     * 表头背景颜色（十六进制，如 #000000）
     */
    private String headBackColor = "#cccccc";

    /**
     * 是否将单元格设置为纯文本格式
     */
    private boolean isText = false;

    /**
     * 是否启用字段描述行
     */
    private boolean enableDesc = false;

    public List<MapFieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<MapFieldDefinition> fields) {
        this.fields = fields;
    }

    public String getHeadFontColor() {
        return headFontColor;
    }

    public void setHeadFontColor(String headFontColor) {
        this.headFontColor = headFontColor;
    }

    public String getHeadBackColor() {
        return headBackColor;
    }

    public void setHeadBackColor(String headBackColor) {
        this.headBackColor = headBackColor;
    }

    public boolean isText() {
        return isText;
    }

    public void setText(boolean text) {
        isText = text;
    }

    public boolean isEnableDesc() {
        return enableDesc;
    }

    public void setEnableDesc(boolean enableDesc) {
        this.enableDesc = enableDesc;
    }

    public List<String> getGroupFields() {
        return groupFields;
    }

    public void setGroupFields(List<String> groupFields) {
        this.groupFields = groupFields;
    }

    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * 分组字段列表（用于创建分组表格）
     */
    private List<String> groupFields = new ArrayList<>();

    /**
     * 自定义属性映射（用于传递额外配置）
     */
    private Map<String, Object> customProperties = new HashMap<>();

    /**
     * 创建默认配置
     *
     * @return 默认配置实例
     */
    public static MapSheetConfig createDefault() {
        return new MapSheetConfig();
    }

    /**
     * 创建表格配置构建器
     *
     * @return 配置构建器实例
     */
    public static SheetBuilder sheetBuilder() {
        return new SheetBuilder();
    }

    /**
     * 添加单个字段
     *
     * @param field 字段定义
     * @return MapSheetConfig实例，支持链式调用
     */
    public MapSheetConfig addField(MapFieldDefinition field) {
        this.fields.add(field);
        return this;
    }

    /**
     * 批量添加字段
     *
     * @param fields 字段定义列表
     * @return MapSheetConfig实例，支持链式调用
     */
    public MapSheetConfig addFields(List<MapFieldDefinition> fields) {
        this.fields.addAll(fields);
        return this;
    }

    /**
     * 批量添加字段（可变参数）
     *
     * @param fields 字段定义可变参数
     * @return MapSheetConfig实例，支持链式调用
     */
    public MapSheetConfig addFields(MapFieldDefinition... fields) {
        this.fields.addAll(Arrays.asList(fields));
        return this;
    }

    /**
     * 添加分组字段
     *
     * @param groupField 分组字段名
     * @return MapSheetConfig实例，支持链式调用
     */
    public MapSheetConfig addGroupField(String groupField) {
        this.groupFields.add(groupField);
        return this;
    }

    /**
     * 添加自定义属性
     *
     * @param key 属性键
     * @param value 属性值
     * @return MapSheetConfig实例，支持链式调用
     */
    public MapSheetConfig addCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MapSheetConfig that = (MapSheetConfig) o;
        return isText == that.isText && enableDesc == that.enableDesc && Objects.equals(fields, that.fields) && Objects.equals(headFontColor, that.headFontColor) && Objects.equals(headBackColor, that.headBackColor) && Objects.equals(groupFields, that.groupFields) && Objects.equals(customProperties, that.customProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields, headFontColor, headBackColor, isText, enableDesc, groupFields, customProperties);
    }

    /**
     * 表格配置构建器
     */
    public static class SheetBuilder {
        private final MapSheetConfig config = new MapSheetConfig();

        /**
         * 设置标题行行号
         *
         * @param titleRow 标题行行号
         * @return SheetBuilder实例
         */
        public SheetBuilder titleRow(int titleRow) {
            config.setTitleRow(titleRow);
            return this;
        }

        /**
         * 设置数据起始行行号
         *
         * @param headLine 数据起始行行号
         * @return SheetBuilder实例
         */
        public SheetBuilder headLine(int headLine) {
            config.setHeadLine(headLine);
            return this;
        }

        /**
         * 设置唯一键字段名集合
         *
         * @param uniKeyNames 唯一键字段名集合
         * @return SheetBuilder实例
         */
        public SheetBuilder uniKeyNames(Set<String> uniKeyNames) {
            config.setUniKeyNames(uniKeyNames);
            return this;
        }

        /**
         * 添加唯一键字段名
         *
         * @param uniKeyName 唯一键字段名
         * @return SheetBuilder实例
         */
        public SheetBuilder addUniKeyName(String uniKeyName) {
            config.addUniKeyName(uniKeyName);
            return this;
        }

        /**
         * 设置是否覆盖已存在数据
         *
         * @param enableCover true表示覆盖，false表示不覆盖
         * @return SheetBuilder实例
         */
        public SheetBuilder enableCover(boolean enableCover) {
            config.setEnableCover(enableCover);
            return this;
        }


        /**
         * 设置是否启用 Upsert 模式
         *
         * true（默认）：根据唯一键匹配，存在则更新，不存在则追加
         * false：不匹配唯一键，所有数据直接追加到表格末尾
         *
         * @param upsert true 为 Upsert 模式，false 为纯追加模式
         * @return SheetBuilder实例
         */
        public SheetBuilder upsert(boolean upsert) {
            config.setUpsert(upsert);
            return this;
        }


        /**
         * 设置字段定义列表
         *
         * @param fields 字段定义列表
         * @return SheetBuilder实例
         */
        public SheetBuilder fields(List<MapFieldDefinition> fields) {
            config.fields = new ArrayList<>(fields);
            return this;
        }

        /**
         * 添加单个字段
         *
         * @param field 字段定义
         * @return SheetBuilder实例
         */
        public SheetBuilder addField(MapFieldDefinition field) {
            config.fields.add(field);
            return this;
        }

        /**
         * 批量添加字段
         *
         * @param fields 字段定义列表
         * @return SheetBuilder实例
         */
        public SheetBuilder addFields(List<MapFieldDefinition> fields) {
            config.fields.addAll(fields);
            return this;
        }

        /**
         * 批量添加字段（可变参数）
         *
         * @param fields 字段定义可变参数
         * @return SheetBuilder实例
         */
        public SheetBuilder addFields(MapFieldDefinition... fields) {
            config.fields.addAll(Arrays.asList(fields));
            return this;
        }

        /**
         * 设置表头字体颜色
         *
         * @param headFontColor 表头字体颜色（十六进制）
         * @return SheetBuilder实例
         */
        public SheetBuilder headFontColor(String headFontColor) {
            config.headFontColor = headFontColor;
            return this;
        }

        /**
         * 设置表头背景颜色
         *
         * @param headBackColor 表头背景颜色（十六进制）
         * @return SheetBuilder实例
         */
        public SheetBuilder headBackColor(String headBackColor) {
            config.headBackColor = headBackColor;
            return this;
        }

        /**
         * 设置表头样式
         *
         * @param fontColor 字体颜色
         * @param backColor 背景颜色
         * @return SheetBuilder实例
         */
        public SheetBuilder headStyle(String fontColor, String backColor) {
            config.headFontColor = fontColor;
            config.headBackColor = backColor;
            return this;
        }

        /**
         * 设置是否将单元格设置为纯文本
         *
         * @param isText 是否设置为纯文本
         * @return SheetBuilder实例
         */
        public SheetBuilder isText(boolean isText) {
            config.isText = isText;
            return this;
        }

        /**
         * 设置是否启用字段描述行
         *
         * @param enableDesc 是否启用
         * @return SheetBuilder实例
         */
        public SheetBuilder enableDesc(boolean enableDesc) {
            config.enableDesc = enableDesc;
            return this;
        }

        /**
         * 设置分组字段列表
         *
         * @param groupFields 分组字段列表
         * @return SheetBuilder实例
         */
        public SheetBuilder groupFields(List<String> groupFields) {
            config.groupFields = new ArrayList<>(groupFields);
            return this;
        }

        /**
         * 设置分组字段（可变参数）
         *
         * @param groupFields 分组字段可变参数
         * @return SheetBuilder实例
         */
        public SheetBuilder groupFields(String... groupFields) {
            config.groupFields = Arrays.asList(groupFields);
            return this;
        }

        /**
         * 添加分组字段
         *
         * @param groupField 分组字段名
         * @return SheetBuilder实例
         */
        public SheetBuilder addGroupField(String groupField) {
            config.groupFields.add(groupField);
            return this;
        }

        /**
         * 设置自定义属性映射
         *
         * @param customProperties 自定义属性映射
         * @return SheetBuilder实例
         */
        public SheetBuilder customProperties(Map<String, Object> customProperties) {
            config.customProperties = new HashMap<>(customProperties);
            return this;
        }

        /**
         * 添加自定义属性
         *
         * @param key 属性键
         * @param value 属性值
         * @return SheetBuilder实例
         */
        public SheetBuilder addCustomProperty(String key, Object value) {
            config.customProperties.put(key, value);
            return this;
        }

        /**
         * 构建配置对象
         *
         * @return MapSheetConfig实例
         */
        public MapSheetConfig build() {
            // 验证必填字段
            if (config.fields.isEmpty()) {
                throw new IllegalArgumentException("字段定义列表不能为空");
            }

            return config;
        }
    }
}

