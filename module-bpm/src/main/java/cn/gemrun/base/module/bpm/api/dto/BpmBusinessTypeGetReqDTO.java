package cn.gemrun.base.module.bpm.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * BPM 业务类型查询 Request DTO
 *
 * @author Gemini
 */
@Data
public class BpmBusinessTypeGetReqDTO {

    /**
     * 业务类型标识
     */
    @NotEmpty(message = "业务类型标识不能为空")
    private String businessType;

}
