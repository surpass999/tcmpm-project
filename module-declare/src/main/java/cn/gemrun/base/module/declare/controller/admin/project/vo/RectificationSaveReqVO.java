package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 整改记录创建/更新 Request VO
 *
 * @author Gemini
 */
@Data
public class RectificationSaveReqVO {

    /**
     * 整改记录主键
     */
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联过程记录ID
     */
    private Long processId;

    /**
     * 整改来源
     */
    private Integer rectifyType;

    /**
     * 整改项编号
     */
    private String rectifyItem;

    /**
     * 整改内容
     */
    private String rectifyContent;

    /**
     * 整改要求
     */
    private String rectifyRequirement;

    /**
     * 完成状态
     */
    private Integer completeStatus;

    /**
     * 整改期限
     */
    private LocalDateTime deadline;

    /**
     * 整改完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 佐证材料文件ID（逗号分隔）
     */
    private String fileIds;

    /**
     * 备注
     */
    private String remark;

}
