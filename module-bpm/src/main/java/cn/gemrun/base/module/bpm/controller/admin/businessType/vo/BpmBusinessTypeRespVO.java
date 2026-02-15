package cn.gemrun.base.module.bpm.controller.admin.businessType.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 业务类型 Response VO")
@Data
public class BpmBusinessTypeRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "业务类型标识", example = "declare:filing:create")
    private String businessType;

    @Schema(description = "业务类型名称", example = "备案申请")
    private String businessName;

    @Schema(description = "流程定义Key", example = "projectFiling")
    private String processDefinitionKey;

    @Schema(description = "流程分类", example = "declare")
    private String processCategory;

    @Schema(description = "描述", example = "医院提交备案申请")
    private String description;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
