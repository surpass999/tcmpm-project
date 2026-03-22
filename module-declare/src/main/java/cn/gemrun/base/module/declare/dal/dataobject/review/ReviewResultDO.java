package cn.gemrun.base.module.declare.dal.dataobject.review;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评审结果 DO
 * 每位专家的评审详情，关联 Flowable 任务
 *
 * @author Gemini
 */
@TableName("declare_review_result")
@KeySequence("declare_review_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDO extends BaseDO {

    /**
     * 结果主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联评审任务ID（declare_review_task.id）
     */
    private Long taskId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * Flowable任务ID
     */
    private String flowableTaskId;

    /**
     * 评审专家ID（declare_expert.id）
     */
    private Long expertId;

    /**
     * 业务类型：1=备案，2=项目，3=成果
     */
    private Integer businessType;

    /**
     * 关联业务ID
     */
    private Long businessId;

    /**
     * 评审状态：0=待评审(PENDING)，1=已接收(RECEIVED)，2=评审中(REVIEWING)，3=已提交(SUBMITTED)，4=超时(TIMEOUT)
     */
    private Integer status;

    /**
     * 是否存在利益冲突
     */
    private Boolean isConflict;

    /**
     * 冲突检测结果
     */
    private String conflictCheckResult;

    /**
     * 是否申请回避
     */
    private Boolean isAvoid;

    /**
     * 回避原因
     */
    private String avoidReason;

    /**
     * 评分数据JSON（各指标评分详情）
     * 格式：[{"indicatorId":101,"indicatorCode":"101","score":5,"comment":"全部满足"}]
     */
    private String scoreData;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 评审结论：通过/需整改/未通过
     */
    private String conclusion;

    /**
     * 评审意见（包含核心结论、具体依据、整改建议）
     */
    private String opinion;

    /**
     * 接收任务时间
     */
    private LocalDateTime receiveTime;

    /**
     * 提交评审时间
     */
    private LocalDateTime submitTime;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 删除原因
     */
    private String deleteReason;

}
