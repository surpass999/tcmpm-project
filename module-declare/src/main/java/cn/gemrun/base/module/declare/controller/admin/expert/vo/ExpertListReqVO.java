package cn.gemrun.base.module.declare.controller.admin.expert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 专家列表 Request VO")
@Data
public class ExpertListReqVO {

    @Schema(description = "专家姓名", example = "张医生")
    private String expertName;

    @Schema(description = "专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家", example = "1")
    private Integer expertType;

    @Schema(description = "状态：1=在册，2=暂停，3=注销", example = "1")
    private Integer status;

    @Schema(description = "工作单位", example = "北京某医院")
    private String workUnit;

}
