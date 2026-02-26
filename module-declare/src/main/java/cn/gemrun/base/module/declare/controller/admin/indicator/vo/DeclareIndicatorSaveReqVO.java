package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * 指标创建/更新 Request VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorSaveReqVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 指标代号（如101、20101、604）
     */
    @NotEmpty(message = "指标代号不能为空")
    private String indicatorCode;

    /**
     * 指标名称
     */
    @NotEmpty(message = "指标名称不能为空")
    private String indicatorName;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全
     */
    private Integer category;

    /**
     * 逻辑校验关系
     */
    private String logicRule;

    /**
     * 计算公式
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
     * 适用项目类型：0=全部
     */
    private Integer projectType;

    /**
     * 适用业务类型
     */
    private String businessType;

}
