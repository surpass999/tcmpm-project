package cn.gemrun.base.module.declare.controller.admin.project.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 整改记录列表项 Response VO
 * 包含过程记录和子流程两种类型
 *
 * @author Gemini
 */
@Data
public class RectificationRecordRespVO {

    /**
     * 记录类型：1=过程记录，2=子流程
     */
    private Integer recordType;

    /**
     * 过程记录ID（recordType=1时有值）
     */
    private Long processId;

    /**
     * 子流程实例ID（recordType=2时有值）
     */
    private String childProcessInstanceId;

    /**
     * 主流程实例ID（recordType=2时关联的过程记录对应的流程实例ID）
     */
    private String parentProcessInstanceId;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 过程标题（过程记录）或流程名称（子流程）
     */
    private String title;

    /**
     * 状态
     * 过程记录：DRAFT, SAVED, SUBMITTED, AUDITING, APPROVED, REJECTED
     * 子流程：参见 BpmProcessInstanceStatusEnum
     */
    private String status;

    /**
     * 状态名称（前端展示用）
     */
    private String statusName;

    /**
     * 报告周期开始时间
     */
    private LocalDateTime reportPeriodStart;

    /**
     * 报告周期结束时间
     */
    private LocalDateTime reportPeriodEnd;

    /**
     * 创建/发起时间
     */
    private LocalDateTime createTime;

    /**
     * 结束时间（子流程有值）
     */
    private LocalDateTime endTime;

    /**
     * 发起人ID
     */
    private Long startUserId;

    /**
     * 发起人名称
     */
    private String startUserName;

    /**
     * 流程定义名称（子流程有值）
     */
    private String processDefinitionName;

    /**
     * 流程定义Key（子流程有值）
     */
    private String processDefinitionKey;

}
