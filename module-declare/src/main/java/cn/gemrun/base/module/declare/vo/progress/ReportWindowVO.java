package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 填报窗口 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportWindowVO {

    /**
     * 窗口ID
     */
    private Long id;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 填报批次
     */
    private Integer reportBatch;

    /**
     * 开放开始时间
     */
    private LocalDateTime windowStart;

    /**
     * 开放结束时间
     */
    private LocalDateTime windowEnd;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Integer status;

}
