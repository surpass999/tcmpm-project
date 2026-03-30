package cn.gemrun.base.module.declare.service.dashboard;

import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.*;

/**
 * 驾驶舱数据服务接口
 *
 * @author Gemini
 */
public interface DashboardService {

    /**
     * 获取驾驶舱统计数据
     */
    DashboardStatsVO getStats();

    /**
     * 获取待办任务列表
     */
    DashboardTaskVO getTasks();

    /**
     * 获取项目进度统计
     */
    ProjectProgressVO getProjectProgress();

    /**
     * 获取资金统计
     */
    FundStatsVO getFundStats();

    /**
     * 获取风险预警列表
     */
    RiskWarningVO getRiskWarnings();

    /**
     * 获取全国统计数据（国家局专用）
     */
    NationalStatsVO getNationalStats();

    /**
     * 获取当前用户角色
     */
    String getCurrentUserRole();

    /**
     * 获取填报窗口统计信息（国家局专用）
     */
    ReportWindowStatsVO getReportWindowStats();
}
