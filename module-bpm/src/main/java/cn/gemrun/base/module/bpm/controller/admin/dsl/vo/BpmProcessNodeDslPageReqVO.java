package cn.gemrun.base.module.bpm.controller.admin.dsl.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 流程节点DSL配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BpmProcessNodeDslPageReqVO extends PageParam {

    @Schema(description = "流程定义Key", example = "proc_filing")
    private String processDefinitionKey;

    @Schema(description = "节点Key", example = "province_audit")
    private String nodeKey;

    @Schema(description = "节点名称", example = "省级审核")
    private String nodeName;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

}
