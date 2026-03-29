package cn.gemrun.base.module.declare.service.dashboard.impl;

import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.*;
import cn.gemrun.base.module.declare.service.dashboard.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 驾驶舱数据服务实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Override
    public DashboardStatsVO getStats() {
        return new DashboardStatsVO();
    }

    @Override
    public DashboardTaskVO getTasks() {
        return new DashboardTaskVO();
    }

    @Override
    public ProjectProgressVO getProjectProgress() {
        return new ProjectProgressVO();
    }

    @Override
    public FundStatsVO getFundStats() {
        return new FundStatsVO();
    }

    @Override
    public RiskWarningVO getRiskWarnings() {
        return new RiskWarningVO();
    }

    @Override
    public NationalStatsVO getNationalStats() {
        return new NationalStatsVO();
    }

    @Override
    public String getCurrentUserRole() {
        return "USER";
    }
}
