package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 动态容器指标子字段的选项定义 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicIndicatorOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选项值
     */
    private String value;

    /**
     * 选项标签
     */
    private String label;

}
