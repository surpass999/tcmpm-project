package cn.gemrun.base.module.declare.dal.dataobject.indicator;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多指标联合验证规则 DO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_indicator_joint_rule")
public class DeclareIndicatorJointRuleDO extends BaseDO {

    /**
     * 规则主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 适用项目类型：0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;

    /**
     * 触发时机：FILL=填报时，PROCESS_SUBMIT=流程提交时
     */
    private String triggerTiming;

    /**
     * 适用流程节点
     */
    private String processNode;

    /**
     * 规则配置（JSON格式）
     */
    private String ruleConfig;

    /**
     * 规则状态：1=启用，0=禁用
     */
    private Integer status;

}
