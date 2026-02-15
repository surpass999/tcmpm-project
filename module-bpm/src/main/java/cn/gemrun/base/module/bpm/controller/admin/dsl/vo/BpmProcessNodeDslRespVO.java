package cn.gemrun.base.module.bpm.controller.admin.dsl.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 流程节点DSL配置 Response VO")
@Data
public class BpmProcessNodeDslRespVO {

    @Schema(description = "配置编号", example = "1")
    private Long id;

    @Schema(description = "流程定义Key", example = "proc_filing")
    private String processDefinitionKey;

    @Schema(description = "节点Key", example = "province_audit")
    private String nodeKey;

    @Schema(description = "节点名称", example = "省级审核")
    private String nodeName;

    @Schema(description = "DSL配置JSON")
    private String dslConfig;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
