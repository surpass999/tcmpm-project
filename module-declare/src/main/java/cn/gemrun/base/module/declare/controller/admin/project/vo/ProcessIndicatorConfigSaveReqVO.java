package cn.gemrun.base.module.declare.controller.admin.project.vo;

import lombok.Data;

/**
 * 过程指标配置创建/更新 Request VO
 *
 * @author Gemini
 */
@Data
public class ProcessIndicatorConfigSaveReqVO {

    /**
     * 配置主键
     */
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
     * 指标ID
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

}
