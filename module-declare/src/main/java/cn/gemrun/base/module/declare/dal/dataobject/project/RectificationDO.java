package cn.gemrun.base.module.declare.dal.dataobject.project;

import java.time.LocalDateTime;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

/**
 * 项目整改跟踪 DO
 *
 * @author Gemini
 */
@TableName("declare_rectification")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RectificationDO extends BaseDO {

    /**
     * 整改主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联项目ID（declare_project.id）
     */
    private Long projectId;

    /**
     * 关联过程记录ID（declare_project_process.id）
     */
    private Long processId;

    /**
     * 整改来源：1=中期评估，2=验收申请
     */
    private Integer rectifyType;

    /**
     * 整改项编号（如：指标601）
     */
    private String rectifyItem;

    /**
     * 整改要求/问题描述
     */
    private String rectifyContent;

    /**
     * 整改措施/完成情况
     */
    private String rectifyMeasures;

    /**
     * 整改期限
     */
    private LocalDateTime deadline;

    /**
     * 整改责任人ID（system_users.id）
     */
    private Long responserId;

    /**
     * 整改完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 完成状态：0=待整改，1=已提交待复验，2=复验通过，3=复验驳回
     */
    private Integer completeStatus;

    /**
     * 佐证材料文件ID集合（infra_file.id，逗号分隔）
     */
    private String evidenceFileIds;

    /**
     * 省级复验意见
     */
    private String provinceReviewOpinion;

    /**
     * 省级复验结果：1=通过，2=驳回
     */
    private Integer provinceReviewResult;

    /**
     * 省级复验时间
     */
    private LocalDateTime provinceReviewTime;

    /**
     * 省级复验人ID
     */
    private Long provinceReviewerId;

    /**
     * 国家局复验意见
     */
    private String nationalReviewOpinion;

    /**
     * 国家局复验结果：1=通过，2=驳回
     */
    private Integer nationalReviewResult;

    /**
     * 国家局复验时间
     */
    private LocalDateTime nationalReviewTime;

    /**
     * 国家局复验人ID
     */
    private Long nationalReviewerId;

    /**
     * 所属部门ID（用于数据权限控制）
     */
    @TableField("dept_id")
    private Long deptId;

}
