package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 填报窗口统计信息 VO（国家局专用）
 *
 * @author Gemini
 */
@Data
public class ReportWindowStatsVO {

    /**
     * 当前是否有开放的填报窗口
     */
    private Boolean hasOpenWindow;

    /**
     * 当前开放窗口名称（备注）
     */
    private String openWindowName;

    /**
     * 当前填报批次（1-4）
     */
    private Integer currentBatch;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 窗口开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    /**
     * 窗口结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    /**
     * 已填报医院数
     */
    private Integer reportedHospitalCount;

    /**
     * 全国医院总数
     */
    private Integer totalHospitalCount;
}
