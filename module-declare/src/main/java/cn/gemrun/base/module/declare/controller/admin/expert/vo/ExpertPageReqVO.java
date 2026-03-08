package cn.gemrun.base.module.declare.controller.admin.expert.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 专家分页 Request VO")
@Data
public class ExpertPageReqVO extends PageParam {

    @Schema(description = "专家姓名", example = "张医生")
    private String expertName;

    @Schema(description = "专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家", example = "1")
    private Integer expertType;

    @Schema(description = "状态：1=在册，2=暂停，3=注销", example = "1")
    private Integer status;

    @Schema(description = "工作单位", example = "北京某医院")
    private String workUnit;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "关联系统用户ID", example = "1")
    private Long userId;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
