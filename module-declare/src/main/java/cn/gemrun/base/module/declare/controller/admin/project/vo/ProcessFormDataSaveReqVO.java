package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 过程填报数据保存 Request VO
 *
 * @author Gemini
 */
@Data
public class ProcessFormDataSaveReqVO {

    /**
     * 过程记录ID
     */
    @NotNull(message = "过程记录ID不能为空")
    private Long processId;

    /**
     * 文本填报内容（JSON格式）
     * 包含：progress-建设进展，problems-存在问题，nextPlan-下一步计划 等
     */
    private Map<String, Object> formData;

    /**
     * 指标值（JSON格式）
     * 键为 indicator_{indicatorId}，值为具体指标值
     */
    private Map<String, Object> indicatorValues;

    /**
     * 附件IDs（逗号分隔）
     */
    private String attachmentIds;

    /**
     * 操作类型：1=保存草稿，2=提交
     * 默认为1保存草稿
     */
    private Integer operateType = 1;

    /**
     * 状态：前端直接传 DRAFT/SAVED/SUBMITTED
     */
    private String status;

}
