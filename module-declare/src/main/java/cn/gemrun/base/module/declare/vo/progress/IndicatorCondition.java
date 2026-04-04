package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;
import java.io.Serializable;

/**
 * 指标搜索条件
 *
 * @author Gemini
 */
@Data
public class IndicatorCondition implements Serializable {

    /**
     * 指标ID（indicator.id，用于前端显示）
     */
    private Long indicatorId;

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 值类型（1-12，排除9文件上传）
     */
    private Integer valueType;

    /**
     * 操作符（全部使用 snake_case）：
     * eq, neq, gt, gte, lt, lte, between
     * contains, starts_with, is_empty, is_not_empty
     * has_any, has_all  (多选用，value用逗号分隔多个值)
     * overlaps, contains_range  (日期区间用)
     */
    private String operator;

    /**
     * 搜索值（字符串，多值用逗号分隔）
     */
    private String value;

    /**
     * 搜索值2（用于区间类操作符：between, overlaps, contains）
     */
    private String value2;

    /**
     * 动态容器子字段专用：子字段编码
     */
    private String fieldCode;

    /**
     * 动态容器子字段专用：子字段类型（number/text/textarea/radio/select/checkbox/multiSelect/date/dateRange）
     */
    private String fieldType;
}
