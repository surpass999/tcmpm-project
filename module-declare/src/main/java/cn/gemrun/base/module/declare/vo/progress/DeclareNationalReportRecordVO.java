package cn.gemrun.base.module.declare.vo.progress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 国家局上报记录 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareNationalReportRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 填报记录ID列表（逗号分隔）
     */
    private String reportIds;

    /**
     * 上报数量
     */
    private Integer reportCount;

    /**
     * 省份名称
     */
    private String provinceName;

    /**
     * 上报人姓名
     */
    private String reporterName;

    /**
     * 上报时间
     */
    private LocalDateTime reportTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
