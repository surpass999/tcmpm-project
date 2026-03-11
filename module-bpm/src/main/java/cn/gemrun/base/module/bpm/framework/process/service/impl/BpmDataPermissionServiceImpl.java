package cn.gemrun.base.module.bpm.framework.process.service.impl;

import cn.gemrun.base.framework.web.core.util.WebFrameworkUtils;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessTypeDO;
import cn.gemrun.base.module.bpm.dal.mysql.BpmBusinessTypeMapper;
import cn.gemrun.base.module.bpm.dal.mysql.process.BpmBusinessProcessMapper;
import cn.gemrun.base.module.bpm.framework.process.annotation.BpmProcessQuery;
import cn.gemrun.base.module.bpm.framework.process.service.BpmDataPermissionService;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.permission.PermissionApi;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BPM 数据权限服务实现
 *
 * 根据流程定义中的任务分配配置，计算用户有权限看到的业务数据
 *
 * @author Gemini
 */
@Slf4j
@Service
public class BpmDataPermissionServiceImpl implements BpmDataPermissionService {

    @Resource
    private BpmBusinessTypeMapper businessTypeMapper;

    @Resource
    private BpmBusinessProcessMapper businessProcessMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private PermissionApi permissionApi;

    @Override
    public List<Long> getAuthorizedBusinessIds(String processCategory, boolean processOnly, java.lang.reflect.Method method) {
        // 1. 解析 processCategory
        if (StrUtil.isBlank(processCategory)) {
            processCategory = parseProcessCategory(method);
        }

        // 2. 查询该分类下所有 businessType
        List<BpmBusinessTypeDO> businessTypes = businessTypeMapper.selectByCategory(processCategory);
        if (CollUtil.isEmpty(businessTypes)) {
            log.warn("流程分类 [{}] 没有配置 businessType", processCategory);
            return Collections.emptyList();
        }

        // 3. 如果只查询流程数据，直接返回所有 businessId
        if (processOnly) {
            return getUserProcessBusinessIds(processCategory, businessTypes);
        }

        // 4. 查询用户在这些流程中的有权限的业务ID
        return getAuthorizedBusinessIdsByAssignConfig(processCategory, businessTypes);
    }

    @Override
    public List<Long> getAuthorizedBusinessIds(BpmProcessQuery bpmProcessQuery, java.lang.reflect.Method method) {
        return getAuthorizedBusinessIds(bpmProcessQuery.processCategory(), bpmProcessQuery.processOnly(), method);
    }

    @Override
    public List<BpmBusinessProcessDO> getUserProcesses(String processCategory) {
        // 1. 获取当前用户ID
        Long userId = WebFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        // 2. 查询该分类下所有 businessType
        List<BpmBusinessTypeDO> businessTypes = businessTypeMapper.selectByCategory(processCategory);
        if (CollUtil.isEmpty(businessTypes)) {
            return Collections.emptyList();
        }

        // 3. 查询用户参与的流程
        List<String> businessTypeList = businessTypes.stream()
                .map(BpmBusinessTypeDO::getBusinessType)
                .collect(Collectors.toList());

        return businessProcessMapper.selectList(Wrappers.<BpmBusinessProcessDO>lambdaQuery()
                .in(BpmBusinessProcessDO::getBusinessType, businessTypeList)
                .like(BpmBusinessProcessDO::getInitiatorIds, "," + userId + ",")
                .orderByDesc(BpmBusinessProcessDO::getStartTime));
    }

    @Override
    public String parseProcessCategory(java.lang.reflect.Method method) {
        if (method == null) {
            return null;
        }

        String className = method.getDeclaringClass().getName();
        String simpleClassName = method.getDeclaringClass().getSimpleName();

        // 规则1: 从类名推断
        // DeclareProjectController -> project
        String businessName = simpleClassName;
        if (businessName.endsWith("Controller")) {
            businessName = businessName.substring(0, businessName.length() - 10); // 去掉 "Controller"
        }

        // 规则2: 从包名推断模块名
        // cn.gemrun.base.module.declare.controller.admin.project -> declare
        String moduleName = "";
        String[] parts = className.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if ("module".equals(parts[i]) && i + 1 < parts.length) {
                moduleName = parts[i + 1];
                break;
            }
        }

