package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量国家局上报请求 VO
 *
 * @author Gemini
 */
@Data
public class BatchNationalReportReqVO {

    /**
     * 上报记录ID列表
     */
    @NotEmpty(message = "上报记录ID列表不能为空")
    private List<Long> reportIds;

    /**
     * 备注
     */
    private String remark;

}
