package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 导出Sheet数据 - 包含表头配置和行数据
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportSheetData implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== Sheet 元信息 ==========

    /** Sheet名称 */
    private String sheetName;

    /** 项目类型 (1-6) */
    private Integer projectType;

    /** 填报记录数 */
    private int rowCount;

    // ========== 表头配置 ==========

    /** 三行表头配置 */
    private SheetHeaderConfig headerConfig;

    // ========== 数据行 ==========

    /** 数据行列表 */
    @Builder.Default
    private List<ExportRowData> rows = new ArrayList<>();

    /**
     * 添加一行数据
     */
    public void addRow(ExportRowData row) {
        this.rows.add(row);
        this.rowCount++;
    }

    // ========== 内部类: 单行数据 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportRowData implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 序号 */
        private int sequenceNo;

        /** 填报记录ID */
        private Long reportId;

        // --- 固定列值 ---

        /** 医院名称 */
        private String hospitalName;

        /** 统一社会信用代码 */
        private String unifiedSocialCreditCode;

        /** 医疗机构执业许可证 */
        private String medicalLicenseNo;

        /** 省份 */
        private String provinceName;

        /** 填报年度 */
        private Integer reportYear;

        /** 填报批次 */
        private Integer reportBatch;

        /** 填报状态 */
        private String reportStatus;

        /** 上报状态 */
        private String nationalReportStatus;

        /** 创建时间 */
        private String createTime;

        // --- 指标值 Map: indicatorCode -> displayValue ---

        /**
         * 指标值 Map
         * Key: indicatorCode (普通指标) 或 "indicatorCode.fieldCode" (动态容器字段)
         * Value: 格式化后的展示值 (String)
         *
         * 普通指标: 直接值
         * 动态容器: JSON数组解析后的多行文本，用 "\n" 连接
         */
        @Builder.Default
        private Map<String, String> indicatorValues = new java.util.LinkedHashMap<>();
    }
}
