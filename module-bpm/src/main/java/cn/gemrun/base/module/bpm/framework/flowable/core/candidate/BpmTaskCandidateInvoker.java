package cn.gemrun.base.module.bpm.framework.flowable.core.candidate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.gemrun.base.framework.common.enums.CommonStatusEnum;
import cn.gemrun.base.framework.common.util.object.ObjectUtils;
import cn.gemrun.base.framework.datapermission.core.annotation.DataPermission;
import cn.gemrun.base.module.bpm.enums.definition.BpmUserTaskApproveTypeEnum;
import cn.gemrun.base.module.bpm.enums.definition.BpmUserTaskAssignStartUserHandlerTypeEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.system.api.permission.PermissionApi;
import cn.gemrun.base.module.system.api.permission.RoleApi;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.*;

import static cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.gemrun.base.module.bpm.enums.ErrorCodeConstants.MODEL_DEPLOY_FAIL_TASK_CANDIDATE_NOT_CONFIG;

/**
 * {@link BpmTaskCandidateStrategy} 的调用者，用于调用对应的策略，实现任务的候选人的计算
 *
 * @author 芋道源码
 */
@Slf4j
public class BpmTaskCandidateInvoker {

    private final Map<BpmTaskCandidateStrategyEnum, BpmTaskCandidateStrategy> strategyMap = new HashMap<>();

    private final AdminUserApi adminUserApi;
    private final RoleApi roleApi;
    private final PermissionApi permissionApi;

    public BpmTaskCandidateInvoker(List<BpmTaskCandidateStrategy> strategyList,
                                   AdminUserApi adminUserApi,
                                   RoleApi roleApi,
                                   PermissionApi permissionApi) {
        strategyList.forEach(strategy -> {
            BpmTaskCandidateStrategy oldStrategy = strategyMap.put(strategy.getStrategy(), strategy);
            Assert.isNull(oldStrategy, "策略(%s) 重复", strategy.getStrategy());
        });
        this.adminUserApi = adminUserApi;
        this.roleApi = roleApi;
        this.permissionApi = permissionApi;
    }

    /**
     * 校验流程模型的任务分配规则全部都配置了
     * 目的：如果有规则未配置，会导致流程任务找不到负责人，进而流程无法进行下去！
     *
     * @param bpmnBytes BPMN XML
     */
    public void validateBpmnConfig(byte[] bpmnBytes) {
        BpmnModel bpmnModel = BpmnModelUtils.getBpmnModel(bpmnBytes);
        assert bpmnModel != null;
        List<UserTask> userTaskList = BpmnModelUtils.getBpmnModelElements(bpmnModel, UserTask.class);
        // 遍历所有的 UserTask，校验审批人配置
        userTaskList.forEach(userTask -> {
            // 1.1 非人工审批，无需校验审批人配置
            Integer approveType = BpmnModelUtils.parseApproveType(userTask);
            if (ObjectUtils.equalsAny(approveType,
                    BpmUserTaskApproveTypeEnum.AUTO_APPROVE.getType(),
                    BpmUserTaskApproveTypeEnum.AUTO_REJECT.getType())) {
                return;
            }

            // 1.2 非空校验
            Integer strategy = BpmnModelUtils.parseCandidateStrategy(userTask);
            String param = BpmnModelUtils.parseCandidateParam(userTask);

            // 如果 BPMN 没有配置审批人策略，检查 DSL 是否配置了 assign
            boolean hasDslAssign = BpmnModelUtils.hasDslAssign(userTask);

            if (strategy == null && !hasDslAssign) {
                throw exception(MODEL_DEPLOY_FAIL_TASK_CANDIDATE_NOT_CONFIG, userTask.getName());
            }
            // 如果策略存在且需要参数，但参数为空
            if (strategy != null) {
                BpmTaskCandidateStrategy candidateStrategy = getCandidateStrategy(strategy);
                // 如果 DSL 有 assign 配置，则跳过参数校验（因为 DSL assign 会覆盖 BPMN 策略）
                if (candidateStrategy.isParamRequired() && StrUtil.isBlank(param) && !hasDslAssign) {
                    throw exception(MODEL_DEPLOY_FAIL_TASK_CANDIDATE_NOT_CONFIG, userTask.getName());
                }
                // DSL 有 assign 时，不需要校验 BPMN 策略的参数
                if (!hasDslAssign) {
                    // 2. 具体策略校验（仅当 DSL 没有 assign 配置时）
                    getCandidateStrategy(strategy).validateParam(param);
                }
            }
        });
    }

