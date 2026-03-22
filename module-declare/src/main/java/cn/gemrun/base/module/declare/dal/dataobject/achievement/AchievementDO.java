package cn.gemrun.base.module.declare.dal.dataobject.achievement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 成果与流通合并 DO
 *
 * @author
 */
@TableName("declare_achievement")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDO extends BaseDO {

    /**
     * 主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 关联项目ID（declare_project.id）
     */
    private Long projectId;

    /**
     * 项目名称（非数据库字段，用于关联查询）
     */
    @TableField(exist = false)
    private String projectName;

    /**
     * 部门ID（数据权限控制用）
     */
    private Long deptId;

    /**
     * 成果名称
     */
    private String achievementName;

    /**
     * 成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验
     */
    private Integer achievementType;

    /**
     * 应用领域
     */
    private String applicationField;

    /**
     * 成果描述
     */
    private String description;

    /**
     * 应用效果描述
     */
    private String effectDescription;

    /**
     * 可复制性评估：1=高，2=中，3=低
     */
    private Integer replicationValue;

    /**
     * 推广范围：1=院内，2=省级，3=全国
     */
    private Integer promotionScope;

    /**
     * 推广次数
     */
    private Integer promotionCount;

    /**
     * 转化类型：1=标准规范，2=创新模式，3=典型案例
     */
    private Integer transformType;

    // ==================== 数据流通信息（从 data_flow 合并）====================

    /**
     * 数据名称
     */
    private String dataName;

    /**
     * 数据描述
     */
    private String dataDescription;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 数据量规模
     */
    private String dataVolume;

    /**
     * 数据质量评级：1=优，2=良，3=中，4=差
     */
    private Integer dataQuality;

    /**
     * 共享范围：1=院内，2=省级，3=全国
     */
    private Integer shareScope;

    /**
     * 流通类型：1=内部使用，2=对外共享，3=交易
     */
    private Integer flowType;

    /**
     * 流通对象
     */
    private String flowObject;

    /**
     * 流通目的
     */
    private String flowPurpose;

    /**
     * 安全备案编号
     */
    private String securityFilingNo;

    /**
     * 安全备案时间
     */
    private LocalDateTime securityFilingTime;

    /**
     * 流通开始时间
     */
    private LocalDateTime startTime;

    /**
     * 流通结束时间
     */
    private LocalDateTime endTime;

    // ==================== 证书信息（对应指标 601, 602）====================

    /**
     * 数据产品证书数（指标601）
     */
    private Integer certificateCount;

    /**
     * 数据产权登记证数（指标602）
     */
    private Integer propertyCount;

    // ==================== 交易信息（对应指标 603, 604）====================

    /**
     * 完成交易的数据产品数（指标603）
     */
    private Integer transactionCount;

    /**
     * 累计交易金额（万元，指标604）
     */
    private BigDecimal transactionAmount;

    /**
     * 交易对象
     */
    private String transactionObject;

    /**
     * 交易完成时间
     */
    private LocalDateTime transactionTime;

    /**
     * 交易合同路径
     */
    private String transactionContract;

    // ==================== 附件和状态 ====================

    /**
     * 附件ID集合
     */
    private String attachmentIds;

    /**
     * 成果状态：DRAFT=草稿，SUBMITTED=已提交，AUDITING=审核中，APPROVED=已通过，REJECTED=退回
     * 由 BPM 流程监听器通过 bizStatus 推进
     */
    private String status;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 审核状态：0=待审核，1=省级通过/待国家局审核，2=国家局审核中，3=已认定推广，4=退回
     */
    private Integer auditStatus;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 推荐状态：0=未推荐，1=已推荐至国家局，2=已纳入推广库
     */
    private Integer recommendStatus;

    /**
     * 推荐意见
     */
    private String recommendOpinion;

    /**
     * 推荐人ID
     */
    private Long recommenderId;

    /**
     * 推荐时间
     */
    private LocalDateTime recommendTime;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 删除原因
     */
    private String deleteReason;

}
