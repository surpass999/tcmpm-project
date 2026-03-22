package cn.gemrun.base.module.declare.framework.listener;

import cn.hutool.core.util.StrUtil;
import cn.gemrun.base.framework.common.util.number.NumberUtils;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewResultMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 备案任务状态监听器
 * 监听备案流程的任务完成事件，更新备案状态
 * <p>
 * 支持 bizStatus 格式：
 * - 普通格式：bizStatus（如：SUBMITTED、NATION_APPROVED）
 * - 带条件格式：bizStatus | condition（如：NATION_APPROVED | TO_PROJECT）
 * <p>
 * 注意：当 bizStatus 包含 | 分隔符时，前半部分更新业务状态，后半部分执行业务操作。
 * 例如：NATION_APPROVED | TO_PROJECT 表示更新状态为国家局审核通过，并执行转项目操作。
 *
 * @author Gemini
 */
@Component
@Slf4j
public class DeclareFilingTaskStatusListener extends BpmTaskStatusEventListener {

    private static final String PROCESS_DEFINITION_KEY = "declare_filing";

    /**
     * 选择专家按钮ID
     */
    private static final Integer BUTTON_ID_SELECT_EXPERT = 8;

    /**
     * 发起整改按钮ID
     */
    private static final Integer BUTTON_ID_RECTIFICATION_RETURN = 9;

    /**
     * bizStatus 中的条件分隔符
     */
    private static final String STATUS_CONDITION_SEPARATOR = "\\|";

    /**
     * 转项目条件标识
     */
    private static final String CONDITION_TO_PROJECT = "TO_PROJECT";

    @Resource
    private FilingService filingService;

    @Resource
    private ReviewTaskMapper reviewTaskMapper;

    @Resource
    private ReviewResultMapper reviewResultMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        // 解析 businessKey: declare_filing_1 -> 业务ID = 1
        Long filingId;
        try {
            filingId = parseBusinessId(event);
        } catch (Exception e) {
            log.warn("[DeclareFilingTaskStatusListener] 解析备案ID失败: businessKey={}", event.getBusinessKey(), e);
            return;
        }

        String taskDefinitionKey = event.getTaskDefinitionKey();
        String bizStatus = event.getBizStatus();
        Integer buttonId = event.getButtonId();

        // 记录接收到的状态变更事件
        log.info("[DeclareFilingTaskStatusListener] 收到任务状态变更事件: filingId={}, taskKey={}, bizStatus={}, buttonId={}",
                filingId, taskDefinitionKey, bizStatus, buttonId);

        // 判断是否是选择专家按钮（buttonId=8）
        if (BUTTON_ID_SELECT_EXPERT.equals(buttonId)) {
            handleSelectExpert(filingId, event);
            return;
        }

        // 判断是否是发起整改按钮
        if (BUTTON_ID_RECTIFICATION_RETURN.equals(buttonId)) {
            handleRectify(filingId, event, event.getVariables());
            return;
        }

