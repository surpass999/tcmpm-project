package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 项目过程记录 Response VO
 *
 * @author Gemini
 */
@Data
public class ProjectProcessRespVO {

    /**
     * 过程记录主键
     */
    private Long id;

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 项目名称（关联查询）
     */
    private String projectName;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
