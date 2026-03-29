package cn.gemrun.base.module.declare.dal.dataobject.progress;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 国家局上报记录表（记录每次批量上报的操作日志）
 */
@TableName("declare_national_report_record")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareNationalReportRecordDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 本次上报的填报记录ID列表(JSON数组)
     */
    private String reportIds;

    /**
     * 上报记录数量
     */
    private Integer reportCount;

    /**
     * 上报省份编码
     */
    private String provinceCode;

    /**
     * 上报省份名称
     */
    private String provinceName;

    /**
     * 上报人ID
     */
    private Long reporterId;

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

}
