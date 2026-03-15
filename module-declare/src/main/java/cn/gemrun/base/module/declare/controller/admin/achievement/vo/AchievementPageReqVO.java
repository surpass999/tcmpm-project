package cn.gemrun.base.module.declare.controller.admin.achievement.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.Date;

@Schema(description = "管理后台 - 成果信息分页 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementPageReqVO extends PageParam {

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "成果名称（模糊搜索）")
    private String achievementName;

    @Schema(description = "成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验")
    private Integer achievementType;

    @Schema(description = "审核状态：0=待审核，1=省级通过/待国家局审核，2=国家局审核中，3=已认定推广，4=退回")
    private Integer auditStatus;

    @Schema(description = "推荐状态：0=未推荐，1=已推荐至国家局，2=已纳入推广库")
    private Integer recommendStatus;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date[] createTime;

}
