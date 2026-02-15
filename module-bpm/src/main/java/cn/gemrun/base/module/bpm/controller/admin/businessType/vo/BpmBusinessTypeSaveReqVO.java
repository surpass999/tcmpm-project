package cn.gemrun.base.module.bpm.controller.admin.businessType.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 业务类型 创建/更新 Request VO")
@Data
public class BpmBusinessTypeSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "业务类型标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "declare:filing:create")
    @NotBlank(message = "业务类型标识不能为空")
    private String businessType;

    @Schema(description = "业务类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "备案申请")
    @NotBlank(message = "业务类型名称不能为空")
    private String businessName;

    @Schema(description = "流程定义Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "projectFiling")
    @NotBlank(message = "流程定义Key不能为空")
    private String processDefinitionKey;

    @Schema(description = "流程分类", example = "declare")
    private String processCategory;

    @Schema(description = "描述", example = "医院提交备案申请")
    private String description;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer enabled;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
