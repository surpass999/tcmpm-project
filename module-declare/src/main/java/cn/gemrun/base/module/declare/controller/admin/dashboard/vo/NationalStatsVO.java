package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶舱全国统计响应VO（国家局专用）
 *
 * @author Gemini
 */
@Data
public class NationalStatsVO {

    /**
     * 全国项目总数
     */
    private Integer totalProjectCount;

    /**
     * 全国平均进度(%)
     */
    private Integer averageProgress;

    /**
     * 整体资金执行率(%)
     */
    private Integer totalFundRate;

    /**
     * 待审批任务数量
     */
    private Integer pendingApprovalCount;

    /**
     * 各省项目分布
     */
    private List<ProvinceItem> provinceDistribution;

    /**
     * 项目类型分布
     */
    private List<DashboardStatsVO.NationalStats.ProjectTypeItem> projectTypeDistribution;

    /**
     * 资金执行排名(前10省)
     */
    private List<ProvinceItem> fundRanking;

    /**
     * 进度排名(前10省)
     */
    private List<ProvinceItem> progressRanking;

    /**
     * 已中期评估项目数
     */
    private Integer midtermProjectCount;

    /**
     * 已验收项目数
     */
    private Integer acceptedProjectCount;

    /**
     * 中央转移资金三年总额（万元），固定值 105600
     */
    private BigDecimal centralFundThreeYearTotal;

    /**
     * 到账资金总额（万元），动态汇总所有项目的 centralFundArrive
     */
    private BigDecimal centralFundArriveTotal;

    @Data
    public static class ProvinceItem {
        /** 省份名称 */
        private String provinceName;
        /** 省份ID */
        private Long provinceId;
        /** 项目数量 */
        private Integer projectCount;
        /** 平均进度(%) */
        private Integer progress;
        /** 资金执行率(%) */
        private Integer fundRate;
        /** 高风险项目数 */
        private Integer highRiskCount;
    }
}
