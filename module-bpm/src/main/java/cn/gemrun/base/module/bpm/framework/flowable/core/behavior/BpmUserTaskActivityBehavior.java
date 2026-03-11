package cn.gemrun.base.module.bpm.framework.flowable.core.behavior;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.gemrun.base.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.TaskHelper;
import org.flowable.engine.interceptor.CreateUserTaskBeforeContext;
import org.flowable.task.service.TaskService;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义的【单个】流程任务的 assignee 负责人的分配
 * 第一步，基于分配规则，计算出分配任务的【单个】候选人。如果找不到，则直接报业务异常，不继续执行后续的流程；
 * 第二步，根据 approvalMode 决定分配方式：
 * - 抢签模式（approvalMode=0）：设置 candidateUsers，所有候选人都能看到任务，谁先签收谁审批
 * - 指定模式（approvalMode=1）：随机选择一个候选人作为 assignee
 *
 * @author 芋道源码
 */
@Slf4j
public class BpmUserTaskActivityBehavior extends UserTaskActivityBehavior {

    /**
     * 审批模式：抢签模式（默认）
     * 所有候选人都能看到任务，谁先签收谁审批
     */
    public static final int APPROVAL_MODE_GRAB = 0;

    /**
     * 审批模式：指定模式
     * 随机选择一个候选人作为处理人
     */
    public static final int APPROVAL_MODE_ASSIGN = 1;

    @Setter
    private BpmTaskCandidateInvoker taskCandidateInvoker;

