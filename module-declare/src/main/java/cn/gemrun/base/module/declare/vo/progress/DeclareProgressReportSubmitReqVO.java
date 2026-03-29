package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

/**
 * 进度填报提交请求 VO
 *
 * @author Gemini
 */
@Data
public class DeclareProgressReportSubmitReqVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 提交意见
     */
    private String opinion;

}
