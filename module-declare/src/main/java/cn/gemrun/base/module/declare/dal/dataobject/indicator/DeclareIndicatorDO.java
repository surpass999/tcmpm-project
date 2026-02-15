package cn.gemrun.base.module.declare.dal.dataobject.indicator;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标体系 DO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("declare_indicator")
public class DeclareIndicatorDO extends BaseDO {

    /**
     * 指标主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 指标代号（如101、20101、604）
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 计量单位（人、万元、次等）
     */
    private String unit;

    /**
     * 指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全
     */
    private Integer category;

    /**
     * 关联指标口径ID
     */
    private Long caliberId;

    /**
     * 逻辑校验关系（如201>=20101、802>=80201+80202）
     */
    private String logicRule;

    /**
     * 计算公式（如401=系统覆盖名老中医工作室数）
     */
    private String calculationRule;

    /**
     * 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传
     */
    private Integer valueType;

    /**
     * 选项定义（JSON格式）
     */
    private String valueOptions;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 最小值
     */
    private java.math.BigDecimal minValue;

    /**
     * 最大值
     */
    private java.math.BigDecimal maxValue;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否在列表显示
     */
    private Boolean showInList;

    /**
     * 子指标代号集合
     */
    private String childrenIndicatorCodes;

    /**
     * 适用项目类型：0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;

    /**
     * 适用业务类型（filing/project/process/achievement/transaction）
     */
    private String businessType;

}
