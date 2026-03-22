package cn.gemrun.base.module.declare.dal.dataobject.review;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评审任务 DO
 *
 * @author Gemini
 */
@TableName("declare_review_task")
@KeySequence("declare_review_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskDO extends BaseDO {

    /**
     * 任务主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 节点Key（对应DSL的nodeKey）
     */
    private String taskDefinitionKey;

    /**
     * 任务类型：字典declare_process_type的值
     * 1=备案论证，2=中期评估，3=验收评审，4=成果审核
     */
    private Integer taskType;

    /**
     * 业务类型：1=备案，2=项目，3=成果
     */
    private Integer businessType;

    /**
     * 关联业务ID
     */
    private Long businessId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;

    /**
     * 任务截止时间
     */
    private LocalDateTime endTime;

    /**
     * 参与专家ID集合（逗号分隔）
     */
    private String expertIds;

    /**
     * 评审标准JSON（关联指标体系）
     */
    private String reviewStandard;

    /**
     * 评审要求
     */
    private String reviewRequirement;

    /**
     * 综合评分
     */
    private BigDecimal totalScore;

    /**
     * 评审结论：通过/需整改/未通过
     */
    private String reviewConclusion;

    /**
     * 任务状态：0=待分配，1=评审中，2=已完成
     */
    private Integer status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 删除原因
     */
    private String deleteReason;

}
