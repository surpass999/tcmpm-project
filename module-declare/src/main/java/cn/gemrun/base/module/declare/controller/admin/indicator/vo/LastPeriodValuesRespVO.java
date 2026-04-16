package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 上期指标值返回 VO
 * display: 经过 getDisplayValue 映射后的值（用于前端显示 label）
 * raw: 原始 valueStr（用于前端解析 inputType 输入内容）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastPeriodValuesRespVO {

    /**
     * 经过映射后的上期值（key = indicatorCode, value = 显示文本 label）
     */
    private Map<String, String> display;

    /**
     * 原始上期值（key = indicatorCode, value = 原始 valueStr，包含 inputType 的 ∵ 分隔符）
     */
    private Map<String, String> raw;
}
