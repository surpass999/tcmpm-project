package cn.gemrun.base.module.declare.dal.dataobject.dataflow;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 数据流通记录 DO
 *
 * @author
 */
@TableName("declare_data_flow")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFlowDO extends BaseDO {

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
     * 部门ID（数据权限控制用）
     */
    private Long deptId;

    /**
     * 数据名称
     */
    private String dataName;

    /**
     * 数据描述
     */
    private String dataDescription;

    /**
     * 数据类型（如：患者数据、诊疗数据）
     */
    private String dataType;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 数据量规模（如：100万条、50GB）
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
     * 流通开始时间
     */
    private LocalDateTime startTime;

    /**
     * 流通结束时间
     */
    private LocalDateTime endTime;

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
     * 取得数据产品证书数（关联指标601）
     */
    private Integer certificateCount;

    /**
     * 取得数据产权登记证数（关联指标602）
     */
    private Integer propertyCount;

    /**
     * 状态：0=草稿(DRAFT)，1=已提交(SUBMITTED)，2=审核中(AUDITING)，3=已通过(APPROVED)，4=退回(REJECTED)
     */
    private Integer status;

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
     * 是否已生成成果展示：0=否，1=是
     */
    private Boolean hasAchievement;

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