    public BpmUserTaskActivityBehavior(UserTask userTask) {
        super(userTask);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected void handleAssignments(TaskService taskService, String assignee, String owner,
        List<String> candidateUsers, List<String> candidateGroups, TaskEntity task, ExpressionManager expressionManager,
        DelegateExecution execution, ProcessEngineConfigurationImpl processEngineConfiguration) {
        // 获取审批模式
        int approvalMode = getApprovalMode(execution);

        // 计算所有候选人
        Set<Long> candidateUserIds = calculateAllCandidateUsers(execution);
        if (CollUtil.isEmpty(candidateUserIds)) {
            return;
        }

        // 检查是否需要设置处理人（assign.type=USER）
        boolean needSetAssignee = isNeedSetAssignee(execution);

        if (approvalMode == APPROVAL_MODE_GRAB) {
            // 抢签模式：设置 candidateUsers
            for (Long userId : candidateUserIds) {
                task.addCandidateUser(String.valueOf(userId));
            }
            log.info("[handleAssignments][抢签模式] 任务 {} 设置候选人: {}",
                    task.getId(), candidateUserIds);

            // 如果需要设置处理人（assign.type=USER），则从流程变量中获取指定的用户作为处理人
            if (needSetAssignee) {
                Long assigneeUserId = getAssigneeUserFromProcessVariable(execution);
                if (assigneeUserId != null && candidateUserIds.contains(assigneeUserId)) {
                    TaskHelper.changeTaskAssignee(task, String.valueOf(assigneeUserId));
                    // 同时设置任务变量 assigneeUser，包含昵称信息，供前端展示
                    setTaskAssigneeUser(task, assigneeUserId);
                    log.info("[handleAssignments][抢签模式+assign.type=USER] 任务 {} 从流程变量设置处理人: {}", task.getId(), assigneeUserId);
                } else {
                    // 如果流程变量中没有指定用户，随机选择一个
                    assigneeUserId = selectAssigneeFromCandidates(candidateUserIds);
                    if (assigneeUserId != null) {
                        TaskHelper.changeTaskAssignee(task, String.valueOf(assigneeUserId));
                        // 同时设置任务变量 assigneeUser
                        setTaskAssigneeUser(task, assigneeUserId);
                        log.info("[handleAssignments][抢签模式+assign.type=USER] 任务 {} 随机设置处理人: {}", task.getId(), assigneeUserId);
                    }
                }
            }
        } else {
            // 指定模式：随机选择一个作为 assignee
            Long assigneeUserId = selectAssigneeFromCandidates(candidateUserIds);
            if (assigneeUserId != null) {
                TaskHelper.changeTaskAssignee(task, String.valueOf(assigneeUserId));
                // 同时设置任务变量 assigneeUser
                setTaskAssigneeUser(task, assigneeUserId);
                log.info("[handleAssignments][指定模式] 任务 {} 设置处理人: {}", task.getId(), assigneeUserId);
            }
        }
    }

    /**
     * 设置任务的 assigneeUser 变量，包含用户昵称信息，供前端展示
     */
    private void setTaskAssigneeUser(TaskEntity task, Long userId) {
        try {
            AdminUserApi adminUserApi = SpringUtil.getBean(AdminUserApi.class);
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            if (user == null) {
                log.warn("[setTaskAssigneeUser] 用户不存在: userId={}", userId);
                return;
            }
            // 构建用户信息 JSON
            JSONObject userInfo = new JSONObject();
            userInfo.set("id", user.getId());
            userInfo.set("nickname", user.getNickname());
            userInfo.set("avatar", user.getAvatar());
            userInfo.set("deptId", user.getDeptId());

            // 存储到任务本地变量
            task.setVariableLocal("assigneeUser", userInfo.toString());
            log.info("[setTaskAssigneeUser] 设置任务处理人信息成功: taskId={}, userId={}, nickname={}",
                    task.getId(), userId, user.getNickname());
        } catch (Exception e) {
            log.error("[setTaskAssigneeUser] 设置任务处理人信息失败: taskId={}, userId={}, error={}",
                    task.getId(), userId, e.getMessage(), e);
        }
    }

    /**
     * 从流程变量中获取指定的处理人用户ID
     * 当 assign.type=USER 时，上一个节点选择的专家用户存储在流程变量中
     */
    private Long getAssigneeUserFromProcessVariable(DelegateExecution execution) {
        try {
            Map<String, Object> processVariables = execution.getVariables();
            if (processVariables.containsKey("PROCESS_APPROVE_USER_SELECT_ASSIGNEES")) {
                Object approveUserSelectAssignees = processVariables.get("PROCESS_APPROVE_USER_SELECT_ASSIGNEES");
                if (approveUserSelectAssignees instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<Long>> expertMap = (Map<String, List<Long>>) approveUserSelectAssignees;
                    // 获取当前节点对应的专家用户ID
                    String currentNodeKey = execution.getCurrentFlowElement() != null
                            ? execution.getCurrentFlowElement().getId()
                            : null;
                    List<Long> expertIds = currentNodeKey != null ? expertMap.get(currentNodeKey) : null;
                    if (expertIds != null && !expertIds.isEmpty()) {
                        // 返回第一个专家用户
                        log.info("[getAssigneeUserFromProcessVariable] 从流程变量获取处理人: nodeKey={}, expertId={}",
                                currentNodeKey, expertIds.get(0));
                        return expertIds.get(0);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[getAssigneeUserFromProcessVariable] 获取处理人失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否需要设置处理人
     * 当 assign.type=USER 时，表示需要设置处理人
     */
    private boolean isNeedSetAssignee(DelegateExecution execution) {
        try {
            FlowElement flowElement = execution.getCurrentFlowElement();
            if (flowElement == null) {
                return false;
            }

            // 优先从已部署流程定义获取完整 DSL
            String dslConfig = null;
            try {
                String processDefinitionId = execution.getProcessDefinitionId();
                cn.gemrun.base.module.bpm.service.definition.BpmModelService modelService =
                        SpringUtil.getBean(cn.gemrun.base.module.bpm.service.definition.BpmModelService.class);
                String bpmnXml = modelService.getBpmnXmlByDefinitionId(processDefinitionId);
                if (StrUtil.isNotBlank(bpmnXml)) {
                    dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, flowElement.getId());
                }
            } catch (Exception e) {
                log.warn("[isNeedSetAssignee] 从流程定义获取 DSL 失败: {}", e.getMessage());
            }

            // 兜底：从 flowElement 扩展属性获取
            if (StrUtil.isBlank(dslConfig)) {
                dslConfig = BpmnModelUtils.parseDslConfig(flowElement);
            }

            if (StrUtil.isNotBlank(dslConfig)) {
                // 确保是完整的 JSON 格式
                if (!dslConfig.trim().startsWith("{")) {
                    dslConfig = "{" + dslConfig;
                }
                JSONObject dslJson = JSONUtil.parseObj(dslConfig);
                JSONObject assignJson = dslJson.getJSONObject("assign");
                if (assignJson != null) {
                    String assignType = assignJson.getStr("type");
                    if ("USER".equals(assignType)) {
                        log.info("[isNeedSetAssignee] 检测到 assign.type=USER: nodeKey={}", flowElement.getId());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[isNeedSetAssignee] 解析 assign.type 失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取审批模式
     * 从 DSL 配置中读取 approvalMode，如果没有配置则默认使用抢签模式
     */
    private int getApprovalMode(DelegateExecution execution) {
        try {
            FlowElement flowElement = execution.getCurrentFlowElement();
            String dslConfig = BpmnModelUtils.parseDslConfig(flowElement);
            if (StrUtil.isNotBlank(dslConfig)) {
                JSONObject dslJson = JSONUtil.parseObj(dslConfig);
                JSONObject assignJson = dslJson.getJSONObject("assign");
                if (assignJson != null) {
                    Integer approvalMode = assignJson.getInt("approvalMode");
                    if (approvalMode != null) {
                        log.info("[getApprovalMode] 节点 {} 使用 DSL approvalMode: {}",
                                flowElement.getId(), approvalMode);
                        return approvalMode;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[getApprovalMode] 解析 approvalMode 失败: {}", e.getMessage());
        }
        // 默认使用抢签模式
        log.info("[getApprovalMode] 节点 {} 未配置 approvalMode，使用默认抢签模式(0)",
                execution.getCurrentFlowElement().getId());
        return APPROVAL_MODE_GRAB;
    }

    /**
     * 计算所有候选人
     */
    @SuppressWarnings("unchecked")
    private Set<Long> calculateAllCandidateUsers(DelegateExecution execution) {
        // 情况一，如果是多实例的任务，例如说会签、或签等情况，则从 Variable 中获取。
        if (super.multiInstanceActivityBehavior != null) {
            return (Set<Long>) execution.getVariable(super.multiInstanceActivityBehavior.getCollectionElementVariable());
        }

        // 情况二，如果非多实例的任务，则计算任务处理人
        return taskCandidateInvoker.calculateUsersByTask(execution);
    }

    /**
     * 从候选人中选择一个作为处理人（指定模式使用）
     */
    private Long selectAssigneeFromCandidates(Set<Long> candidateUserIds) {
        if (CollUtil.isEmpty(candidateUserIds)) {
            return null;
        }
        // 随机选择一个
        int index = RandomUtil.randomInt(candidateUserIds.size());
        return CollUtil.get(candidateUserIds, index);
    }

    @Override
    protected void handleCategory(CreateUserTaskBeforeContext beforeContext, ExpressionManager expressionManager,
                                  TaskEntity task, DelegateExecution execution) {
        ProcessDefinitionEntity processDefinitionEntity = CommandContextUtil.getProcessDefinitionEntityManager().findById(execution.getProcessDefinitionId());
        if (processDefinitionEntity == null) {
            log.warn("[handleCategory][任务编号({}) 找不到流程定义({})]", task.getId(), execution.getProcessDefinitionId());
            return;
        }
        task.setCategory(processDefinitionEntity.getCategory());
    }

}
