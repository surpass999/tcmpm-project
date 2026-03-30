package cn.gemrun.base.module.declare.controller.admin.dashboard;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.*;
import cn.gemrun.base.module.declare.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 驾驶舱数据接口
 *
 * @author Gemini
 */
@Tag(name = "驾驶舱接口")
@RestController
@RequestMapping("/declare/dashboard")
@Validated
@Slf4j
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "获取驾驶舱统计数据")
    @GetMapping("/stats")
    public CommonResult<DashboardStatsVO> getStats() {
        return CommonResult.success(dashboardService.getStats());
    }

    @Operation(summary = "获取待办任务列表")
    @GetMapping("/tasks")
    public CommonResult<DashboardTaskVO> getTasks() {
        return CommonResult.success(dashboardService.getTasks());
    }

    @Operation(summary = "获取项目进度统计")
    @GetMapping("/project-progress")
    public CommonResult<ProjectProgressVO> getProjectProgress() {
        return CommonResult.success(dashboardService.getProjectProgress());
    }

    @Operation(summary = "获取资金统计")
    @GetMapping("/fund-stats")
    public CommonResult<FundStatsVO> getFundStats() {
        return CommonResult.success(dashboardService.getFundStats());
    }

    @Operation(summary = "获取风险预警列表")
    @GetMapping("/risks")
    public CommonResult<RiskWarningVO> getRiskWarnings() {
        return CommonResult.success(dashboardService.getRiskWarnings());
    }

    @Operation(summary = "获取全国统计数据（国家局专用）")
    @GetMapping("/national-stats")
    public CommonResult<NationalStatsVO> getNationalStats() {
        return CommonResult.success(dashboardService.getNationalStats());
    }

    @Operation(summary = "获取当前用户角色")
    @GetMapping("/user-role")
    public CommonResult<String> getUserRole() {
        return CommonResult.success(dashboardService.getCurrentUserRole());
    }

    @Operation(summary = "获取填报窗口统计信息（国家局专用）")
    @GetMapping("/report-window-stats")
    public CommonResult<ReportWindowStatsVO> getReportWindowStats() {
        return CommonResult.success(dashboardService.getReportWindowStats());
    }
}
