package cn.gemrun.base.module.declare.dal.dataobject.progress;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 年度进度填报主表（只存元信息，不存指标值）
 */
@TableName("declare_progress_report")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareProgressReportDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 填报批次(1-4)
     */
    private Integer reportBatch;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 部门ID（用于数据权限控制，对应 system_dept.id）
     */
    private Long deptId;

    /**
     * 医院名称(冗余)
     */
    private String hospitalName;

    /**
     * 省份编码
     */
    private String provinceCode;

    /**
     * 省份名称
     */
    private String provinceName;

    // ========== 填报阶段状态 ==========

    /**
     * 填报状态: DRAFT-草稿 SAVED-已保存 SUBMITTED-待审批 BPM审批流返回的其他状态
     */
    private String reportStatus;

    // ========== 省级审核状态 ==========

    /**
     * 省级审核: 0-未提交 1-省级审核中 2-省级通过 3-省级驳回
     */
    private Integer provinceStatus;

    /**
     * 省级审核意见
     */
    private String provinceOpinion;

    /**
     * 审核人姓名
     */
    private String auditUserName;

    /**
     * 填报人姓名（保存时填写）
     */
    private String reportUserName;

    /**
     * 省级审核时间
     */
    private LocalDateTime provinceAuditTime;

    /**
     * 省级审核人ID
     */
    private Long provinceAuditorId;

    /**
     * 省级审核人姓名
     */
    private String provinceAuditorName;

    // ========== 国家局上报状态 ==========

    /**
     * 国家局上报: 0-未上报 1-已上报
     */
    private Integer nationalReportStatus;

    /**
     * 上报时间
     */
    private LocalDateTime nationalReportTime;

    /**
     * 上报人ID
     */
    private Long nationalReporterId;

    /**
     * 上报人姓名
     */
    private String nationalReporterName;

    // ========== BPM流程实例ID ==========

    /**
     * 医院内部审核流程实例ID
     */
    private String hospitalProcessInstanceId;

    /**
     * 省级审核流程实例ID
     */
    private String provinceProcessInstanceId;

    // ========== 提交人信息 ==========

    /**
     * 创建者(提交员)
     */
    private String creator;

}
