package cn.gemrun.base.module.declare.dal.dataobject.project;

import java.time.LocalDateTime;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

/**
 * 项目过程记录 DO
 *
 * @author Gemini
 */
@TableName("declare_project_process")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectProcessDO extends BaseDO {

    /**
     * 过程记录主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联项目ID（declare_project.id）
     */
    private Long projectId;

    /**
     * 过程类型：1=建设过程，2=半年报，3=年度总结，4=中期评估，5=整改记录，6=验收申请
     */
    private Integer processType;

    /**
     * 过程标题（如：2025年上半年建设过程）
     */
    private String processTitle;

    /**
     * 过程数据JSON
     */
    private String processData;

    /**
     * 指标值JSON
     */
    private String indicatorValues;


    /**
     * 附件ID集合
     */
    private String attachmentIds;

    /**
     * 状态：数据字典（DRAFT草稿，SUBMITTED已提交，AUDITING审核中，APPROVED通过，REJECTED退回）
     */
    private String status;

    /**
     * 状态变更原因
     */
    private String statusReason;


    /**
     * 报告周期开始时间
     */
    private LocalDateTime reportPeriodStart;

    /**
     * 报告周期结束时间
     */
    private LocalDateTime reportPeriodEnd;

    /**
     * 报告提交时间
     */
    private LocalDateTime reportTime;

    /**
     * 审核意见
     */
    private String reviewOpinion;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审核结果：1=通过，2=退回
     */
    private Integer reviewResult;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 所属部门ID（用于数据权限控制）
     */
    @TableField("dept_id")
    private Long deptId;

    /**
     * 创建人ID（用户ID，用于判断是否为创建人）
     */
    private Long creatorId;

    /**
     * 指标版本（用于记录同步次数）
     */
    private Integer version;

}
