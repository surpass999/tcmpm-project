package cn.gemrun.base.module.bpm.api.task.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 审批详情请求 DTO
 *
 * @author Gemini
 */
@Data
public class BpmApprovalDetailReqDTO {

    /**
     * 流程定义编号
     */
    private String processDefinitionId;

    /**
     * 流程实例编号
     */
    private String processInstanceId;

    /**
     * 任务编号
     */
    private String taskId;

    /**
     * 流程变量
     */
    private Map<String, Object> processVariables;

}
