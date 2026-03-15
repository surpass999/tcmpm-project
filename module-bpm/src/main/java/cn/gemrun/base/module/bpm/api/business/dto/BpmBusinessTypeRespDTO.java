package cn.gemrun.base.module.bpm.api.business.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务类型 Response DTO
 *
 * @author Gemini
 */
@Data
public class BpmBusinessTypeRespDTO {

    /**
     * 编号
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
     * 描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
