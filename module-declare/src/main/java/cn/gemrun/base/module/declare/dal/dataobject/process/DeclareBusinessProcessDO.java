package cn.gemrun.base.module.declare.dal.dataobject.process;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.*;
import java.time.*;

/**
 * 业务与流程实例关联 DO
 *
 * @author Gemini
 */
@TableName("declare_business_process")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeclareBusinessProcessDO extends BaseDO implements Serializable {

    /**
     * 业务类型
     * 例如：declare:filing:submit
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
