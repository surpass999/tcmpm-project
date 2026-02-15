package cn.gemrun.base.module.declare.dal.dataobject.indicator;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标值存储 DO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_indicator_value")
public class DeclareIndicatorValueDO extends BaseDO {

    /**
     * 主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
     */
    private Integer businessType;

    /**
     * 关联业务主键
     */
    private Long businessId;

    /**
     * 关联指标ID
     */
    private Long indicatorId;

    /**
     * 指标代号（冗余）
     */
    private String indicatorCode;

    /**
     * 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传
     */
    private Integer valueType;

    /**
     * 数字型值
     */
    private BigDecimal valueNum;

    /**
     * 字符串型值（单选/多选值以逗号分隔）
     */
    private String valueStr;

    /**
     * 布尔型值
     */
    private Boolean valueBool;

    /**
     * 日期型值
     */
    private LocalDateTime valueDate;

    /**
     * 日期区间-开始
     */
    private LocalDateTime valueDateStart;

    /**
     * 日期区间-结束
     */
    private LocalDateTime valueDateEnd;

    /**
     * 长文本型值
     */
    private String valueText;

    /**
     * 是否有效（校验通过）
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
