package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 项目创建/更新 Request VO
 *
 * @author Gemini
 */
@Data
public class ProjectSaveReqVO {

    /**
     * 项目主键
     */
    private Long id;

    /**
     * 关联备案ID
     */
    private Long filingId;

    /**
     * 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目状态（字典值）
     */
    private String projectStatus;

    /**
     * 总投资（万元）
     */
    private BigDecimal totalInvestment;

    /**
     * 中央转移支付到账金额（万元）
     */
    private BigDecimal centralFundArrive;

    /**
     * 累计完成投资（万元）
     */
    private BigDecimal accumulatedInvestment;

    /**
     * 中央转移支付累计使用金额（万元）
     */
    private BigDecimal centralFundUsed;

    /**
     * 立项时间
     */
    private LocalDateTime startTime;

    /**
     * 计划完成时间
     */
    private LocalDateTime planEndTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 实际进度（%）
     */
    private Integer actualProgress;

    /**
     * 项目负责人ID
     */
    private Long leaderUserId;

    /**
     * 负责人手机号
     */
    private String leaderMobile;

    /**
     * 负责人姓名
     */
    private String leaderName;

    /**
     * 所属部门ID（用于数据权限控制）
     */
    private Long deptId;

}
