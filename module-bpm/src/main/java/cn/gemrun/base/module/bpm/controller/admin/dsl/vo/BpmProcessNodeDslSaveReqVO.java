package cn.gemrun.base.module.bpm.controller.admin.dsl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 流程节点DSL配置 创建/更新 Request VO")
@Data
public class BpmProcessNodeDslSaveReqVO {

    @Schema(description = "配置编号", example = "1")
    private Long id;

    @Schema(description = "流程定义Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "proc_filing")
    @NotBlank(message = "流程定义Key不能为空")
    private String processDefinitionKey;

    @Schema(description = "节点Key", requiredMode = Schema.RequiredMode.REQUIRED, example = "province_audit")
    @NotBlank(message = "节点Key不能为空")
    private String nodeKey;

    @Schema(description = "节点名称", example = "省级审核")
    private String nodeName;

    @Schema(description = "DSL配置JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "DSL配置不能为空")
    private String dslConfig;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "是否启用不能为空")
    private Integer enabled;

    @Schema(description = "备注", example = "省级审核节点配置")
    private String remark;

}
