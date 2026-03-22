package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标值 Response VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorValueRespVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 业务类型
     */
    private Integer businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 值类型
     */
    private Integer valueType;

    /**
     * 数字型值
     */
    private String valueNum;

    /**
     * 字符串型值
     */
    private String valueStr;

    /**
     * 布尔型值
     */
    private Boolean valueBool;

    /**
     * 日期型值
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

    /**
     * 是否有效
     */
    private Boolean isValid;

    /**
     * 校验信息
     */
    private String validationMsg;

    /**
     * 填报时间
     */
    private LocalDateTime fillTime;

    /**
     * 填报人ID
     */
    private Long fillerId;

}
