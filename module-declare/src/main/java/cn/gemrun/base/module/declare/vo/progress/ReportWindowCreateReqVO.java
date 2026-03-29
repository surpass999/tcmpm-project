package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 填报窗口创建请求 VO
 *
 * @author Gemini
 */
@Data
public class ReportWindowCreateReqVO {

    /**
     * 填报年度
     */
    @NotNull(message = "填报年度不能为空")
    private Integer reportYear;

    /**
     * 填报批次
     */
    @NotNull(message = "填报批次不能为空")
    private Integer reportBatch;

    /**
     * 开放开始时间
     */
    @NotNull(message = "开放开始时间不能为空")
    private LocalDateTime windowStart;

    /**
     * 开放结束时间
     */
    @NotNull(message = "开放结束时间不能为空")
    private LocalDateTime windowEnd;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态: 1-启用 0-禁用
     */
    private Integer status;

}
