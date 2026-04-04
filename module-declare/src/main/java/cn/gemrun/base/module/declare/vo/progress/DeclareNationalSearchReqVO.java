package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 国家局高级搜索请求参数
 *
 * @author Gemini
 */
@Data
public class DeclareNationalSearchReqVO implements Serializable {

    // ============ 基本信息区 ============

    /**
     * 医院名称（模糊匹配）
     */
    private String hospitalName;

    /**
     * 填报年度
     */
    private Integer reportYear;

    /**
     * 填报批次（1-4）
     */
    private Integer reportBatch;

    /**
     * 填报状态
     */
    private String reportStatus;

    /**
     * 省级审核状态（0-3）
     */
    private Integer provinceStatus;

    /**
     * 国家局上报状态（0-1）
     */
    private Integer nationalReportStatus;

    /**
     * 项目类型（1-6，必选）
     */
    private Integer projectType;

    // ============ 指标条件区 ============

    /**
     * 指标条件组列表（组间 AND 连接，组内 OR 连接），空列表表示不使用指标条件
     */
    private List<IndicatorConditionGroup> indicatorGroups = new ArrayList<>();
}
