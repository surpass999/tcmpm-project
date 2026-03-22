package cn.gemrun.base.module.declare.service.dashboard.impl;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.ip.core.Area;
import cn.gemrun.base.framework.ip.core.utils.AreaUtils;
import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.bpm.framework.core.util.BusinessKeyParser;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.dal.dataobject.filing.FilingDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectProcessMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.module.declare.enums.ProcessType;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.enums.ProjectTypeEnum;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import cn.gemrun.base.module.declare.service.dashboard.DashboardService;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.module.system.dal.dataobject.permission.RoleDO;
import cn.gemrun.base.module.system.service.dept.DeptService;
import cn.gemrun.base.module.system.service.permission.PermissionService;
import cn.gemrun.base.module.system.service.permission.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 驾驶舱数据服务实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectProcessMapper projectProcessMapper;

    @Resource
    private ReviewTaskMapper reviewTaskMapper;

    @Resource
    private ExpertService expertService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private PermissionService permissionService;

    @Resource
    private RoleService roleService;

    @Resource
    private DeptService deptService;

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;

    @Resource
    private FilingService filingService;

    @Resource
    private AchievementService achievementService;

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

    @Resource
    private DeclareIndicatorValueMapper indicatorValueMapper;

    private static final String ROLE_HOSPITAL = "hospital";
    private static final String ROLE_PROVINCE = "province";
    private static final String ROLE_NATION = "nation";
    private static final String ROLE_EXPERT = "expert";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getCurrentUserRole() {
        Long userId = getLoginUserId();
        log.info("[Dashboard] getCurrentUserRole 调用，userId={}", userId);
        if (userId == null) {
            log.warn("[Dashboard] userId 为 null，返回 hospital");
            return ROLE_HOSPITAL;
        }

        // 获取用户的所有角色 code
        Set<Long> roleIdSet = permissionService.getUserRoleIdListByUserId(userId);
        log.info("[Dashboard] 用户 {} 拥有的角色ID: {}", userId, roleIdSet);
        if (CollectionUtils.isEmpty(roleIdSet)) {
            log.warn("[Dashboard] 用户 {} 没有任何角色，返回 hospital", userId);
            return ROLE_HOSPITAL;
        }

        List<RoleDO> roles = roleService.getRoleListFromCache(roleIdSet);
        Set<String> roleCodes = roles.stream()
                .map(RoleDO::getCode)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());
        log.info("[Dashboard] 用户 {} 的角色码(roleCodes): {}", userId, roleCodes);

        // 按优先级判断：专家 > 省级 > 国家局 > 医院
        if (roleCodes.contains("expert")) {
            log.info("[Dashboard] 匹配到 expert，返回 expert");
            return ROLE_EXPERT;
        }
        if (roleCodes.contains("province")) {
            log.info("[Dashboard] 匹配到 province，返回 province");
            return ROLE_PROVINCE;
        }
        if (roleCodes.contains("nation")) {
            log.info("[Dashboard] 匹配到 nation，返回 nation");
            return ROLE_NATION;
        }
        if (roleCodes.contains("hospital")) {
            log.info("[Dashboard] 匹配到 hospital，返回 hospital");
            return ROLE_HOSPITAL;
        }

        // 没有匹配到项目专用角色，按数据权限兜底
        // 超级管理员默认国家局视角
        if (roleCodes.contains("super_admin") || roleCodes.contains("admin")) {
            log.info("[Dashboard] 匹配到管理员角色，返回 nation");
            return ROLE_NATION;
        }

        log.warn("[Dashboard] 未匹配到任何已知角色，返回 hospital");
        return ROLE_HOSPITAL;
    }

    @Override
    public DashboardStatsVO getStats() {
        String userRole = getCurrentUserRole();
        Long userId = getLoginUserId();
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setUserRole(userRole);

        // 基础统计
        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);
        vo.setProjectCount(projects.size());

        // 计算平均进度
        int avgProgress = calculateAverageProgress(projects);
        vo.setProjectProgress(avgProgress);

        // 计算资金执行率
        int fundRate = calculateFundExecutionRate(projects);
        vo.setFundExecutionRate(fundRate);

        // 预警数量
        int warningCount = getWarningCount(projects);
        vo.setWarningCount(warningCount);

        // 根据角色设置专属数据
        switch (userRole) {
            case ROLE_HOSPITAL:
                vo.setHospitalStats(getHospitalStats(projects));
                vo.setTaskCount(vo.getHospitalStats().getDraftReportCount());
                break;
            case ROLE_PROVINCE:
                vo.setProvinceStats(getProvinceStats(projects, userId));
                vo.setTaskCount(vo.getProvinceStats().getPendingReviewCount());
                break;
            case ROLE_NATION:
                vo.setNationalStats(getNationalStats(projects, userId));
                vo.setTaskCount(vo.getNationalStats().getPendingApprovalCount());
                break;
            case ROLE_EXPERT:
                vo.setExpertStats(getExpertStats(userId));
                vo.setTaskCount(vo.getExpertStats().getPendingReviewCount());
                break;
        }

        return vo;
    }

    @Override
    public DashboardTaskVO getTasks() {
        DashboardTaskVO vo = new DashboardTaskVO();
        String userRole = getCurrentUserRole();
        Long userId = getLoginUserId();

        // BPM待办任务 - TODO: 调用BPM服务获取实际待办
        DashboardTaskVO.TaskList bpmTasks = getBpmTasks(userRole, userId);
        vo.setBpmTasks(bpmTasks);

        // 业务待办（草稿状态）
        DashboardTaskVO.TaskList draftTasks = getDraftTasks(userRole, userId);
        vo.setDraftTasks(draftTasks);

        // 预警任务
        DashboardTaskVO.TaskList warningTasks = getWarningTasks(userRole, userId);
        vo.setWarningTasks(warningTasks);

        return vo;
    }

    @Override
    public ProjectProgressVO getProjectProgress() {
        ProjectProgressVO vo = new ProjectProgressVO();
        String userRole = getCurrentUserRole();
        Long userId = getLoginUserId();

        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);

        vo.setTotalProjectCount(projects.size());
        vo.setAverageProgress(calculateAverageProgress(projects));

        return vo;
    }

    @Override
    public FundStatsVO getFundStats() {
        FundStatsVO vo = new FundStatsVO();
        String userRole = getCurrentUserRole();
        Long userId = getLoginUserId();

        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);

        // 计算资金统计
        BigDecimal totalFund = BigDecimal.ZERO;
        BigDecimal executedFund = BigDecimal.ZERO;

        for (ProjectDO project : projects) {
            if (project.getTotalInvestment() != null) {
                totalFund = totalFund.add(project.getTotalInvestment());
            }
            if (project.getAccumulatedInvestment() != null) {
                executedFund = executedFund.add(project.getAccumulatedInvestment());
            }
        }

        vo.setTotalFund(totalFund);
        vo.setExecutedFund(executedFund);

        int executionRate = 0;
        if (totalFund.compareTo(BigDecimal.ZERO) > 0) {
            executionRate = executedFund.divide(totalFund, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
        }
        vo.setExecutionRate(executionRate);

        return vo;
    }

    @Override
    public RiskWarningVO getRiskWarnings() {
        RiskWarningVO vo = new RiskWarningVO();
        String userRole = getCurrentUserRole();
        Long userId = getLoginUserId();

        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);
        List<RiskWarningVO.WarningItem> warnings = new ArrayList<>();

        for (ProjectDO project : projects) {
            // 检查进度预警
            if (project.getActualProgress() != null && project.getActualProgress() < 50) {
                RiskWarningVO.WarningItem warning = new RiskWarningVO.WarningItem();
                warning.setId(project.getId());
                warning.setWarningType("progress");
                warning.setLevel(project.getActualProgress() < 30 ? "high" : "medium");
                warning.setTitle("项目进度滞后");
                warning.setDescription("项目【" + project.getProjectName() + "】进度为" + project.getActualProgress() + "%，低于预期");
                warning.setProjectId(project.getId());
                warning.setProjectName(project.getProjectName());
                warning.setWarningTime(LocalDateTime.now().format(DATE_FORMATTER));
                warning.setStatus("pending");
                warning.setJumpUrl("/declare/project/" + project.getId());
                warnings.add(warning);
            }

            // 检查资金预警
            if (project.getTotalInvestment() != null && project.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0
                    && project.getAccumulatedInvestment() != null) {
                int fundRate = project.getAccumulatedInvestment()
                        .divide(project.getTotalInvestment(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).intValue();
                if (fundRate < 30) {
                    RiskWarningVO.WarningItem warning = new RiskWarningVO.WarningItem();
                    warning.setId(project.getId() + 10000L);
                    warning.setWarningType("fund");
                    warning.setLevel(fundRate < 15 ? "high" : "medium");
                    warning.setTitle("资金执行滞后");
                    warning.setDescription("项目【" + project.getProjectName() + "】资金执行率为" + fundRate + "%，低于预期");
                    warning.setProjectId(project.getId());
                    warning.setProjectName(project.getProjectName());
                    warning.setWarningTime(LocalDateTime.now().format(DATE_FORMATTER));
                    warning.setStatus("pending");
                    warning.setJumpUrl("/declare/project/" + project.getId());
                    warnings.add(warning);
                }
            }
        }

        // 分类统计
        long highCount = warnings.stream().filter(w -> "high".equals(w.getLevel())).count();
        long mediumCount = warnings.stream().filter(w -> "medium".equals(w.getLevel())).count();
        long lowCount = warnings.stream().filter(w -> "low".equals(w.getLevel())).count();

        vo.setTotalCount(warnings.size());
        vo.setHighRiskCount((int) highCount);
        vo.setMediumRiskCount((int) mediumCount);
        vo.setLowRiskCount((int) lowCount);
        vo.setWarnings(warnings);

        return vo;
    }

    @Override
    public NationalStatsVO getNationalStats() {
        NationalStatsVO vo = new NationalStatsVO();

        // 获取所有项目
        List<ProjectDO> allProjects = projectMapper.selectList(null);
        vo.setTotalProjectCount(allProjects.size());

        // 计算全国平均进度
        int avgProgress = calculateAverageProgress(allProjects);
        vo.setAverageProgress(avgProgress);

        // 计算资金执行率
        int fundRate = calculateFundExecutionRate(allProjects);
        vo.setTotalFundRate(fundRate);

        // 新增：已中期评估项目数
        Long midtermCount = projectMapper.selectCountByStatus(ProjectStatus.MIDTERM.getStatus());
        vo.setMidtermProjectCount(midtermCount != null ? midtermCount.intValue() : 0);

        // 修复：已验收项目数（使用 actual_end_time 判断）
        Long acceptedCount = projectMapper.selectCountByActualEndTime();
        vo.setAcceptedProjectCount(acceptedCount != null ? acceptedCount.intValue() : 0);

        // 新增：项目类型分布（使用 XML 聚合查询，补充缺失类型确保显示所有6种）
        List<DashboardStatsVO.NationalStats.ProjectTypeItem> typeItems = projectMapper.selectProjectTypeAggregation();

        // 转换为 Map，方便查找
        Map<Integer, DashboardStatsVO.NationalStats.ProjectTypeItem> typeItemMap = typeItems.stream()
                .collect(Collectors.toMap(DashboardStatsVO.NationalStats.ProjectTypeItem::getTypeValue, item -> item, (a, b) -> a));

        // 补充缺失的项目类型（枚举值 1-6）
        List<DashboardStatsVO.NationalStats.ProjectTypeItem> fullTypeItems = new ArrayList<>();
        int totalCount = vo.getTotalProjectCount();

        for (ProjectTypeEnum typeEnum : ProjectTypeEnum.values()) {
            Integer typeValue = typeEnum.getType();
            // 跳过 null 和 0（通用类型），只显示 1-6
            if (typeValue == null || typeValue == 0) {
                continue;
            }

            DashboardStatsVO.NationalStats.ProjectTypeItem item = typeItemMap.get(typeValue);
            if (item == null) {
                // 该类型没有项目，创建空项目计数
                item = new DashboardStatsVO.NationalStats.ProjectTypeItem();
                item.setTypeValue(typeValue);
                item.setProjectCount(0);
                item.setTotalInvestment(BigDecimal.ZERO);
                item.setArrivedFund(BigDecimal.ZERO);
                item.setCompletedCount(0);
            }

            // 设置名称和百分比
            item.setTypeName(typeEnum.getName());
            int pct = totalCount > 0 ? item.getProjectCount() * 100 / totalCount : 0;
            item.setPercentage(pct);

            // 新增：设置完成率和已完成数量
            item.setCompletedCount(item.getCompletedCount() != null ? item.getCompletedCount() : 0);
            int completed = item.getCompletedCount();
            int rate = totalCount > 0 ? (completed * 100 / totalCount) : 0;
            item.setCompletionRate(rate);

            fullTypeItems.add(item);
        }

        vo.setProjectTypeDistribution(fullTypeItems);

        // 新增：资金统计
        // 固定值：10.56亿 = 105600 万元
        vo.setCentralFundThreeYearTotal(new BigDecimal("105600"));

        // 动态汇总所有项目的 centralFundArrive
        BigDecimal totalArrive = allProjects.stream()
                .map(ProjectDO::getCentralFundArrive)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setCentralFundArriveTotal(totalArrive);

        // 新增：省份分布（使用 XML 聚合查询）
        List<NationalStatsVO.ProvinceItem> provinceItems = projectMapper.selectProvinceAggregation();
        if (CollectionUtils.isEmpty(provinceItems)) {
            provinceItems = Collections.emptyList();
        } else {
            // 补充省份名称（从 AreaUtils 获取）
            for (NationalStatsVO.ProvinceItem item : provinceItems) {
                Area area = AreaUtils.getArea(item.getProvinceId().intValue());
                item.setProvinceName(area != null ? area.getName() : "未知省份");
            }
        }
        vo.setProvinceDistribution(provinceItems);

        return vo;
    }

    // ====== 私有方法 ======

    private Long getLoginUserId() {
        try {
            return WebFrameworkUtils.getLoginUserId();
        } catch (Exception e) {
            log.warn("获取登录用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    private List<ProjectDO> getProjectsByUserRole(String userRole, Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        switch (userRole) {
            case ROLE_HOSPITAL:
                // 医院用户：只看自己负责的项目
                return projectMapper.selectList(
                        new LambdaQueryWrapper<ProjectDO>()
                                .eq(ProjectDO::getLeaderUserId, userId)
                );
            case ROLE_PROVINCE:
                // 省级用户：看辖区内所有项目（含子部门）
                AdminUserRespDTO user = adminUserApi.getUser(userId);
                if (user != null && user.getDeptId() != null) {
                    Set<Long> deptIds = new HashSet<>();
                    deptIds.add(user.getDeptId());
                    Set<Long> childDeptIds = deptService.getChildDeptIdListFromCache(user.getDeptId());
                    if (childDeptIds != null) {
                        deptIds.addAll(childDeptIds);
                    }
                    return projectMapper.selectList(
                            new LambdaQueryWrapper<ProjectDO>()
                                    .in(ProjectDO::getDeptId, deptIds)
                    );
                }
                return Collections.emptyList();
            case ROLE_NATION:
                // 国家局：看所有项目
                return projectMapper.selectList(null);
            default:
                return Collections.emptyList();
        }
    }

    private int calculateAverageProgress(List<ProjectDO> projects) {
        if (CollectionUtils.isEmpty(projects)) {
            return 0;
        }

        int totalProgress = 0;
        int count = 0;

        for (ProjectDO project : projects) {
            if (project.getActualProgress() != null) {
                totalProgress += project.getActualProgress();
                count++;
            }
        }

        return count > 0 ? totalProgress / count : 0;
    }

    private int calculateFundExecutionRate(List<ProjectDO> projects) {
        if (CollectionUtils.isEmpty(projects)) {
            return 0;
        }

        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalExecuted = BigDecimal.ZERO;

        for (ProjectDO project : projects) {
            if (project.getTotalInvestment() != null) {
                totalBudget = totalBudget.add(project.getTotalInvestment());
            }
            if (project.getAccumulatedInvestment() != null) {
                totalExecuted = totalExecuted.add(project.getAccumulatedInvestment());
            }
        }

        if (totalBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return totalExecuted.divide(totalBudget, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).intValue();
    }

    private int getWarningCount(List<ProjectDO> projects) {
        int count = 0;
        for (ProjectDO project : projects) {
            if (project.getActualProgress() != null && project.getActualProgress() < 50) {
                count++;
            }
        }
        return count;
    }

    private DashboardStatsVO.HospitalStats getHospitalStats(List<ProjectDO> projects) {
        DashboardStatsVO.HospitalStats stats = new DashboardStatsVO.HospitalStats();
        stats.setMyProjectCount(projects.size());

        // 查询该用户负责的项目的过程记录
        List<Long> projectIds = projects.stream().map(ProjectDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            stats.setDraftReportCount(0);
            stats.setReviewingReportCount(0);
            stats.setCompletedReportCount(0);
            stats.setUrgentTaskCount(0);
            // 无项目时，四维度进度全部为0
            stats.setDimensionProgress(createEmptyDimensionProgress());
            return stats;
        }

        // 草稿状态报告
        long draftCount = projectProcessMapper.selectCount(
                new LambdaQueryWrapper<ProjectProcessDO>()
                        .in(ProjectProcessDO::getProjectId, projectIds)
                        .eq(ProjectProcessDO::getStatus, "DRAFT")
        );

        // 审核中报告
        long reviewingCount = projectProcessMapper.selectCount(
                new LambdaQueryWrapper<ProjectProcessDO>()
                        .in(ProjectProcessDO::getProjectId, projectIds)
                        .in(ProjectProcessDO::getStatus, "SUBMITTED", "REVIEWING")
        );

        // 已完成报告
        long completedCount = projectProcessMapper.selectCount(
                new LambdaQueryWrapper<ProjectProcessDO>()
                        .in(ProjectProcessDO::getProjectId, projectIds)
                        .eq(ProjectProcessDO::getStatus, "COMPLETED")
        );

        stats.setDraftReportCount((int) draftCount);
        stats.setReviewingReportCount((int) reviewingCount);
        stats.setCompletedReportCount((int) completedCount);
        stats.setUrgentTaskCount(getUrgentTaskCount(projects));

        // 计算四维度进度（取第一个项目的进度）
        Long primaryProjectId = projectIds.get(0);
        DashboardStatsVO.DimensionProgress dimensionProgress = calculateDimensionProgress(primaryProjectId);
        stats.setDimensionProgress(dimensionProgress);

        return stats;
    }

    private int getUrgentTaskCount(List<ProjectDO> projects) {
        // 截止日期在7天内的任务
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);
        List<Long> projectIds = projects.stream().map(ProjectDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            return 0;
        }

        return projectProcessMapper.selectCount(
                new LambdaQueryWrapper<ProjectProcessDO>()
                        .in(ProjectProcessDO::getProjectId, projectIds)
                        .eq(ProjectProcessDO::getStatus, "DRAFT")
                        .le(ProjectProcessDO::getReportPeriodEnd, deadline)
        ).intValue();
    }

    private DashboardStatsVO.ProvinceStats getProvinceStats(List<ProjectDO> projects, Long userId) {
        DashboardStatsVO.ProvinceStats stats = new DashboardStatsVO.ProvinceStats();
        stats.setRegionProjectCount(projects.size());
        stats.setRegionProgress(calculateAverageProgress(projects));
        stats.setHighRiskCount(getWarningCount(projects));

        // 从BPM服务获取待审核任务数
        stats.setPendingReviewCount(getBpmTodoCount(userId));

        // 各项目类型分布（基于 dept_id 过滤后的项目列表聚合，逻辑同 getNationalStats）
        List<DashboardStatsVO.NationalStats.ProjectTypeItem> typeItems = buildProjectTypeDistribution(projects);
        stats.setProjectTypeDistribution(typeItems);

        return stats;
    }

    /**
     * 构建项目类型分布聚合数据（供省级和国家局共用）
     */
    private List<DashboardStatsVO.NationalStats.ProjectTypeItem> buildProjectTypeDistribution(List<ProjectDO> projects) {
        // 转换为 Map，按 typeValue 分组
        Map<Integer, List<ProjectDO>> projectsByType = projects.stream()
                .filter(p -> p.getProjectType() != null && p.getProjectType() != 0)
                .collect(Collectors.groupingBy(ProjectDO::getProjectType));

        List<DashboardStatsVO.NationalStats.ProjectTypeItem> result = new ArrayList<>();
        int totalCount = projects.size();

        for (ProjectTypeEnum typeEnum : ProjectTypeEnum.values()) {
            Integer typeValue = typeEnum.getType();
            // 跳过 null 和 0（通用类型），只显示 1-6
            if (typeValue == null || typeValue == 0) {
                continue;
            }

            List<ProjectDO> typeProjects = projectsByType.getOrDefault(typeValue, Collections.emptyList());
            int count = typeProjects.size();

            DashboardStatsVO.NationalStats.ProjectTypeItem item = new DashboardStatsVO.NationalStats.ProjectTypeItem();
            item.setTypeValue(typeValue);
            item.setTypeName(typeEnum.getName());
            item.setProjectCount(count);
            item.setPercentage(totalCount > 0 ? count * 100 / totalCount : 0);

            // 计算总投资额和到账资金
            BigDecimal totalInvestment = typeProjects.stream()
                    .map(ProjectDO::getTotalInvestment)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            item.setTotalInvestment(totalInvestment);

            BigDecimal arrivedFund = typeProjects.stream()
                    .map(ProjectDO::getCentralFundArrive)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            item.setArrivedFund(arrivedFund);

            // 已完成数量：actualEndTime 非空
            long completedCount = typeProjects.stream()
                    .filter(p -> p.getActualEndTime() != null)
                    .count();
            item.setCompletedCount((int) completedCount);
            item.setCompletionRate(count > 0 ? (int) (completedCount * 100 / count) : 0);

            result.add(item);
        }

        return result;
    }

    private DashboardStatsVO.NationalStats getNationalStats(List<ProjectDO> projects, Long userId) {
        DashboardStatsVO.NationalStats stats = new DashboardStatsVO.NationalStats();
        stats.setTotalProjectCount(projects.size());
        stats.setNationalProgress(calculateAverageProgress(projects));
        stats.setTotalFundRate(calculateFundExecutionRate(projects));

        // 从BPM服务获取待审批任务数
        stats.setPendingApprovalCount(getBpmTodoCount(userId));

        // 项目类型分布（补充缺失类型确保显示所有6种）
        List<DashboardStatsVO.NationalStats.ProjectTypeItem> typeItems = projectMapper.selectProjectTypeAggregation();

        // 转换为 Map，方便查找
        Map<Integer, DashboardStatsVO.NationalStats.ProjectTypeItem> typeItemMap = typeItems.stream()
                .collect(Collectors.toMap(DashboardStatsVO.NationalStats.ProjectTypeItem::getTypeValue, item -> item, (a, b) -> a));

        // 补充缺失的项目类型（枚举值 1-6）
        List<DashboardStatsVO.NationalStats.ProjectTypeItem> fullTypeItems = new ArrayList<>();
        int totalCount = stats.getTotalProjectCount();

        for (ProjectTypeEnum typeEnum : ProjectTypeEnum.values()) {
            Integer typeValue = typeEnum.getType();
            // 跳过 null 和 0（通用类型），只显示 1-6
            if (typeValue == null || typeValue == 0) {
                continue;
            }

            DashboardStatsVO.NationalStats.ProjectTypeItem item = typeItemMap.get(typeValue);
            if (item == null) {
                // 该类型没有项目，创建空项目计数
                item = new DashboardStatsVO.NationalStats.ProjectTypeItem();
                item.setTypeValue(typeValue);
                item.setProjectCount(0);
                item.setTotalInvestment(BigDecimal.ZERO);
                item.setArrivedFund(BigDecimal.ZERO);
                item.setCompletedCount(0);
            }

            // 设置名称和百分比
            item.setTypeName(typeEnum.getName());
            int pct = totalCount > 0 ? item.getProjectCount() * 100 / totalCount : 0;
            item.setPercentage(pct);

            // 新增：设置完成率和已完成数量
            item.setCompletedCount(item.getCompletedCount() != null ? item.getCompletedCount() : 0);
            int completed = item.getCompletedCount();
            int rate = totalCount > 0 ? (completed * 100 / totalCount) : 0;
            item.setCompletionRate(rate);

            fullTypeItems.add(item);
        }

        stats.setProjectTypeDistribution(fullTypeItems);

        // 已中期评估项目数
        Long midtermCount = projectMapper.selectCountByStatus(ProjectStatus.MIDTERM.getStatus());
        stats.setMidtermProjectCount(midtermCount != null ? midtermCount.intValue() : 0);

        // 修复：已验收项目数（使用 actual_end_time 判断）
        Long acceptedCount = projectMapper.selectCountByActualEndTime();
        stats.setAcceptedProjectCount(acceptedCount != null ? acceptedCount.intValue() : 0);

        // 中央转移资金三年总额（万元），固定值 105600
        stats.setCentralFundThreeYearTotal(new BigDecimal("105600"));

        // 动态汇总所有项目的 centralFundArrive
        BigDecimal totalArrive = projects.stream()
                .map(ProjectDO::getCentralFundArrive)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setCentralFundArriveTotal(totalArrive);

        return stats;
    }

    private DashboardStatsVO.ExpertStats getExpertStats(Long userId) {
        DashboardStatsVO.ExpertStats stats = new DashboardStatsVO.ExpertStats();

        // 检查用户是否关联了专家
        if (userId == null) {
            stats.setPendingReviewCount(0);
            stats.setCompletedReviewCount(0);
            stats.setMonthlyReviewCount(0);
            return stats;
        }

        ExpertDO expert = expertService.getExpertByUserId(userId);
        if (expert == null) {
            // 未找到关联专家，返回空数据
            stats.setPendingReviewCount(0);
            stats.setCompletedReviewCount(0);
            stats.setMonthlyReviewCount(0);
            return stats;
        }

        // 查询待评审任务（状态为1=评审中，且用户ID在expertIds中）
        // 注意：expertIds 字段存储的是 userId，不是 ExpertDO.id
        LambdaQueryWrapper<ReviewTaskDO> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ReviewTaskDO::getStatus, 1) // 评审中
                .like(ReviewTaskDO::getExpertIds, String.valueOf(userId));
        long pendingCount = reviewTaskMapper.selectCount(pendingWrapper);
        stats.setPendingReviewCount((int) pendingCount);

        // 查询已完成评审任务（状态为2=已完成）
        LambdaQueryWrapper<ReviewTaskDO> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(ReviewTaskDO::getStatus, 2) // 已完成
                .like(ReviewTaskDO::getExpertIds, String.valueOf(userId));
        long completedCount = reviewTaskMapper.selectCount(completedWrapper);
        stats.setCompletedReviewCount((int) completedCount);

        // 查询本月评审任务（本月开始的任务）
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<ReviewTaskDO> monthlyWrapper = new LambdaQueryWrapper<>();
        monthlyWrapper.eq(ReviewTaskDO::getStatus, 2) // 已完成
                .like(ReviewTaskDO::getExpertIds, String.valueOf(userId))
                .ge(ReviewTaskDO::getStartTime, monthStart);
        long monthlyCount = reviewTaskMapper.selectCount(monthlyWrapper);
        stats.setMonthlyReviewCount((int) monthlyCount);

        return stats;
    }

    private DashboardTaskVO.TaskList getBpmTasks(String userRole, Long userId) {
        DashboardTaskVO.TaskList taskList = new DashboardTaskVO.TaskList();

        // 专家角色：返回评审任务列表
        if (ROLE_EXPERT.equals(userRole) && userId != null) {
            return getExpertReviewTasks(userId);
        }

        // nation/province/hospital 角色：调用 BPM 服务获取待办任务
        if (userId != null) {
            return getBpmTodoTasks();
        }

        taskList.setTotal(0);
        taskList.setItems(Collections.emptyList());
        return taskList;
    }

    /**
     * 调用 BPM 服务获取待办任务列表
     */
    private DashboardTaskVO.TaskList getBpmTodoTasks() {
        DashboardTaskVO.TaskList taskList = new DashboardTaskVO.TaskList();
        Long userId = getLoginUserId();
        if (userId == null) {
            taskList.setTotal(0);
            taskList.setItems(Collections.emptyList());
            return taskList;
        }

        try {
            // 调用 BPM 待办分页接口，获取前5条
            PageResult<Task> pageResult = bpmTaskService.getTaskTodoPage(userId,
                    new cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO());
            List<Task> tasks = pageResult != null && pageResult.getList() != null
                    ? pageResult.getList() : Collections.emptyList();

            // 批量查询项目名称
            Map<String, String> projectNameMap = buildProjectNameMap(tasks);

            List<DashboardTaskVO.TaskItem> items = new ArrayList<>();
            for (Task task : tasks) {
                DashboardTaskVO.TaskItem item = new DashboardTaskVO.TaskItem();
                item.setTaskId(task.getId());
                item.setProcessInstanceId(task.getProcessInstanceId());
                item.setTaskName(task.getName());
                item.setProcessTitle(task.getName());
                // 根据流程实例ID获取项目名称
                item.setProjectName(projectNameMap.getOrDefault(task.getProcessInstanceId(), ""));
                item.setTaskType("bpm_task");
                item.setStatus("pending");
                if (task.getCreateTime() != null) {
                    item.setCreateTime(task.getCreateTime().toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMATTER));
                }
                item.setJumpUrl("");
                items.add(item);
            }

            taskList.setTotal(pageResult != null && pageResult.getTotal() != null ? pageResult.getTotal().intValue() : 0);
            taskList.setItems(items);
        } catch (Exception e) {
            log.warn("[Dashboard] 调用BPM待办接口失败: {}", e.getMessage());
            taskList.setTotal(0);
            taskList.setItems(Collections.emptyList());
        }
        return taskList;
    }

    /**
     * 根据任务列表构建流程实例ID -> 项目名称 的映射
     */
    private Map<String, String> buildProjectNameMap(List<Task> tasks) {
        Map<String, String> result = new HashMap<>();
        if (CollectionUtils.isEmpty(tasks)) {
            return result;
        }

        // 1. 收集所有流程实例ID
        List<String> processInstanceIds = tasks.stream()
                .map(Task::getProcessInstanceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(processInstanceIds)) {
            return result;
        }

        // 2. 批量查询流程实例，获取 businessKey
        Map<String, ProcessInstance> processInstanceMap = bpmProcessInstanceService
                .getProcessInstanceMap(new HashSet<>(processInstanceIds));

        // 3. 解析 businessKey，获取过程记录ID
        Map<String, Long> processInstanceToProcessId = new HashMap<>();
        Map<Long, Long> processIdToProjectId = new HashMap<>();
        List<Long> processIds = new ArrayList<>();

        for (Map.Entry<String, ProcessInstance> entry : processInstanceMap.entrySet()) {
            String processInstanceId = entry.getKey();
            ProcessInstance pi = entry.getValue();
            if (pi == null || pi.getBusinessKey() == null) {
                continue;
            }
            try {
                String processDefKey = BusinessKeyParser.extractProcessDefinitionKey(pi.getBusinessKey());
                Long processId = BusinessKeyParser.parseBusinessId(pi.getBusinessKey(), processDefKey);
                processInstanceToProcessId.put(processInstanceId, processId);
                processIds.add(processId);
            } catch (Exception e) {
                log.debug("[Dashboard] 解析businessKey失败: {}", pi.getBusinessKey(), e);
            }
        }

        if (CollectionUtils.isEmpty(processIds)) {
            return result;
        }

        // 4. 批量查询过程记录，获取 projectId
        List<ProjectProcessDO> processList = projectProcessMapper.selectBatchIds(processIds);
        for (ProjectProcessDO process : processList) {
            if (process.getProjectId() != null) {
                processIdToProjectId.put(process.getId(), process.getProjectId());
            }
        }

        // 5. 批量查询项目，获取项目名称
        List<Long> projectIds = processIdToProjectId.values().stream().distinct().collect(Collectors.toList());
        List<ProjectDO> projectList = projectMapper.selectBatchIds(projectIds);
        Map<Long, String> projectNameMap = new HashMap<>();
        for (ProjectDO project : projectList) {
            projectNameMap.put(project.getId(), project.getProjectName());
        }

        // 6. 构建结果：流程实例ID -> 项目名称
        for (Map.Entry<String, Long> entry : processInstanceToProcessId.entrySet()) {
            Long processId = entry.getValue();
            Long projectId = processIdToProjectId.get(processId);
            if (projectId != null) {
                String projectName = projectNameMap.get(projectId);
                if (projectName != null) {
                    result.put(entry.getKey(), projectName);
                }
            }
        }

        return result;
    }

    /**
     * 获取 BPM 待办任务数量
     */
    private int getBpmTodoCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        try {
            PageResult<Task> pageResult = bpmTaskService.getTaskTodoPage(userId,
                    new cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO());
            return pageResult != null && pageResult.getTotal() != null ? pageResult.getTotal().intValue() : 0;
        } catch (Exception e) {
            log.warn("[Dashboard] 获取BPM待办数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取专家的评审任务列表
     */
    private DashboardTaskVO.TaskList getExpertReviewTasks(Long userId) {
        DashboardTaskVO.TaskList taskList = new DashboardTaskVO.TaskList();

        // 检查用户是否关联了专家
        if (userId == null) {
            taskList.setTotal(0);
            taskList.setItems(Collections.emptyList());
            return taskList;
        }

        ExpertDO expert = expertService.getExpertByUserId(userId);
        if (expert == null) {
            taskList.setTotal(0);
            taskList.setItems(Collections.emptyList());
            return taskList;
        }

        // 查询待评审任务（状态为1=评审中，且用户ID在expertIds中）
        // 注意：expertIds 字段存储的是 userId，不是 ExpertDO.id
        LambdaQueryWrapper<ReviewTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReviewTaskDO::getStatus, 1) // 评审中
                .like(ReviewTaskDO::getExpertIds, String.valueOf(userId))
                .orderByAsc(ReviewTaskDO::getEndTime);

        List<ReviewTaskDO> reviewTasks = reviewTaskMapper.selectList(wrapper);
        List<DashboardTaskVO.TaskItem> items = new ArrayList<>();

        for (ReviewTaskDO task : reviewTasks) {
            DashboardTaskVO.TaskItem item = new DashboardTaskVO.TaskItem();
            item.setTaskId(String.valueOf(task.getId()));
            item.setProcessId(task.getBusinessId());
            item.setProcessType(task.getTaskType());
            item.setTaskName(task.getTaskName());
            item.setProcessTitle(task.getTaskName());
            item.setProjectName(getProjectDisplayName(task.getBusinessType(), task.getBusinessId()));
            item.setProjectId(task.getBusinessId());
            item.setTaskType("bpm_task");
            item.setStatus("pending");

            if (task.getStartTime() != null) {
                item.setCreateTime(task.getStartTime().format(DATE_FORMATTER));
            }

            if (task.getEndTime() != null) {
                item.setDeadline(task.getEndTime().format(DATE_FORMATTER));
                // 判断是否紧急（3天内到期）
                item.setUrgent(task.getEndTime().isBefore(LocalDateTime.now().plusDays(3)));
            }

            item.setJumpUrl("/review");
            items.add(item);
        }

        taskList.setTotal(items.size());
        taskList.setItems(items);
        return taskList;
    }

    /**
     * 获取评审任务的展示名称（所属项目列）
     * 格式：备案=项目名称，项目过程=项目名称-过程标题，成果=项目名称-成果标题
     */
    private String getProjectDisplayName(Integer businessType, Long businessId) {
        if (businessType == null || businessId == null) {
            return "未知";
        }

        ProjectDO project = null;
        String subName = "";

        switch (businessType) {
            case 1: { // 备案
                FilingDO filing = filingService.getFiling(businessId);
                if (filing == null || filing.getProjectId() == null) {
                    return "未知备案";
                }
                project = projectMapper.selectById(filing.getProjectId());
                break;
            }
            case 2: { // 项目过程
                ProjectProcessDO process = projectProcessMapper.selectById(businessId);
                if (process == null || process.getProjectId() == null) {
                    return "未知过程";
                }
                project = projectMapper.selectById(process.getProjectId());
                subName = process.getProcessTitle();
                break;
            }
            case 3: { // 成果
                AchievementDO achievement = achievementService.getAchievement(businessId);
                if (achievement == null || achievement.getProjectId() == null) {
                    return "未知成果";
                }
                project = projectMapper.selectById(achievement.getProjectId());
                subName = achievement.getAchievementName();
                break;
            }
            default:
                return "未知业务";
        }

        if (project == null) {
            return "未知项目";
        }

        // 拼接返回：备案=项目名称，项目过程=项目名称-过程标题，成果=项目名称-成果标题
        if (subName == null || subName.isEmpty()) {
            return project.getProjectName();
        }
        return project.getProjectName() + " - " + subName;
    }

    private DashboardTaskVO.TaskList getDraftTasks(String userRole, Long userId) {
        DashboardTaskVO.TaskList taskList = new DashboardTaskVO.TaskList();
        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);
        List<Long> projectIds = projects.stream().map(ProjectDO::getId).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(projectIds)) {
            taskList.setTotal(0);
            taskList.setItems(Collections.emptyList());
            return taskList;
        }

        List<ProjectProcessDO> drafts = projectProcessMapper.selectList(
                new LambdaQueryWrapper<ProjectProcessDO>()
                        .in(ProjectProcessDO::getProjectId, projectIds)
                        .eq(ProjectProcessDO::getStatus, "DRAFT")
                        .orderByAsc(ProjectProcessDO::getReportPeriodEnd)
        );

        List<DashboardTaskVO.TaskItem> items = new ArrayList<>();
        for (ProjectProcessDO draft : drafts) {
            DashboardTaskVO.TaskItem item = new DashboardTaskVO.TaskItem();
            item.setTaskId(String.valueOf(draft.getId()));
            item.setProcessId(draft.getId());
            item.setProcessType(draft.getProcessType());
            item.setTaskName(getProcessTypeName(draft.getProcessType()));
            item.setProcessTitle(draft.getProcessTitle());
            item.setTaskType("draft_report");
            item.setStatus("pending");

            // 获取项目名称
            ProjectDO project = projects.stream()
                    .filter(p -> p.getId().equals(draft.getProjectId()))
                    .findFirst().orElse(null);
            if (project != null) {
                item.setProjectName(project.getProjectName());
                item.setProjectId(project.getId());
            }

            if (draft.getReportPeriodEnd() != null) {
                item.setDeadline(draft.getReportPeriodEnd().format(DATE_FORMATTER));
                // 判断是否紧急（3天内到期）
                item.setUrgent(draft.getReportPeriodEnd().isBefore(LocalDateTime.now().plusDays(3)));
            }

            item.setJumpUrl("/declare/project-process/" + draft.getId());
            items.add(item);
        }

        taskList.setTotal(items.size());
        taskList.setItems(items);
        return taskList;
    }

    private DashboardTaskVO.TaskList getWarningTasks(String userRole, Long userId) {
        DashboardTaskVO.TaskList taskList = new DashboardTaskVO.TaskList();
        List<ProjectDO> projects = getProjectsByUserRole(userRole, userId);

        List<DashboardTaskVO.TaskItem> items = new ArrayList<>();

        for (ProjectDO project : projects) {
            // 进度预警
            if (project.getActualProgress() != null && project.getActualProgress() < 50) {
                DashboardTaskVO.TaskItem item = new DashboardTaskVO.TaskItem();
                item.setTaskId("progress_" + project.getId());
                item.setProjectId(project.getId());
                item.setProjectName(project.getProjectName());
                item.setTaskName("进度预警");
                item.setTaskType("warning");
                item.setStatus("pending");
                item.setUrgent(project.getActualProgress() < 30);
                item.setJumpUrl("/declare/project/" + project.getId());
                items.add(item);
            }
        }

        taskList.setTotal(items.size());
        taskList.setItems(items);
        return taskList;
    }

    private String getProcessTypeName(Integer processType) {
        if (processType == null) {
            return "未知";
        }
        ProcessType type = ProcessType.valueOf(processType);
        return type != null ? type.getName() : "未知";
    }

    // ==================== 四维度进度计算方法 ====================

    /**
     * 计算项目四维度进度
     * 基于指标体系计算各维度进度
     *
     * @param projectId 项目ID
     * @return 四维度进度
     */
    public DashboardStatsVO.DimensionProgress calculateDimensionProgress(Long projectId) {
        DashboardStatsVO.DimensionProgress progress = new DashboardStatsVO.DimensionProgress();
        try {
            progress.setSystemProgress(calculateSystemProgress(projectId));
            progress.setDatasetProgress(calculateDatasetProgress(projectId));
            progress.setSecurityProgress(calculateSecurityProgress(projectId));
            progress.setAchievementProgress(calculateAchievementProgress(projectId));
        } catch (Exception e) {
            log.warn("[Dashboard] 计算四维度进度失败: {}", e.getMessage());
            progress.setSystemProgress(0);
            progress.setDatasetProgress(0);
            progress.setSecurityProgress(0);
            progress.setAchievementProgress(0);
        }
        return progress;
    }

    /**
     * 创建空的四维度进度对象
     */
    private DashboardStatsVO.DimensionProgress createEmptyDimensionProgress() {
        DashboardStatsVO.DimensionProgress progress = new DashboardStatsVO.DimensionProgress();
        progress.setSystemProgress(0);
        progress.setDatasetProgress(0);
        progress.setSecurityProgress(0);
        progress.setAchievementProgress(0);
        return progress;
    }

    /**
     * 计算系统功能建设进度
     * 优先使用指标303（系统功能覆盖率），其次使用30101/301
     */
    private Integer calculateSystemProgress(Long projectId) {
        // 方式1: 直接使用303指标值（系统功能覆盖率，已自动计算）
        DeclareIndicatorValueDO coverageIndicator = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "303");
        if (coverageIndicator != null && coverageIndicator.getValueNum() != null) {
            return coverageIndicator.getValueNum().intValue();
        }

        // 方式2: 计算30101/301（已完成系统数/计划系统数）
        DeclareIndicatorValueDO planSystem = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "301");
        DeclareIndicatorValueDO completedSystem = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "30101");
        if (planSystem != null && planSystem.getValueNum() != null
                && planSystem.getValueNum().compareTo(BigDecimal.ZERO) > 0) {
            if (completedSystem != null && completedSystem.getValueNum() != null) {
                return completedSystem.getValueNum()
                        .divide(planSystem.getValueNum(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .intValue();
            }
        }
        return 0;
    }

    /**
     * 计算高质量数据集建设进度
     * 基于502/501（已完成数据集数/计划数据集数）
     */
    private Integer calculateDatasetProgress(Long projectId) {
        DeclareIndicatorValueDO planDataset = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "501");
        DeclareIndicatorValueDO completedDataset = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "502");

        if (planDataset == null || planDataset.getValueNum() == null
                || planDataset.getValueNum().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (completedDataset != null && completedDataset.getValueNum() != null) {
            return completedDataset.getValueNum()
                    .divide(planDataset.getValueNum(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();
        }
        return 0;
    }

    /**
     * 计算信息安全备案进度
     * 优先使用804布尔指标，其次基于category=7指标完成率
     */
    private Integer calculateSecurityProgress(Long projectId) {
        // 方式1: 使用804布尔指标（数据安全分类分级完成情况）
        DeclareIndicatorValueDO securityClassification = indicatorValueMapper
                .selectByBusinessAndIndicatorCode(2, projectId, "804");
        if (securityClassification != null
                && Boolean.TRUE.equals(securityClassification.getValueBool())) {
            return 100;
        }

        // 方式2: 基于category=7指标完成率
        return calculateCategoryCompletionRate(7, projectId);
    }

    /**
     * 计算成果转化进度
     * 基于category=4指标完成率
     */
    private Integer calculateAchievementProgress(Long projectId) {
        // 成果指标关联到AchievementDO，需要查询成果记录
        List<AchievementDO> achievements = achievementService.getAchievementListByProjectId(projectId);
        if (achievements == null || achievements.isEmpty()) {
            return 0;
        }

        int totalIndicators = 0;
        int completedIndicators = 0;

        for (AchievementDO achievement : achievements) {
            List<DeclareIndicatorDO> category4Indicators = indicatorMapper.selectByCategory(4);
            List<DeclareIndicatorValueDO> indicatorValues = indicatorValueMapper
                    .selectByBusinessAndCategory(7, achievement.getId(), 4);

            totalIndicators += category4Indicators.size();
            completedIndicators += countFilledIndicators(category4Indicators, indicatorValues);
        }

        if (totalIndicators == 0) {
            return 0;
        }
        return (int) (completedIndicators * 100.0 / totalIndicators);
    }

    /**
     * 计算指定分类的指标完成率
     */
    private Integer calculateCategoryCompletionRate(Integer category, Long projectId) {
        List<DeclareIndicatorDO> indicators = indicatorMapper.selectByCategory(category);
        if (indicators == null || indicators.isEmpty()) {
            return 0;
        }

        List<String> indicatorCodes = indicators.stream()
                .map(DeclareIndicatorDO::getIndicatorCode)
                .collect(Collectors.toList());

        List<DeclareIndicatorValueDO> values = indicatorValueMapper
                .selectByIndicatorCodes(2, projectId, indicatorCodes);

        int completedCount = countFilledIndicators(indicators, values);
        return (int) (completedCount * 100.0 / indicators.size());
    }

    /**
     * 统计已填写的指标数量
     */
    private int countFilledIndicators(List<DeclareIndicatorDO> indicators,
                                     List<DeclareIndicatorValueDO> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        Map<String, DeclareIndicatorValueDO> valueMap = values.stream()
                .collect(Collectors.toMap(
                        DeclareIndicatorValueDO::getIndicatorCode, v -> v, (v1, v2) -> v1));

        int count = 0;
        for (DeclareIndicatorDO indicator : indicators) {
            DeclareIndicatorValueDO value = valueMap.get(indicator.getIndicatorCode());
            if (value != null && isValueFilled(value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断指标值是否已填写
     */
    private boolean isValueFilled(DeclareIndicatorValueDO value) {
        return (value.getValueNum() != null)
                || (value.getValueStr() != null && !value.getValueStr().isEmpty())
                || (value.getValueBool() != null)
                || (value.getValueText() != null && !value.getValueText().isEmpty());
    }
}
