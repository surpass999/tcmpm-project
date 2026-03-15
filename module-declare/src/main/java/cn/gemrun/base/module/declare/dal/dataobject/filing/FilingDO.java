package cn.gemrun.base.module.declare.dal.dataobject.filing;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

/**
 * 项目备案核心信息 DO
 *
 * @author 杜春渔
 */
@TableName("declare_filing")
@KeySequence("declare_filing_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingDO extends BaseDO {

    /**
     * 备案主键（自增）
     */
    @TableId
    private Long id;
    /**
     * 流程实例ID
     */
    private String processInstanceId;
    /**
     * 备案编号（格式：PRNT+年月日+序号，如 PRNT202603140001）
     */
    private String filingCode;
    /**
     * 统一社会信用代码
     */
    private String socialCreditCode;
    /**
     * 医疗机构执业许可证号
     */
    private String medicalLicenseNo;
    /**
     * 机构名称
     */
    private String orgName;
    /**
     * 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;
    /**
     * 有效期限开始时间
     */
    private LocalDateTime validStartTime;
    /**
     * 有效期限结束时间
     */
    private LocalDateTime validEndTime;
    /**
     * 项目计划完成时间
     */
    private LocalDateTime planEndTime;
    /**
     * 建设内容（备案方案核心）
     */
    private String constructionContent;
    /**
     * 备案状态：对应流程 DSL bizStatus
     * DRAFT=草稿，SUBMITTED=已提交，PROVINCE_APPROVED=省级审核通过，EXPERT_APPROVED=专家论证通过，
     * NATION_APPROVED=国家局审核通过，ARCHIVED=已归档，RETURNED=退回修改
     */
    private String filingStatus;
    /**
     * 状态原因
     */
    private String statusReason;
    /**
     * 省级审核意见
     */
    private String provinceReviewOpinion;
    /**
     * 省级审核时间
     */
    private LocalDateTime provinceReviewTime;
    /**
     * 省级审核人ID（关联system_users.id）
     */
    private Long provinceReviewerId;
    /**
     * 省级审核结果：0=待审核，1=通过，2=拒绝
     */
    private Integer provinceReviewResult;
    /**
     * 国家局审核意见
     */
    private String nationalReviewOpinion;
    /**
     * 国家局审核时间
     */
    private LocalDateTime nationalReviewTime;
    /**
     * 国家局审核人ID（关联system_users.id）
     */
    private Long nationalReviewerId;
    /**
     * 国家局审核结果：0=待审核，1=通过，2=拒绝
     */
    private Integer nationalReviewResult;
    /**
     * 专家论证意见
     */
    private String expertReviewOpinion;
    /**
     * 论证专家ID集合（逗号分隔，关联declare_expert.id）
     */
    private String expertReviewerIds;
    /**
     * 备案归档时间
     */
    private LocalDateTime filingArchiveTime;
    /**
     * 关联项目ID（关联project_project.id）
     */
    private Long projectId;
    /**
     * 所属部门ID（用于数据权限控制）
     */
    @TableField("dept_id")
    private Long deptId;


}