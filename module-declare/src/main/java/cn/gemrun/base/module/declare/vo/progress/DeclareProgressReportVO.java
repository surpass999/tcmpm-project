package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 进度填报 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareProgressReportVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 流程实例ID（BPM 流程实例，关联 progressReportApprove 流程）
     */
    private String hospitalProcessInstanceId;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 填报批次
     */
    private Integer reportBatch;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 医院名称
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

    /**
     * 填报状态
     */
    private String reportStatus;

    /**
     * 填报状态名称
     */
    private String reportStatusName;

    /**
     * 省级审核状态
     */
    private Integer provinceStatus;

    /**
     * 省级审核状态名称
     */
    private String provinceStatusName;

    /**
     * 审核人姓名
     */
    private String auditUserName;

    /**
     * 填报人姓名
     */
    private String reportUserName;

    /**
     * 国家局上报状态
     */
    private Integer nationalReportStatus;

    /**
     * 国家局上报状态名称
     */
    private String nationalReportStatusName;

    /**
     * 国家局上报时间
     */
    private LocalDateTime nationalReportTime;

    /**
     * 国家局上报人姓名
     */
    private String nationalReporterName;

    /**
     * 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;

    /**
     * 项目类型名称
     */
    private String projectTypeName;

    /**
     * 项目类型简称（如"综合型"）
     */
    private String projectTypeShortName;

    /**
     * 填报窗口开始时间
     */
    private LocalDateTime windowStart;

    /**
     * 填报窗口结束时间
     */
    private LocalDateTime windowEnd;

    /**
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;

    /**
     * 医疗机构执业许可证登记号
     */
    private String medicalLicenseNo;

    /**
     * 填报人（用户ID）
     */
    private String creator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
