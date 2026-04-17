package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标对比行 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareCompareIndicatorRowVO {

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 值类型: 1=数字 2=字符串 3=布尔 4=日期 5=长文本 6=单选 7=多选 8=日期区间 9=文件 10=单选下拉 11=多选下拉 12=动态容器
     */
    private Integer valueType;

    /**
     * 选项定义JSON
     */
    private String valueOptions;

    /**
     * 所属分组ID（关联二级分组）
     */
    private Long groupId;

    /**
     * 一级分组排序号
     */
    private Integer parentGroupSort;

    /**
     * 二级分组排序号
     */
    private Integer groupSort;

    /**
     * 指标排序号
     */
    private Integer indicatorSort;

    /**
     * 二级分组名称
     */
    private String groupName;

    /**
     * 一级分组名称
     */
    private String parentGroupName;

    /**
     * 记录A的值
     */
    private Object valueA;

    /**
     * 记录B的值
     */
    private Object valueB;

    /**
     * 差异类型: up/down/different/equal/none
     */
    private String diffType;

    /**
     * 扩展配置（包含联动规则等）
     */
    private String extraConfig;
}
