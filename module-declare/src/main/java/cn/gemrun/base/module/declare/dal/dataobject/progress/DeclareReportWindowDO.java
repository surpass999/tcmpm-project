package cn.gemrun.base.module.declare.dal.dataobject.progress;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 填报时间窗口表
 *
 * @author Gemini
 */
@TableName("declare_report_window")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareReportWindowDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 填报批次(1-4)
     */
    private Integer reportBatch;

    /**
     * 窗口开放开始时间
     */
    private LocalDateTime windowStart;

    /**
     * 窗口开放结束时间
     */
    private LocalDateTime windowEnd;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态: 1-启用 0-禁用
     */
    private Integer status;
}
