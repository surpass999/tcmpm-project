package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel Sheet 表头配置 - 用于描述三行表头结构
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetHeaderConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 固定列定义 (所有Sheet通用) ==========

    /**
     * 固定列列表 (序号、医院名称、省份等)
     */
    @Builder.Default
    private List<ColumnDefinition> fixedColumns = new ArrayList<>();

    // ========== 指标列定义 (按分组组织) ==========

    /**
     * 一级分组列表 (按 sort 排序)
     */
    @Builder.Default
    private List<GroupHeaderConfig> groupHeaders = new ArrayList<>();

    /**
     * 总列数 (固定列 + 指标列)
     */
    private int totalColumnCount;

    // ========== 内部类: 列定义 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnDefinition implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 列编码 (固定列用) */
        private String code;

        /** 列标题 */
        private String title;

        /** 列宽 (字符数) */
        @Builder.Default
        private Integer width = 15;

        /** 该列是否跨多行 (固定列跨3行, 指标列根据类型决定) */
        @Builder.Default
        private Integer rowSpan = 1;
    }

    // ========== 内部类: 分组表头 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupHeaderConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 一级分组ID */
        private Long level1Id;

        /** 一级分组编码 */
        private String level1Code;

        /** 一级分组名称 */
        private String level1Name;

        /** 二级分组ID (可为null表示该分组下无二级分组) */
        private Long level2Id;

        /** 二级分组编码 */
        private String level2Code;

        /** 二级分组名称 (可为null) */
        private String level2Name;

        /** 该分组下的指标列列表 (按 sort 排序) */
        @Builder.Default
        private List<IndicatorColumnConfig> indicators = new ArrayList<>();

        /** 该分组在Excel中占据的起始列索引 */
        private int startColumnIndex;

        /** 该分组在Excel中占据的结束列索引 (包含) */
        private int endColumnIndex;

        /** 该分组下的指标列总数 (普通指标1列, 动态容器=子字段数) */
        private int columnSpan;

        /** 是否有二级分组 */
        public boolean hasLevel2() {
            return level2Id != null && level2Id > 0;
        }
    }

    // ========== 内部类: 指标列 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndicatorColumnConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 指标ID */
        private Long indicatorId;

        /** 指标编码 */
        private String indicatorCode;

        /** 指标名称 */
        private String indicatorName;

        /** 单位 */
        private String unit;

        /** 值类型 (1-12) */
        private Integer valueType;

        /** 指标选项定义 JSON (type=6/7/10/11 时有值，用于值→标签映射) */
        private String valueOptions;

        /** 指标所属分组ID */
        private Long groupId;

        /** 指标组内排序 */
        private Integer sort;

        /** 是否为动态容器指标 */
        public boolean isDynamicContainer() {
            return valueType != null && valueType == 12;
        }

        /** 动态容器子字段列表 (type=12时) */
        @Builder.Default
        private List<DynamicFieldConfig> dynamicFields = new ArrayList<>();

        /**
         * 该指标占据的列数
         * 普通指标=1, 动态容器=子字段数
         */
        public int getColumnSpan() {
            if (isDynamicContainer() && dynamicFields != null) {
                return dynamicFields.size();
            }
            return 1;
        }
    }

    // ========== 内部类: 动态容器子字段 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicFieldConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 字段编码 */
        private String fieldCode;

        /** 字段名称 */
        private String fieldLabel;

        /** 字段类型: text/number/textarea/radio/checkbox/select/multiSelect/date/dateRange */
        private String fieldType;

        /** 动态容器字段的选项定义 JSON (radio/select/checkbox/multiSelect 类型时用于值→标签映射) */
        private String fieldOptions;

        /** 字段单位 (number类型可能有) */
        private String unit;

        /** 该字段在Excel中的列标题 */
        public String getColumnTitle(String indicatorCode) {
            StringBuilder sb = new StringBuilder();
            sb.append(indicatorCode).append(".").append(fieldCode);
            sb.append(" ").append(fieldLabel);
            if (unit != null && !unit.isEmpty() && !"-".equals(unit)) {
                sb.append(" [单位: ").append(unit).append("]");
            }
            return sb.toString();
        }
    }
}
