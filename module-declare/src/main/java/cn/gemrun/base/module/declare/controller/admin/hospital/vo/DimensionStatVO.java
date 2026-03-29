package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 维度统计 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionStatVO {

    /**
     * 维度1
     */
    private String dimension1;

    /**
     * 维度2
     */
    private String dimension2;

    /**
     * 数量
     */
    private Long count;

}