        // 组合: 模块名 + 业务名
        // 例如: declare + project = declare-project
        // 或者: project (如果没有模块名)
        if (StrUtil.isNotBlank(moduleName)) {
            return moduleName + "-" + businessName.toLowerCase();
        }
        return businessName.toLowerCase();
    }

    /**
     * 根据任务分配配置，查询用户有权限的业务ID
     */
    private List<Long> getAuthorizedBusinessIdsByAssignConfig(String processCategory, List<BpmBusinessTypeDO> businessTypes) {
        // 获取当前用户
        Long userId = WebFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        // 收集所有有权限的业务ID
        Set<Long> authorizedBusinessIds = new HashSet<>();

        List<String> businessTypeList = businessTypes.stream()
                .map(BpmBusinessTypeDO::getBusinessType)
                .collect(Collectors.toList());

        // 1. 查询该用户是发起人或者参与人的业务（使用 FIND_IN_SET 精确匹配）
        List<BpmBusinessProcessDO> initiatorProcesses = businessProcessMapper.selectList(
                Wrappers.<BpmBusinessProcessDO>lambdaQuery()
                        .in(BpmBusinessProcessDO::getBusinessType, businessTypeList)
                        .and(w -> w.eq(BpmBusinessProcessDO::getInitiatorId, userId)
                                .or()
                                .apply("FIND_IN_SET(" + userId + ", initiator_ids) > 0"))
        );
        initiatorProcesses.forEach(p -> authorizedBusinessIds.add(p.getBusinessId()));

        // 2. 根据任务分配配置查询用户是审批人的业务
        List<BpmBusinessProcessDO> allProcesses = businessProcessMapper.selectList(
                Wrappers.<BpmBusinessProcessDO>lambdaQuery()
                        .in(BpmBusinessProcessDO::getBusinessType, businessTypeList)
                        .isNotNull(BpmBusinessProcessDO::getCurrentAssignType)
        );

        for (BpmBusinessProcessDO process : allProcesses) {
            if (hasPermissionByAssignConfig(process.getCurrentAssignType(), process.getCurrentAssignSource(), userId)) {
                authorizedBusinessIds.add(process.getBusinessId());
            }
        }

        return new ArrayList<>(authorizedBusinessIds);
    }

    /**
     * 查询用户参与的流程业务ID（场景2：我的待办/已办）
     */
    private List<Long> getUserProcessBusinessIds(String processCategory, List<BpmBusinessTypeDO> businessTypes) {
        Long userId = WebFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        // 查询用户是发起人或参与人的流程
        List<BpmBusinessProcessDO> processes = businessProcessMapper.selectList(
                Wrappers.<BpmBusinessProcessDO>lambdaQuery()
                        .in(BpmBusinessProcessDO::getBusinessType,
                                businessTypes.stream().map(BpmBusinessTypeDO::getBusinessType).collect(Collectors.toList()))
                        .like(BpmBusinessProcessDO::getInitiatorIds, "," + userId + ",")
        );

        return processes.stream()
                .map(BpmBusinessProcessDO::getBusinessId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 根据任务分配策略，计算用户是否有权限
     *
     * @param assignType   分配类型（对应 BpmTaskCandidateStrategyEnum）
     * @param assignSource 分配来源（岗位ID/用户ID/角色编码等）
     * @param userId      当前用户ID
     * @return 是否有权限
     */
    private boolean hasPermissionByAssignConfig(String assignType, String assignSource, Long userId) {
        if (assignType == null) {
            return false;
        }

        // 根据 BpmTaskCandidateStrategyEnum 判断
        switch (assignType) {
            case "USER":  // 指定用户
                return hasPermissionForUser(assignSource, userId);

            case "DEPT_POST":  // 本部门岗位
                return hasPermissionForDeptPost(assignSource, userId);

            case "POST":  // 岗位
                return hasPermissionForPost(assignSource, userId);

            case "ROLE":  // 角色
                return hasPermissionForRole(assignSource, userId);

            case "DEPT_MEMBER":  // 部门成员
            case "DEPT_LEADER":  // 部门负责人
                return hasPermissionForDept(assignSource, userId);

            case "START_USER":  // 发起人本人
                // 发起人权限在 initiatorProcesses 中已处理
                return false;

            case "START_USER_SELECT":  // 发起人自选
                return hasPermissionForUser(assignSource, userId);

            case "START_USER_DEPT_LEADER":  // 发起人部门负责人
                return hasPermissionForStartUserDeptLeader(assignSource, userId);

            case "USER_GROUP":  // 用户组
                return hasPermissionForUserGroup(assignSource, userId);

            default:
                log.debug("未处理的任务分配类型: {}", assignType);
                return false;
        }
    }

    /**
     * 判断用户是否是指定用户
     */
    private boolean hasPermissionForUser(String assignSource, Long userId) {
        if (StrUtil.isBlank(assignSource)) {
            return false;
        }
        // source 格式: "1,2,3" 或单个 "1"
        String[] userIds = assignSource.split(",");
        for (String uid : userIds) {
            if (String.valueOf(userId).equals(uid.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户是否属于本部门岗位
     */
    private boolean hasPermissionForDeptPost(String assignSource, Long userId) {
        if (StrUtil.isBlank(assignSource)) {
            return false;
        }
        try {
            Long postId = Long.parseLong(assignSource.trim());
            List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(Collections.singletonList(postId));
            return users.stream().anyMatch(u -> Objects.equals(u.getId(), userId));
        } catch (NumberFormatException e) {
            log.warn("解析岗位ID失败: {}", assignSource);
            return false;
        }
    }

    /**
     * 判断用户是否属于指定岗位
     */
    private boolean hasPermissionForPost(String assignSource, Long userId) {
        return hasPermissionForDeptPost(assignSource, userId);
    }

    /**
     * 判断用户是否拥有指定角色
     */
    private boolean hasPermissionForRole(String assignSource, Long userId) {
        if (StrUtil.isBlank(assignSource)) {
            return false;
        }
        // TODO: 需要通过角色编码获取角色ID，再查询用户
        // 暂时无法实现，需要 RoleApi
        log.warn("角色权限判断需要 RoleApi 支持: {}", assignSource);
        return false;
    }

    /**
     * 判断用户是否是部门成员/负责人
     */
    private boolean hasPermissionForDept(String assignSource, Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            return false;
        }
        // 直接返回用户有部门ID，即属于该部门
        return true;
    }

    /**
     * 判断用户是否是发起人的部门负责人
     */
    private boolean hasPermissionForStartUserDeptLeader(String assignSource, Long userId) {
        // 这个需要根据业务数据中的发起人部门来判断
        // 简化处理：暂时返回 false
        return false;
    }

    /**
     * 判断用户是否属于用户组
     */
    private boolean hasPermissionForUserGroup(String assignSource, Long userId) {
        // TODO: 实现用户组判断逻辑
        return false;
    }

}
