package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶舱资金统计响应VO
 *
 * @author Gemini
 */
@Data
public class FundStatsVO {

    /**
     * 资金总额(万元)
     */
    private BigDecimal totalFund;

    /**
     * 已执行金额(万元)
     */
    private BigDecimal executedFund;

    /**
     * 执行率(%)
     */
    private Integer executionRate;

    /**
     * 各医院资金执行情况
     */
    private List<HospitalFund> hospitalFunds;

    /**
     * 资金执行趋势(按月)
     */
    private List<MonthlyTrend> monthlyTrend;

    @Data
    public static class HospitalFund {
        /** 医院名称 */
        private String hospitalName;
        /** 医院ID */
        private Long hospitalId;
        /** 项目名称 */
        private String projectName;
        /** 项目ID */
        private Long projectId;
        /** 预算金额(万元) */
        private BigDecimal budgetFund;
        /** 已执行金额(万元) */
        private BigDecimal executedFund;
        /** 执行率(%) */
        private Integer executionRate;
    }

    @Data
    public static class MonthlyTrend {
        /** 月份(YYYY-MM) */
        private String month;
        /** 计划执行金额 */
        private BigDecimal plannedFund;
        /** 实际执行金额 */
        private BigDecimal actualFund;
    }
}