    /**
     * 计算任务的候选人
     *
     * @param execution 执行任务
     * @return 用户编号集合
     */
    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致找不到候选人
    public Set<Long> calculateUsersByTask(DelegateExecution execution) {
        // 注意：解决极端情况下，Flowable 异步调用，导致租户 id 丢失的情况
        // 例如说，SIMPLE 延迟器在 trigger 的时候！！！
        return FlowableUtils.execute(execution.getTenantId(), () -> {
            // 审批类型非人工审核时，不进行计算候选人。原因是：后续会自动通过、不通过
            FlowElement flowElement = execution.getCurrentFlowElement();
            Integer approveType = BpmnModelUtils.parseApproveType(flowElement);
            if (ObjectUtils.equalsAny(approveType,
                    BpmUserTaskApproveTypeEnum.AUTO_APPROVE.getType(),
                    BpmUserTaskApproveTypeEnum.AUTO_REJECT.getType())) {
                return new HashSet<>();
            }

            // 1.1 计算任务的候选人
            Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
            String param = BpmnModelUtils.parseCandidateParam(flowElement);
            Set<Long> userIds;
            if (strategy != null) {
                userIds = getCandidateStrategy(strategy).calculateUsersByTask(execution, param);
            } else {
                // 传统策略未配置时，尝试 DSL assign
                String dslConfig = BpmnModelUtils.parseDslConfig(flowElement);
                userIds = calculateUsersByDslAssign(execution, dslConfig);
            }
            // 1.2 移除被禁用的用户
            removeDisableUsers(userIds);

            // 2. 候选人为空时，根据“审批人为空”的配置补充
            if (CollUtil.isEmpty(userIds)) {
                userIds = getCandidateStrategy(BpmTaskCandidateStrategyEnum.ASSIGN_EMPTY.getStrategy())
                        .calculateUsersByTask(execution, param);
                // ASSIGN_EMPTY 策略，不需要移除被禁用的用户。原因是，再移除，可能会出现更没审批人了！！！
            }

            // 3. 移除发起人的用户
            ProcessInstance processInstance = SpringUtil.getBean(BpmProcessInstanceService.class)
                    .getProcessInstance(execution.getProcessInstanceId());
            Assert.notNull(processInstance, "流程实例({}) 不存在", execution.getProcessInstanceId());
            removeStartUserIfSkip(userIds, flowElement, Long.valueOf(processInstance.getStartUserId()));
            return userIds;
        });
    }

    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致找不到候选人
    public Set<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId,
                                              Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        // 如果是 CallActivity 子流程，不进行计算候选人
        FlowElement flowElement = BpmnModelUtils.getFlowElementById(bpmnModel, activityId);
        if (flowElement instanceof CallActivity || flowElement instanceof SubProcess) {
            return new HashSet<>();
        }
        // 审批类型非人工审核时，不进行计算候选人。原因是：后续会自动通过、不通过
        Integer approveType = BpmnModelUtils.parseApproveType(flowElement);
        if (ObjectUtils.equalsAny(approveType,
                BpmUserTaskApproveTypeEnum.AUTO_APPROVE.getType(),
                BpmUserTaskApproveTypeEnum.AUTO_REJECT.getType())) {
            return new HashSet<>();
        }

        // 1.1 计算任务的候选人
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
        String param = BpmnModelUtils.parseCandidateParam(flowElement);
        Set<Long> userIds;
        if (strategy != null) {
            userIds = getCandidateStrategy(strategy).calculateUsersByActivity(bpmnModel, activityId, param,
                    startUserId, processDefinitionId, processVariables);
        } else {
            userIds = new HashSet<>();
        }
        // 1.2 移除被禁用的用户
        removeDisableUsers(userIds);

        // 2. 候选人为空时，根据“审批人为空”的配置补充
        if (CollUtil.isEmpty(userIds)) {
            userIds = getCandidateStrategy(BpmTaskCandidateStrategyEnum.ASSIGN_EMPTY.getStrategy())
                    .calculateUsersByActivity(bpmnModel, activityId, param, startUserId, processDefinitionId, processVariables);
            // ASSIGN_EMPTY 策略，不需要移除被禁用的用户。原因是，再移除，可能会出现更没审批人了！！！
        }

