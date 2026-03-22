package cn.gemrun.base.module.bpm.api.business.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 业务类型查询 Request DTO
 *
 * @author Gemini
 */
@Data
public class BpmBusinessTypeGetReqDTO {

    /**
     * 业务类型标识
     */
    @NotBlank(message = "业务类型标识不能为空")
    private String businessType;

}
