package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 项目过程记录创建/更新 Request VO
 *
 * @author Gemini
 */
@Data
public class ProjectProcessSaveReqVO {

    /**
     * 过程记录主键
     */
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 过程类型
     */
    private Integer processType;

    /**
     * 过程标题
     */
    private String processTitle;

    /**
     * 报告周期开始时间
     */
    private LocalDateTime reportPeriodStart;

    /**
     * 报告周期结束时间
     */
    private LocalDateTime reportPeriodEnd;

    /**
     * 报告提交时间
     */
    private LocalDateTime reportTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 佐证材料文件ID（逗号分隔）
     */
    private String fileIds;

    /**
     * 备注
     */
    private String remark;

    /**
     * 所属部门ID（用于数据权限控制）
     */
    private Long deptId;

}
