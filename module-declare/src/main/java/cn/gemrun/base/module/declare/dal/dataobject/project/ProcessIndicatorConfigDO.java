package cn.gemrun.base.module.declare.dal.dataobject.project;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 过程类型指标配置 DO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_process_indicator_config")
public class ProcessIndicatorConfigDO extends BaseDO {

    /**
     * 配置主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 过程类型（1=建设过程，2=半年报，3=年度总结，4=中期评估，5=整改记录，6=验收申请）
     */
    private Integer processType;

    /**
     * 项目类型（0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型）
     */
    private Integer projectType;

    /**
     * 指标ID（关联 declare_indicator.id）
     */
    private Long indicatorId;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 满分值
     */
    private java.math.BigDecimal maxScore;

    /**
     * 满足(100%)的比例
     */
    private java.math.BigDecimal scoreRatioSatisfied;

    /**
     * 基本满足(75%)的比例
     */
    private java.math.BigDecimal scoreRatioBasic;

    /**
     * 部分满足(50%)的比例
     */
    private java.math.BigDecimal scoreRatioPartial;

    /**
     * 不满足(25%)的比例
     */
    private java.math.BigDecimal scoreRatioUnsatisfied;

}
