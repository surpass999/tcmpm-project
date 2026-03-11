package cn.gemrun.base.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.dept.PostApi;
import cn.gemrun.base.module.system.api.dept.dto.DeptRespDTO;
import cn.gemrun.base.module.system.api.dept.dto.PostRespDTO;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本部门岗位 {@link BpmTaskCandidateStrategy} 实现类
 * <p>
 * 基于"上一节点提交人"查找：从上一节点处理人的部门向上查找1级部门，
 * 然后在目标部门下查找有指定岗位的用户
 *
 * @author Gemini
 */
@Component
@Slf4j
public class BpmTaskCandidateDeptPostStrategy implements BpmTaskCandidateStrategy {

    @Resource
    private DeptApi deptApi;
    @Resource
    private PostApi postApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    @Lazy  // 延迟加载，避免循环依赖
    private HistoryService historyService;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.DEPT_POST;
    }

    @Override
    public void validateParam(String param) {
        // 参数为岗位ID，如 "2"
        if (param == null || param.trim().isEmpty()) {
            throw new IllegalArgumentException("参数不能为空，应为岗位ID");
        }

        try {
            Long postId = Long.parseLong(param);
            PostRespDTO post = postApi.getPost(postId);
            if (post == null) {
                throw new IllegalArgumentException("岗位不存在: " + postId);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("岗位ID必须为数字: " + param);
        }
    }

    @Override
    public boolean isParamRequired() {
        return true;
    }

    @Override
    public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        // param 就是岗位ID（如 "2"）
        Long postId;
        try {
            postId = Long.parseLong(param);
        } catch (NumberFormatException e) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 岗位ID不是数字: {}", param);
            return Collections.emptySet();
        }

        // 1. 获取流程实例ID
        String processInstanceId = execution.getProcessInstanceId();
        if (processInstanceId == null) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到流程实例ID");
            return Collections.emptySet();
        }

        // 2. 从流程变量中获取 level 参数（默认1）
        Integer level = 1;
        Map<String, Object> processVariables = execution.getVariables();
        log.info("[BpmTaskCandidateDeptPostStrategy] 当前流程变量 keys: {}", processVariables.keySet());
        if (processVariables.containsKey("PROCESS_DEPT_POST_LEVEL")) {
            Object levelObj = processVariables.get("PROCESS_DEPT_POST_LEVEL");
            if (levelObj instanceof Number) {
                level = ((Number) levelObj).intValue();
            }
            log.info("[BpmTaskCandidateDeptPostStrategy] 从流程变量读取到 level: {}", level);
        } else {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到 PROCESS_DEPT_POST_LEVEL 变量，使用默认 level=1");
        }

        // 3. 始终基于发起人计算，不管上一节点是谁
        // 这样可以确保省级用户提交后，专家审批后，仍然由省级用户审批
        log.info("[BpmTaskCandidateDeptPostStrategy] 基于发起人计算部门层级, level={}", level);
        return calculateUsersByStartUser(execution, postId, level);
    }

    /**
     * 基于发起人查找（用于第一个用户任务，没有上一节点的情况）
     */
    private Set<Long> calculateUsersByStartUser(DelegateExecution execution, Long postId, Integer level) {
        if (level == null) {
            level = 1;
        }
        Long startUserId = getStartUserId(execution);
        if (startUserId == null) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到发起人ID");
            return Collections.emptySet();
        }

        AdminUserRespDTO startUser = adminUserApi.getUser(startUserId);
        if (startUser == null || startUser.getDeptId() == null) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到发起人部门信息, userId={}", startUserId);
            return Collections.emptySet();
        }

        Long startDeptId = startUser.getDeptId();

        // 向上查找 level 级部门
        Long targetDeptId = findParentDeptByLevel(startDeptId, level);
        if (targetDeptId == null) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到{}级上级部门, startDeptId={}", level, startDeptId);
            return Collections.emptySet();
        }

        log.info("[BpmTaskCandidateDeptPostStrategy] 基于发起人: 发起人部门={}, {}级上级部门={}, 岗位ID={}",
                startDeptId, level, targetDeptId, postId);

        PostRespDTO post = postApi.getPost(postId);
        if (post == null) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 岗位不存在, postId={}", postId);
            return Collections.emptySet();
        }

        // 查询目标部门下的所有用户
        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(Collections.singletonList(targetDeptId));
        log.info("[BpmTaskCandidateDeptPostStrategy] 目标部门 {} 下的用户数量: {}, 用户IDs: {}",
                targetDeptId, users.size(), users.stream().map(AdminUserRespDTO::getId).collect(Collectors.toList()));

        // 过滤出拥有指定岗位的用户
        Set<Long> result = users.stream()
                .filter(user -> {
                    if (user.getPostIds() == null || user.getPostIds().isEmpty()) {
                        log.info("[BpmTaskCandidateDeptPostStrategy] 用户 {} 无岗位信息, postIds={}", user.getId(), user.getPostIds());
                        return false;
                    }
                    boolean hasPost = user.getPostIds().contains(post.getId());
                    log.info("[BpmTaskCandidateDeptPostStrategy] 用户 {} 的岗位 {} 是否包含岗位 {}: {}",
                            user.getId(), user.getPostIds(), post.getId(), hasPost);
                    return hasPost;
                })
                .map(AdminUserRespDTO::getId)
                .collect(Collectors.toSet());

        log.info("[BpmTaskCandidateDeptPostStrategy] 最终候选人数量: {}, 用户IDs: {}", result.size(), result);
        return result;
    }

    /**
     * 获取上一节点的处理人ID
     *
     * @param processInstanceId 流程实例ID
     * @return 上一节点处理人ID，如果没有则返回null
     */
    private Long getPreviousTaskAssignee(String processInstanceId) {
        // 查询已完成的按时间倒序排列的任务，取最近的一个
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 1);

        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        HistoricTaskInstance lastTask = tasks.get(0);
        String assignee = lastTask.getAssignee();
        if (assignee == null || assignee.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(assignee);
        } catch (NumberFormatException e) {
            log.warn("[BpmTaskCandidateDeptPostStrategy] 任务处理人ID不是数字: {}", assignee);
            return null;
        }
    }

    /**
     * 通过 parent_id 递归向上查找第 N 级部门
     *
     * @param startDeptId 起始部门ID
     * @param level       目标层级（第 N 级上级）
     * @return 目标部门ID，如果未找到返回 null
     */
    private Long findParentDeptByLevel(Long startDeptId, Integer level) {
        Long currentId = startDeptId;
        int currentLevel = 0;

        while (currentId != null && currentId != 0L) {
            // 获取部门信息
            DeptRespDTO dept = deptApi.getDept(currentId);
            if (dept == null) {
                log.warn("[BpmTaskCandidateDeptPostStrategy] 部门不存在, deptId={}", currentId);
                break;
            }

            // 判断是否到达目标层级
            if (currentLevel == level) {
                log.info("[BpmTaskCandidateDeptPostStrategy] 找到第{}级部门, deptId={}, deptName={}",
                        level, dept.getId(), dept.getName());
                return dept.getId();
            }

            // 继续向上查找
            currentId = dept.getParentId();
            currentLevel++;
        }

        log.warn("[BpmTaskCandidateDeptPostStrategy] 未找到第{}级上级部门, startDeptId={}", level, startDeptId);
        return null;
    }

    /**
     * 获取发起人ID
     */
    private Long getStartUserId(DelegateExecution execution) {
        // 先尝试 initiator（Flowable标准变量）
        Object startUserIdObj = execution.getVariable("initiator");
        if (startUserIdObj == null) {
            // 再尝试 startUserId
            startUserIdObj = execution.getVariable("startUserId");
        }
        if (startUserIdObj == null) {
            return null;
        }

        if (startUserIdObj instanceof Long) {
            return (Long) startUserIdObj;
        } else if (startUserIdObj instanceof Integer) {
            return ((Integer) startUserIdObj).longValue();
        } else if (startUserIdObj instanceof String) {
            try {
                return Long.parseLong((String) startUserIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
