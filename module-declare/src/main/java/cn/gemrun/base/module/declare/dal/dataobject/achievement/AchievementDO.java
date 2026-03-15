package cn.gemrun.base.module.declare.dal.dataobject.achievement;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 成果信息 DO
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
     * 成果主键（自增）
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

    /**
     * 审核状态：0=待审核(PENDING)，1=省级通过/待国家局审核(SUBMITTED)，2=国家局审核中(AUDITING)，3=已认定推广(APPROVED)，4=退回(REJECTED)
     */
    private Integer auditStatus;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 推荐状态：0=未推荐(PENDING)，1=已推荐至国家局(SUBMITTED)，2=已纳入推广库(APPROVED)
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
    @Version
    private Integer version;

    /**
     * 删除原因
     */
    private String deleteReason;

}