        // 获取业务状态，如果有则更新
        if (bizStatus != null && !bizStatus.isEmpty()) {
            // 解析 bizStatus 格式：状态值 | 条件标识（如：NATION_APPROVED | TO_PROJECT）
            if (bizStatus.contains("|")) {
                String[] statusParts = bizStatus.split(STATUS_CONDITION_SEPARATOR);
                String filingStatus = statusParts[0].trim();
                String condition = statusParts.length > 1 ? statusParts[1].trim() : null;

                // 更新备案状态（内部会自动处理转项目逻辑：当状态为 NATION_APPROVED 时）
                filingService.updateFilingStatus(filingId, filingStatus);
                log.info("[DeclareFilingTaskStatusListener] 更新备案状态为{}，条件={}: filingId={}",
                        filingStatus, condition, filingId);

                // 如果条件是 TO_PROJECT，且状态不是 NATION_APPROVED，手动触发转项目
                // （FilingServiceImpl.updateFilingStatus 只在 NATION_APPROVED 时自动转项目）
                if (CONDITION_TO_PROJECT.equals(condition) && !"NATION_APPROVED".equals(filingStatus)) {
                    log.warn("[DeclareFilingTaskStatusListener] 条件为TO_PROJECT但状态不是NATION_APPROVED，请检查流程配置: filingId={}, status={}",
                            filingId, filingStatus);
                }
            } else {
                // 兼容旧格式：直接使用 bizStatus 作为备案状态
                filingService.updateFilingStatus(filingId, bizStatus);
                log.info("[DeclareFilingTaskStatusListener] 更新备案状态为{}: filingId={}, taskKey={}",
                        bizStatus, filingId, taskDefinitionKey);
            }
        } else {
            // bizStatus 为空时，只有发起人节点才更新状态为 SUBMITTED
            // 中间节点（专家评审等）bizStatus 为空时不更新业务状态，由流程设计保证各节点正确配置 bizStatus
            if (isStartUserNode(taskDefinitionKey)) {
                filingService.updateFilingStatus(filingId, "SUBMITTED");
                log.info("[DeclareFilingTaskStatusListener] 发起人节点完成，更新备案状态为SUBMITTED: filingId={}, taskKey={}",
                        filingId, taskDefinitionKey);
            } else {
                log.info("[DeclareFilingTaskStatusListener] 中间节点 bizStatus 为空，不更新备案状态: filingId={}, taskKey={}",
                        filingId, taskDefinitionKey);
            }
        }
    }

    /**
     * 处理选择专家按钮事件
     * 当 buttonId=8 时，创建评审任务记录和评审结果记录
     */
    @Transactional(rollbackFor = Exception.class)
    protected void handleSelectExpert(Long filingId, BpmTaskStatusEvent event) {
        String processInstanceId = event.getProcessInstanceId();
        log.info("[DeclareFilingTaskStatusListener] 处理选择专家事件: filingId={}, processInstanceId={}",
                filingId, processInstanceId);

        // 1. 从流程变量中获取选择的专家ID
        String expertIdsStr = getExpertIdsFromVariables(processInstanceId);
        if (expertIdsStr == null || expertIdsStr.isEmpty()) {
            log.warn("[DeclareFilingTaskStatusListener] 未获取到专家IDs，processInstanceId={}", processInstanceId);
            return;
        }
        log.info("[DeclareFilingTaskStatusListener] 获取到专家IDs: {}", expertIdsStr);

        // 2. 查询刚创建的专家评审任务
        String firstExpertId = expertIdsStr.split(",")[0];
        List<Task> expertTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(firstExpertId)
                .list();

        if (expertTasks.isEmpty()) {
            log.warn("[DeclareFilingTaskStatusListener] 未找到专家任务，processInstanceId={}", processInstanceId);
            return;
        }

        // 3. 获取专家任务的节点信息
        Task expertTask = expertTasks.get(0);
        log.info("[DeclareFilingTaskStatusListener] 找到专家任务: taskId={}, taskKey={}, taskName={}",
                expertTask.getId(), expertTask.getTaskDefinitionKey(), expertTask.getName());

        // 4. 创建评审任务记录
        ReviewTaskDO reviewTask = new ReviewTaskDO();
        reviewTask.setProcessInstanceId(processInstanceId);
        reviewTask.setTaskDefinitionKey(expertTask.getTaskDefinitionKey());
        reviewTask.setTaskName(expertTask.getName());
        reviewTask.setBusinessType(1); // 业务类型：1=备案
        reviewTask.setBusinessId(filingId);
        reviewTask.setTaskType(1); // 任务类型：1=备案论证
        reviewTask.setExpertIds(expertIdsStr); // 专家IDs
        reviewTask.setStatus(1); // 评审中
        reviewTask.setStartTime(LocalDateTime.now());

        reviewTaskMapper.insert(reviewTask);

        log.info("[DeclareFilingTaskStatusListener] 创建评审任务成功: reviewTaskId={}, filingId={}, expertIds={}",
                reviewTask.getId(), filingId, expertIdsStr);

        // 5. 为每个专家创建评审结果记录
        String[] userIdArray = expertIdsStr.split(",");
        for (int i = 0; i < userIdArray.length; i++) {
            String userIdStr = userIdArray[i].trim();
            if (userIdStr.isEmpty()) {
                continue;
            }
            Long userId = Long.parseLong(userIdStr);

            // 查找该专家的任务
            Task singleExpertTask = null;
            if (i < expertTasks.size()) {
                singleExpertTask = expertTasks.get(i);
            } else {
                // 如果任务数少于专家数，查其他任务
                List<Task> otherTasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .taskAssignee(userIdStr)
                        .list();
                if (!otherTasks.isEmpty()) {
                    singleExpertTask = otherTasks.get(0);
                }
            }

            ReviewResultDO reviewResult = new ReviewResultDO();
            reviewResult.setTaskId(reviewTask.getId());
            reviewResult.setProcessInstanceId(processInstanceId);
            reviewResult.setFlowableTaskId(singleExpertTask != null ? singleExpertTask.getId() : null);
            reviewResult.setExpertId(userId); // 存用户ID
            reviewResult.setBusinessType(1); // 1=备案
            reviewResult.setBusinessId(filingId);
            reviewResult.setStatus(0); // 待评审
            reviewResult.setIsConflict(false);
            reviewResult.setIsAvoid(false);

            reviewResultMapper.insert(reviewResult);
            log.info("[DeclareFilingTaskStatusListener] 创建评审结果: resultId={}, userId={}",
                    reviewResult.getId(), userId);
        }

        log.info("[DeclareFilingTaskStatusListener] 创建评审结果完成: taskId={}, expertCount={}",
                reviewTask.getId(), userIdArray.length);
    }

    /**
     * 从流程变量中获取选择的专家ID
     */
    private String getExpertIdsFromVariables(String processInstanceId) {
        // 获取审批人选择变量
        Object startUserSelectObj = runtimeService.getVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
        Object approveUserSelectObj = runtimeService.getVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);

        StringBuilder expertIdsBuilder = new StringBuilder();

        // 解析 START_USER_SELECT 变量
        if (startUserSelectObj instanceof Map) {
            Map<?, ?> startUserSelectMap = (Map<?, ?>) startUserSelectObj;
            for (Map.Entry<?, ?> entry : startUserSelectMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    List<?> userList = (List<?>) value;
                    for (Object userId : userList) {
                        if (expertIdsBuilder.length() > 0) {
                            expertIdsBuilder.append(",");
                        }
                        expertIdsBuilder.append(userId);
                    }
                }
            }
        }

        // 解析 APPROVE_USER_SELECT 变量
        if (approveUserSelectObj instanceof Map) {
            Map<?, ?> approveUserSelectMap = (Map<?, ?>) approveUserSelectObj;
            for (Map.Entry<?, ?> entry : approveUserSelectMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    List<?> userList = (List<?>) value;
                    for (Object userId : userList) {
                        if (expertIdsBuilder.length() > 0) {
                            expertIdsBuilder.append(",");
                        }
                        expertIdsBuilder.append(userId);
                    }
                }
            }
        }

        return expertIdsBuilder.toString();
    }

    /**
     * 判断是否是发起人节点
     */
    private boolean isStartUserNode(String taskDefinitionKey) {
        return "StartUserNode".equals(taskDefinitionKey)
                || "startEvent1".equals(taskDefinitionKey)
                || (taskDefinitionKey != null && taskDefinitionKey.toLowerCase().contains("start"));
    }

    /**
     * 处理发起整改按钮事件
     * 当 bizStatus=NEED_RECTIFY 时，启动整改子流程
     */
    private void handleRectify(Long filingId, BpmTaskStatusEvent event, Map<String, Object> variables) {
        // 1. 从按钮配置的 rectifyProcessDefinitionKey 读取子流程定义 Key
        String rectifyProcessKey = variables != null
                ? (String) variables.get("rectifyProcessDefinitionKey") : null;
        if (StrUtil.isEmpty(rectifyProcessKey)) {
            log.warn("[handleRectify] bizStatus=NEED_RECTIFY 但未找到 rectifyProcessDefinitionKey, event={}", event);
            return;
        }

        // 2. 获取子流程发起人（当前操作人，即审批人自己）
        Long initiatorId = event.getUserId();
        if (initiatorId == null) {
            log.warn("[handleRectify] 审批人ID为空，无法启动整改子流程, event={}", event);
            return;
        }
        log.info("[handleRectify] 准备启动整改子流程: filingId={}, rectifyProcessKey={}, initiatorId={}",
                filingId, rectifyProcessKey, initiatorId);

        // 3. 构建子流程 businessKey: 整改流程Key_filingId
        String childBusinessKey = rectifyProcessKey + "_" + filingId;

        // 4. 设置子流程的发起人（Flowable 引擎自动使用此 ID 作为子流程发起人）
        FlowableUtils.setAuthenticatedUserId(initiatorId);

        // 5. 传递变量给子流程
        Map<String, Object> childVariables = new HashMap<>();
        childVariables.put("businessId", filingId);
        childVariables.put("businessType", event.getBusinessType());
        childVariables.put("PARENT_PROCESS_INSTANCE_ID", event.getProcessInstanceId());
        childVariables.put("RECTIFY_INITIATOR_ID", initiatorId);
        // 设置子流程状态为"审批中"，避免子流程完成时缺少状态变量
        childVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        if (event.getReason() != null) {
            childVariables.put("rectifyOpinion", event.getReason());
            childVariables.put("RECTIFY_REASON", event.getReason());
        }

        // 6. 获取主流程的业务创建人ID并传递给子流程
        String parentProcessInstanceId = event.getProcessInstanceId();
        Object parentBusinessCreatorId = runtimeService.getVariable(parentProcessInstanceId, "businessCreatorId");
        if (parentBusinessCreatorId != null) {
            childVariables.put("businessCreatorId", NumberUtils.parseLong(parentBusinessCreatorId.toString()));
            log.info("[handleRectify] 从主流程获取 businessCreatorId={}", parentBusinessCreatorId);
        }

        // 6. 启动整改子流程
        try {
            org.flowable.engine.runtime.ProcessInstance childInstance = runtimeService.startProcessInstanceByKey(
                    rectifyProcessKey, childBusinessKey, childVariables);
            log.info("[handleRectify] 整改子流程已启动: parentProcessInstanceId={}, childProcessInstanceId={}, initiatorId={}",
                    event.getProcessInstanceId(), childInstance.getId(), initiatorId);
        } catch (Exception e) {
            log.error("[handleRectify] 启动整改子流程失败: rectifyProcessKey={}, filingId={}", rectifyProcessKey, filingId, e);
        }
    }

}
