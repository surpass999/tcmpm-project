package cn.gemrun.base.module.bpm.api.dto;

import lombok.Data;

/**
 * BPM 业务类型 Response DTO
 *
 * @author Gemini
 */
@Data
public class BpmBusinessTypeRespDTO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 业务类型标识
     */
    private String businessType;

    /**
     * 业务类型名称
     */
    private String businessName;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程分类
     */
    private String processCategory;

    /**
     * 是否启用
     */
    private Integer enabled;

}
