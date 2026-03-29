package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 医院标签分配请求 VO
 *
 * @author Gemini
 */
@Data
public class HospitalTagAssignReqVO {

    /**
     * 医院编码
     */
    @NotNull(message = "医院编码不能为空")
    private String hospitalCode;

    /**
     * 标签ID列表
     */
    private List<Long> tagIds;

}
