package cn.gemrun.base.module.declare.controller.admin.dataflow.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.Date;

@Schema(description = "管理后台 - 数据流通记录分页 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFlowPageReqVO extends PageParam {

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "数据名称（模糊搜索）")
    private String dataName;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "流通类型：1=内部使用，2=对外共享，3=交易")
    private Integer flowType;

    @Schema(description = "状态：0=草稿，1=已提交，2=审核中，3=已通过，4=退回")
    private Integer status;

    @Schema(description = "是否已生成成果展示：0=否，1=是")
    private Boolean hasAchievement;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date[] createTime;

}
