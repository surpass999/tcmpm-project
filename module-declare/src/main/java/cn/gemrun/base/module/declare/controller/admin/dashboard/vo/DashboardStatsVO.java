package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 驾驶舱统计数据响应VO
 *
 * @author Gemini
 */
@Data
public class DashboardStatsVO {

    /**
     * 用户角色: hospital-医院, province-省级, national-国家局, expert-专家
     */
    private String userRole;

    // ===== 统计卡片数据 =====

    /**
     * 待办任务数量
     */
    private Integer taskCount;

    /**
     * 项目总数
     */
    private Integer projectCount;

    /**
     * 项目平均进度(%)
     */
    private Integer projectProgress;

    /**
     * 资金执行率(%)
     */
    private Integer fundExecutionRate;

    /**
     * 风险预警数量
     */
    private Integer warningCount;

    /**
     * 未读通知数量
     */
    private Integer noticeCount;

    // ===== 按角色的详细数据 =====

    /**
     * 医院专属数据
     */
    private HospitalStats hospitalStats;

    /**
     * 省级专属数据
     */
    private ProvinceStats provinceStats;

    /**
     * 国家局专属数据
     */
    private NationalStats nationalStats;

    /**
     * 专家专属数据
     */
    private ExpertStats expertStats;

    @Data
    public static class HospitalStats {
        /** 我负责的项目数量 */
        private Integer myProjectCount;
        /** 待填写报告数量(草稿状态) */
        private Integer draftReportCount;
        /** 审核中报告数量 */
        private Integer reviewingReportCount;
        /** 已完成报告数量 */
        private Integer completedReportCount;
        /** 即将到期任务数量 */
        private Integer urgentTaskCount;
        /** 四维度进度 */
        private DimensionProgress dimensionProgress;
    }

    /**
     * 四维度进度
     */
    @Data
    public static class DimensionProgress {
        /** 系统功能建设进度(%) */
        private Integer systemProgress;
        /** 高质量数据集建设进度(%) */
        private Integer datasetProgress;
        /** 信息安全备案进度(%) */
        private Integer securityProgress;
        /** 成果转化进度(%) */
        private Integer achievementProgress;
    }

    @Data
    public static class ProvinceStats {
        /** 辖区项目总数 */
        private Integer regionProjectCount;
        /** 待审核任务数量 */
        private Integer pendingReviewCount;
        /** 辖区平均进度(%) */
        private Integer regionProgress;
        /** 高风险项目数量 */
        private Integer highRiskCount;
        /** 各地市项目分布 */
        private List<RegionItem> regionDistribution;
        /** 各项目类型分布（基于 dept_id 过滤后的项目聚合） */
        private List<NationalStats.ProjectTypeItem> projectTypeDistribution;
    }

    @Data
    public static class NationalStats {
        /** 全国项目总数 */
        private Integer totalProjectCount;
        /** 待审批任务数量 */
        private Integer pendingApprovalCount;
        /** 全国平均进度(%) */
        private Integer nationalProgress;
        /** 整体资金执行率(%) */
        private Integer totalFundRate;
        /** 各省项目分布 */
        private List<ProvinceItem> provinceDistribution;
        /** 项目类型分布 */
        private List<ProjectTypeItem> projectTypeDistribution;
        /** 已中期评估项目数 */
        private Integer midtermProjectCount;
        /** 已验收项目数 */
        private Integer acceptedProjectCount;
        /** 中央转移资金三年总额（万元） */
        private BigDecimal centralFundThreeYearTotal;
        /** 到账资金总额（万元） */
        private BigDecimal centralFundArriveTotal;

        @Data
        public static class ProjectTypeItem {
            /** 类型名称 */
            private String typeName;
            /** 类型值 */
            private Integer typeValue;
            /** 项目数量 */
            private Integer projectCount;
            /** 占比(%) */
            private Integer percentage;
            /** 该项目类型总投资额（万元） */
            private BigDecimal totalInvestment;
            /** 该项目类型已到账资金（万元） */
            private BigDecimal arrivedFund;
            /** 已完成项目数（actual_end_time IS NOT NULL） */
            private Integer completedCount;
            /** 完成率(%) */
            private Integer completionRate;
        }
    }

    @Data
    public static class ExpertStats {
        /** 待评审任务数量 */
        private Integer pendingReviewCount;
        /** 已完成评审数量 */
        private Integer completedReviewCount;
        /** 本月评审任务数 */
        private Integer monthlyReviewCount;
    }

    @Data
    public static class RegionItem {
        private String regionName;
        private Long regionId;
        private Integer projectCount;
        private Integer progress;
    }

    @Data
    public static class ProvinceItem {
        private String provinceName;
        private Long provinceId;
        private Integer projectCount;
        private Integer progress;
        private Integer fundRate;
    }
}
