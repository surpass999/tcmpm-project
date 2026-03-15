package cn.gemrun.base.module.declare.dal.dataobject.project;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

/**
 * 已立项项目核心信息 DO
 *
 * @author Gemini
 */
@TableName("declare_project")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDO extends BaseDO {

    /**
     * 项目主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联备案ID（declare_filing.id）
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
     * 项目状态：字典 declare_project_status
     * INITIATION=初始化，FILING=立项中，CONSTRUCTION=建设中，MIDTERM=中期评估，
     * RECTIFICATION=整改中，ACCEPTANCE=验收中，ACCEPTED=已验收，TERMINATED=已终止
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
     * 项目负责人ID（关联system_users.id）
     */
    private Long leaderUserId;

    /**
     * 负责人手机号
     */
    private String leaderMobile;

    /**
     * 负责人姓名（冗余）
     */
    private String leaderName;

    /**
     * 所属部门ID（用于数据权限控制）
     */
    @TableField("dept_id")
    private Long deptId;

}
