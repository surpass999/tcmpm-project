package cn.gemrun.base.module.declare.service.dashboard.impl;

import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import cn.gemrun.base.module.declare.controller.admin.dashboard.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.hospital.DeclareHospitalDO;
import cn.gemrun.base.module.declare.dal.mysql.DeclareHospitalMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.dal.mysql.achievement.AchievementMapper;
import cn.gemrun.base.module.declare.enums.ProjectTypeEnum;
import cn.gemrun.base.module.declare.service.dashboard.DashboardService;
import cn.gemrun.base.module.declare.service.progress.DeclareReportWindowService;
import cn.gemrun.base.module.declare.vo.progress.ReportWindowVO;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.module.system.dal.dataobject.permission.RoleDO;
import cn.gemrun.base.module.system.enums.permission.DataScopeEnum;
import cn.gemrun.base.module.system.service.permission.PermissionService;
import cn.gemrun.base.module.system.service.permission.RoleService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 驾驶舱数据服务实现类
 *
 * @author Gemini
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private static final Set<String> HOSPITAL_ROLE_CODES = new HashSet<>();
    private static final Set<String> PROVINCE_ROLE_CODES = new HashSet<>();
    private static final Set<String> NATION_ROLE_CODES = new HashSet<>();
    private static final Set<String> EXPERT_ROLE_CODES = new HashSet<>();

    static {
        HOSPITAL_ROLE_CODES.add("HOSPITAL");
        PROVINCE_ROLE_CODES.add("PROVINCE");
        NATION_ROLE_CODES.add("NATION");
        EXPERT_ROLE_CODES.add("EXPERT");
    }

    @Resource
    @Lazy
    private PermissionService permissionService;
    @Resource
    @Lazy
    private RoleService roleService;
    @Resource
    @Lazy
    private AdminUserApi adminUserApi;
    @Resource
    private DeclareHospitalMapper hospitalMapper;
    @Resource
    private DeclareProgressReportMapper progressReportMapper;
    @Resource
    private AchievementMapper achievementMapper;
    @Resource
    @Lazy
    private DeclareReportWindowService reportWindowService;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setUserRole(determineUserRole());

        // 医院驾驶舱数据
        if ("hospital".equals(vo.getUserRole())) {
            vo.setHospitalStats(buildHospitalStats());
        }
        // 省级驾驶舱数据
        if ("province".equals(vo.getUserRole())) {
            vo.setProvinceStats(buildProvinceStats());
        }
        return vo;
    }

    /**
     * 构建医院驾驶舱专属统计数据
     */
    private DashboardStatsVO.HospitalStats buildHospitalStats() {
        DashboardStatsVO.HospitalStats stats = new DashboardStatsVO.HospitalStats();

        // 是否有开放的填报窗口
        boolean hasOpenWindow = reportWindowService.isAnyWindowOpen();
        stats.setHasUnfilledOpenWindow(hasOpenWindow);

        // 成果统计数据
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Long deptId = null;
        if (userId != null) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user != null) {
                deptId = user.getDeptId();
            }
        }
        if (deptId != null) {
            long totalCount = achievementMapper.selectCountByDeptId(deptId);
            long approvedCount = achievementMapper.selectCountApprovedByDeptId(deptId);
            stats.setAchievementTotalCount((int) totalCount);
            stats.setAchievementApprovedCount((int) approvedCount);

            // 成果转化进度：已认定推广数 / 成果总数 * 100
            DashboardStatsVO.DimensionProgress dimensionProgress = new DashboardStatsVO.DimensionProgress();
            if (totalCount > 0) {
                dimensionProgress.setAchievementProgress((int) (approvedCount * 100 / totalCount));
            } else {
                dimensionProgress.setAchievementProgress(0);
            }
            stats.setDimensionProgress(dimensionProgress);
        } else {
            stats.setAchievementTotalCount(0);
            stats.setAchievementApprovedCount(0);
        }

        return stats;
    }

    /**
     * 构建省级驾驶舱专属统计数据
     */
    private DashboardStatsVO.ProvinceStats buildProvinceStats() {
        DashboardStatsVO.ProvinceStats stats = new DashboardStatsVO.ProvinceStats();

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = userId != null ? adminUserApi.getUser(userId) : null;
        Long deptId = user != null ? user.getDeptId() : null;

        // 通过用户归属的医院确定省份
        String provinceCode = null;
        if (deptId != null) {
            DeclareHospitalDO hospital = hospitalMapper.selectByDeptId(deptId);
            if (hospital != null) {
                provinceCode = hospital.getProvinceCode();
            }
        }

        if (provinceCode == null) {
            // 兜底：取该用户可见的第一家医院的省份
            List<DeclareHospitalDO> hospitals = hospitalMapper.selectList(null);
            if (CollUtil.isNotEmpty(hospitals)) {
                provinceCode = hospitals.get(0).getProvinceCode();
            }
        }

        log.info("[Dashboard] 省级驾驶舱 provinceCode={}", provinceCode);
        stats.setProvinceCode(provinceCode);

        if (provinceCode != null) {
            // 辖区医院总数
            int regionProjectCount = hospitalMapper.selectCountActiveByProvince(provinceCode);
            stats.setRegionProjectCount(regionProjectCount);

            // 已填报医院数
            int reportedCount = progressReportMapper.selectReportedHospitalCountByProvince(provinceCode);
            stats.setReportedHospitalCount(reportedCount);

            // 待审核任务数量
            int pendingCount = progressReportMapper.selectProvincePendingCountByProvince(provinceCode);
            stats.setPendingReviewCount(pendingCount);

            // 项目类型分布（按省份过滤）
            List<DashboardStatsVO.NationalStats.ProjectTypeItem> typeItems =
                    hospitalMapper.selectCountGroupByProjectTypeOfProvince(provinceCode);
            if (CollUtil.isEmpty(typeItems)) {
                typeItems = Collections.emptyList();
            }
            // 补充缺失的类型，全部6种都展示
            List<DashboardStatsVO.NationalStats.ProjectTypeItem> fullItems = new java.util.ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                final int typeVal = i;
                DashboardStatsVO.NationalStats.ProjectTypeItem exist = typeItems.stream()
                        .filter(t -> t.getTypeValue() != null && t.getTypeValue().equals(typeVal))
                        .findFirst().orElse(null);
                DashboardStatsVO.NationalStats.ProjectTypeItem item = new DashboardStatsVO.NationalStats.ProjectTypeItem();
                item.setTypeValue(i);
                item.setProjectCount(exist != null ? exist.getProjectCount() : 0);
                item.setPercentage(exist != null ? exist.getPercentage() : 0);
                ProjectTypeEnum e = ProjectTypeEnum.valueOf(i);
                item.setTypeName(e != null ? e.getName() : "未知");
                fullItems.add(item);
            }
            stats.setProjectTypeDistribution(fullItems);

            // 平均进度
            int progress = regionProjectCount > 0 ? (reportedCount * 100 / regionProjectCount) : 0;
            stats.setRegionProgress(progress);
        }

        return stats;
    }

    /**
     * 根据用户角色确定驾驶舱类型
     * 优先级：HOSPITAL > PROVINCE > NATION > EXPERT
     */
    private String determineUserRole() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.warn("[Dashboard] 无法获取当前登录用户，返回默认 hospital");
            return "hospital";
        }

        // 1. 获取用户角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            log.warn("[Dashboard] 用户 {} 无角色，返回默认 hospital", userId);
            return "hospital";
        }

        List<RoleDO> roles = roleService.getRoleListFromCache(roleIds);
        if (CollUtil.isEmpty(roles)) {
            return "hospital";
        }

        // 2. 获取用户部门，判断是否为医院用户
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        Long deptId = user != null ? user.getDeptId() : null;
        boolean isHospitalUser = deptId != null && hospitalMapper.selectByDeptId(deptId) != null;

        // 3. 根据角色标识确定驾驶舱类型
        // 优先级：医院用户（HOSPITAL）> 省级用户（PROVINCE）> 国家局用户（NATION）> 专家用户（EXPERT）
        // 如果用户被分配了多个角色，以最高优先级的为准
        for (RoleDO role : roles) {
            String code = role.getCode();
            if (code == null) {
                continue;
            }
            if (isHospitalUser && HOSPITAL_ROLE_CODES.contains(code)) {
                return "hospital";
            }
            if (PROVINCE_ROLE_CODES.contains(code)) {
                return "province";
            }
            if (NATION_ROLE_CODES.contains(code)) {
                return "nation";
            }
            if (EXPERT_ROLE_CODES.contains(code)) {
                return "expert";
            }
        }

        // 4. 兜底：如果用户有 ALL 数据范围权限，视为国家局用户
        if (hasAllDataScope(roles)) {
            return "nation";
        }

        // 5. 兜底：如果用户是医院用户但没有分配 HOSPITAL 角色，仍然显示医院控制台
        if (isHospitalUser) {
            return "hospital";
        }

        log.warn("[Dashboard] 用户 {} 角色 {} 无法匹配任何驾驶舱类型，默认 hospital",
                userId, roles);
        return "hospital";
    }

    private boolean hasAllDataScope(List<RoleDO> roles) {
        for (RoleDO role : roles) {
            if (role.getDataScope() != null && DataScopeEnum.ALL.getScope().equals(role.getDataScope())) {
                return true;
            }
        }
        return false;
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
        NationalStatsVO vo = new NationalStatsVO();

        int totalCount = hospitalMapper.selectCountActive();
        vo.setTotalProjectCount(totalCount);

        List<DashboardStatsVO.NationalStats.ProjectTypeItem> typeItems =
                hospitalMapper.selectCountGroupByProjectType();
        if (CollUtil.isNotEmpty(typeItems)) {
            for (DashboardStatsVO.NationalStats.ProjectTypeItem item : typeItems) {
                ProjectTypeEnum e = ProjectTypeEnum.valueOf(item.getTypeValue());
                if (e != null) {
                    item.setTypeName(e.getName());
                }
            }
        }
        vo.setProjectTypeDistribution(typeItems);

        List<NationalStatsVO.ProvinceItem> provinceItems = hospitalMapper.selectCountGroupByProvince();
        vo.setProvinceDistribution(provinceItems);

        int reportedCount = progressReportMapper.selectReportedHospitalCount();
        vo.setAverageProgress(totalCount > 0 ? (reportedCount * 100 / totalCount) : 0);

        vo.setPendingApprovalCount(progressReportMapper.selectPendingReportCount());
        vo.setMidtermProjectCount(progressReportMapper.selectTotalReportCount());
        vo.setAcceptedProjectCount(progressReportMapper.selectProvincePendingCount());

        vo.setTotalFundRate(0);
        vo.setCentralFundThreeYearTotal(java.math.BigDecimal.ZERO);
        vo.setCentralFundArriveTotal(java.math.BigDecimal.ZERO);

        return vo;
    }

    @Override
    public ReportWindowStatsVO getReportWindowStats() {
        ReportWindowStatsVO vo = new ReportWindowStatsVO();
        vo.setTotalHospitalCount(hospitalMapper.selectCountActive());
        vo.setReportedHospitalCount(progressReportMapper.selectReportedHospitalCount());

        ReportWindowVO openWindow = reportWindowService.getLatestOpenWindow();
        if (openWindow != null) {
            vo.setHasOpenWindow(true);
            vo.setOpenWindowName(openWindow.getRemark());
            vo.setCurrentBatch(openWindow.getReportBatch());
            vo.setReportYear(openWindow.getReportYear());
            vo.setStartDate(openWindow.getWindowStart());
            vo.setEndDate(openWindow.getWindowEnd());
        } else {
            vo.setHasOpenWindow(false);
        }
        return vo;
    }

    @Override
    public String getCurrentUserRole() {
        return determineUserRole();
    }
}
