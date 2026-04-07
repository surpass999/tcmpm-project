package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 进度填报保存请求 VO（报告数据 + 指标值一起提交）
 *
 * @author Gemini
 */
@Data
public class DeclareProgressReportSaveReqVO {

    /**
     * 报告ID，null 表示新建，否则表示更新
     */
    private Long id;

    /**
     * 填报年度（新建时必填）
     */
    private Integer reportYear;

    /**
     * 填报批次（新建时必填）
     */
    private Integer reportBatch;

    /**
     * 填报状态：SAVED=已保存，SUBMITTED=已提交
     */
    private String reportStatus;

    /**
     * 填报人姓名（保存时填写）
     */
    private String reportUserName;

    /**
     * 指标值列表
     */
    private List<IndicatorValueItem> values;

    /**
     * 指标值项
     */
    @Data
    public static class IndicatorValueItem {

        /**
         * 指标ID
         */
        private Long indicatorId;

        /**
         * 指标代号
         */
        private String indicatorCode;

        /**
         * 值类型：1=数字，2=文本，3=日期，5=百分比，6=多行文本
         */
        private Integer valueType;

        /**
         * 值
         */
        private Object value;

        /**
         * 日期区间开始（type=8 时使用）
         */
        private String valueDateStart;

        /**
         * 日期区间结束（type=8 时使用）
         */
        private String valueDateEnd;
    }
}
