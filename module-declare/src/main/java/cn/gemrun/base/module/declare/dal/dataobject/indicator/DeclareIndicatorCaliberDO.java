package cn.gemrun.base.module.declare.dal.dataobject.indicator;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标口径 DO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_indicator_caliber")
public class DeclareIndicatorCaliberDO extends BaseDO {

    /**
     * 口径主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联指标ID
     */
    private Long indicatorId;

    /**
     * 指标解释
     */
    private String definition;

    /**
     * 统计范围
     */
    private String statisticScope;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 填报要求
     */
    private String fillRequire;

    /**
     * 计算公式
     */
    @TableField("calculation_example")
    private String calculationExample;

    /**
     * 指标名称（冗余字段，不存储数据库）
     */
    @TableField(exist = false)
    private String indicatorName;

}
