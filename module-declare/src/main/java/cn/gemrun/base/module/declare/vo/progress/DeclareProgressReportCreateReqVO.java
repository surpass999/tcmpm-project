package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 进度填报创建请求 VO
 *
 * @author Gemini
 */
@Data
public class DeclareProgressReportCreateReqVO {

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

}
