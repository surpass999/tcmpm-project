package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 指标值保存请求 VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorValueSaveReqVO {

    /**
     * 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
     */
    @NotNull(message = "业务类型不能为空")
    private Integer businessType;

    /**
     * 关联业务主键
     */
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    /**
     * 指标值列表
     */
    @NotNull(message = "指标值不能为空")
    private List<IndicatorValueItem> values;

    /**
     * 单个指标值
     */
    @Data
    public static class IndicatorValueItem {

        /**
         * 指标ID
         */
        @NotNull(message = "指标ID不能为空")
        private Long indicatorId;

        /**
         * 指标代号
         */
        @NotNull(message = "指标代号不能为空")
        private String indicatorCode;

        /**
         * 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间
         */
        @NotNull(message = "值类型不能为空")
        private Integer valueType;

        /**
         * 数字型值
         */
        private String valueNum;

        /**
         * 字符串型值（单选/多选值以逗号分隔）
         */
        private String valueStr;

        /**
         * 布尔型值
         */
        private Boolean valueBool;

        /**
         * 日期型值（格式：yyyy-MM-dd HH:mm:ss）
         */
        private String valueDate;

        /**
         * 日期区间-开始
         */
        private String valueDateStart;

        /**
         * 日期区间-结束
         */
        private String valueDateEnd;

        /**
         * 长文本型值
         */
        private String valueText;

    }

}
