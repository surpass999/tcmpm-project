package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

/**
 * 过程填报数据 Response VO
 *
 * @author Gemini
 */
@Data
public class ProcessFormDataRespVO {

    /**
     * 过程记录ID
     */
    private Long processId;

    /**
     * 文本填报内容（JSON格式）
     */
    private Map<String, Object> formData;

    /**
     * 指标值（JSON格式）
     */
    private Map<String, Object> indicatorValues;

    /**
     * 附件IDs（逗号分隔）
     */
    private String attachmentIds;

    /**
     * 填报状态
     */
    private String status;

    /**
     * 填报状态名称
     */
    private String statusName;

    /**
     * 填报人姓名
     */
    private String createName;

    /**
     * 填报时间
     */
    private LocalDateTime createTime;

    /**
     * 指标版本（已同步次数）
     */
    private Integer indicatorVersion;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;

}
