package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 动态容器指标的子字段定义 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicIndicatorFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段编码（唯一标识）
     */
    @NotBlank(message = "字段编码不能为空")
    private String fieldCode;

    /**
     * 字段名称（展示用）
     */
    @NotBlank(message = "字段名称不能为空")
    private String fieldLabel;

    /**
     * 字段类型：text/number/textarea/radio/checkbox/select/multiSelect/date/dateRange
     */
    @NotBlank(message = "字段类型不能为空")
    private String fieldType;

    /**
     * 是否必填
     */
    @Builder.Default
    private Boolean required = false;

    /**
     * 排序
     */
    @Builder.Default
    private Integer sort = 0;

    /**
     * 选项列表（radio/checkbox/select/multiSelect 类型使用）
     */
    private List<DynamicIndicatorOptionVO> options;

    /**
     * 最大字符数（text/textarea 类型使用）
     */
    private Integer maxLength;

    /**
     * 占位文本
     */
    private String placeholder;

    /**
     * 是否支持搜索（select/multiSelect 类型使用）
     */
    @Builder.Default
    private Boolean showSearch = false;

    /**
     * 最小选择数（checkbox/multiSelect 类型使用）
     */
    private Integer minSelect;

    /**
     * 最大选择数（checkbox/multiSelect 类型使用）
     */
    private Integer maxSelect;

    /**
     * 日期格式（date/dateRange 类型使用）
     */
    private String format;

    /**
     * 布局方式（radio/checkbox 类型使用）：horizontal/vertical
     */
    private String layout;

    /**
     * textarea 行数
     */
    @Builder.Default
    private Integer rows = 3;

    /**
     * 精度/小数位（number 类型使用）
     */
    private Integer precision;

    /**
     * 数字前缀（number 类型使用）
     */
    private String prefix;

    /**
     * 数字后缀（number 类型使用）
     */
    private String suffix;

}
