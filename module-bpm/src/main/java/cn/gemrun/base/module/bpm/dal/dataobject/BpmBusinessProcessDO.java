package cn.gemrun.base.module.bpm.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务与流程实例关联 DO
 *
 * 用于记录所有业务模块的流程关联信息
 * 表名：bpm_business_process
 *
 * @author Gemini
 */
@TableName("bpm_business_process")
@KeySequence("bpm_business_process_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BpmBusinessProcessDO extends BaseDO implements Serializable {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 业务类型
     * 例如：declare:filing:submit, news:publish:submit
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 当前节点Key
     */
    private String currentNodeKey;

    /**
     * 当前流程状态
     */
    private String currentStatus;

    /**
     * 发起人ID
     */
    private Long initiatorId;

    /**
     * 参与者ID列表（逗号分隔）
     * 包括发起人和所有处理过任务的人，用于数据可见性
     */
    private String initiatorIds;

    /**
     * 当前任务分配类型
     * DEPT_POST / USER / START_USER / DEPT_LEADER 等
     */
    private String currentAssignType;

    /**
     * 当前任务分配来源
     * 根据 type 不同含义不同：
     * - DEPT_POST: 岗位ID
     * - USER: 用户ID列表
     * - START_USER_SELECT: 用户来源
     * - EXPRESSION: 表达式
     */
    private String currentAssignSource;

    /**
     * 当前节点 DSL 配置 JSON
     * 包含节点的所有配置信息，如 actions、roles、assign、vars 等
     */
    private String dslJson;

    /**
     * 流程开始时间
     */
    private LocalDateTime startTime;

    /**
     * 流程结束时间
     */
    private LocalDateTime endTime;

    /**
     * 流程结果
     * agree = 通过
     * reject = 驳回
     * back = 退回
     */
    private String result;

}