        // 3. 移除发起人的用户
        removeStartUserIfSkip(userIds, flowElement, startUserId);
        return userIds;
    }

    @VisibleForTesting
    void removeDisableUsers(Set<Long> assigneeUserIds) {
        if (CollUtil.isEmpty(assigneeUserIds)) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(assigneeUserIds);
        assigneeUserIds.removeIf(id -> {
            AdminUserRespDTO user = userMap.get(id);
            return user == null || CommonStatusEnum.isDisable(user.getStatus());
        });
    }

    /**
     * 如果“审批人与发起人相同时”，配置了 SKIP 跳过，则移除发起人
     *
     * 注意：如果只有一个候选人，则不处理，避免无法审批
     *
     * @param assigneeUserIds 当前分配的候选人
     * @param flowElement 当前节点
     * @param startUserId 发起人
     */
    @VisibleForTesting
    void removeStartUserIfSkip(Set<Long> assigneeUserIds, FlowElement flowElement, Long startUserId) {
        if (CollUtil.size(assigneeUserIds) <= 1) {
            return;
        }
        Integer assignStartUserHandlerType = BpmnModelUtils.parseAssignStartUserHandlerType(flowElement);
        if (ObjectUtil.notEqual(assignStartUserHandlerType, BpmUserTaskAssignStartUserHandlerTypeEnum.SKIP.getType())) {
            return;
        }
        assigneeUserIds.remove(startUserId);
    }

    private BpmTaskCandidateStrategy getCandidateStrategy(Integer strategy) {
        BpmTaskCandidateStrategyEnum strategyEnum = BpmTaskCandidateStrategyEnum.valueOf(strategy);
        Assert.notNull(strategyEnum, "策略({}) 不存在", strategy);
        BpmTaskCandidateStrategy strategyObj = strategyMap.get(strategyEnum);
        Assert.notNull(strategyObj, "策略({}) 不存在", strategy);
        return strategyObj;
    }

    /**
     * 根据 DSL assign 配置计算候选人（当 BPMN 未配置传统候选策略时使用）
     *
     * DSL assign type 说明：
     * - START_USER：发起人自己
     * - STATIC_ROLE：固定角色，通过 DSL 顶层 roles 字段获取角色编码列表
     */
    private Set<Long> calculateUsersByDslAssign(DelegateExecution execution, String dslConfig) {
        if (StrUtil.isBlank(dslConfig)) {
            log.warn("[calculateUsersByDslAssign] DSL配置为空，无法计算候选人");
            return new HashSet<>();
        }
        try {
            JSONObject dslJson = JSONUtil.parseObj(dslConfig);
            JSONObject assignJson = dslJson.getJSONObject("assign");
            if (assignJson == null) {
                log.warn("[calculateUsersByDslAssign] DSL中无assign配置: {}", dslConfig);
                return new HashSet<>();
            }
            String type = assignJson.getStr("type");
            log.info("[calculateUsersByDslAssign] DSL assign type={}", type);

            if ("START_USER".equals(type)) {
                BpmTaskCandidateStrategy strategy = strategyMap.get(BpmTaskCandidateStrategyEnum.START_USER);
                if (strategy != null) {
                    return strategy.calculateUsersByTask(execution, null);
                }
            } else if ("STATIC_ROLE".equals(type)) {
                // 从 DSL 顶层 roles 字段获取角色编码（如 ["PROVINCE", "NATION"]）
                JSONArray rolesArray = dslJson.getJSONArray("roles");
                if (rolesArray != null && !rolesArray.isEmpty()) {
                    Set<String> roleCodes = new HashSet<>(rolesArray.toList(String.class));
                    Set<Long> roleIds = roleApi.getRoleIdsByCodes(roleCodes);
                    log.info("[calculateUsersByDslAssign] STATIC_ROLE roleCodes={}, roleIds={}", roleCodes, roleIds);
                    if (!roleIds.isEmpty()) {
                        return permissionApi.getUserRoleIdListByRoleIds(roleIds);
                    }
                }
            } else if ("DEPT_POST".equals(type)) {
                // DEPT_POST: 本部门岗位
                // source 是岗位ID（如 "2"），level 是部门层级（默认1）
                String source = assignJson.getStr("source");
                Integer level = assignJson.getInt("level", 1);
                log.info("[calculateUsersByDslAssign] DEPT_POST source={}, level={}", source, level);

                if (StrUtil.isNotBlank(source)) {
                    BpmTaskCandidateStrategy strategy = strategyMap.get(BpmTaskCandidateStrategyEnum.DEPT_POST);
                    if (strategy != null) {
                        // source 作为参数传递
                        return strategy.calculateUsersByTask(execution, source);
                    }
                }
            } else {
                log.warn("[calculateUsersByDslAssign] 未支持的DSL assign type={}", type);
            }
        } catch (Exception e) {
            log.warn("[calculateUsersByDslAssign] DSL解析失败: {}", e.getMessage());
        }
        return new HashSet<>();
    }

}