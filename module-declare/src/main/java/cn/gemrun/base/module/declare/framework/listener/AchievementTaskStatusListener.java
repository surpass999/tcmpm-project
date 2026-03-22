package cn.gemrun.base.module.declare.framework.listener;

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
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
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

import cn.hutool.core.util.StrUtil;

/**
 * 成果任务状态监听器
 * 监听成果流程的任务完成事件，更新成果状态
 * <p>
 * 支持 bizStatus 格式：
 * - 普通格式：bizStatus（如：SUBMITTED、APPROVED）
 * - 带条件格式：bizStatus | condition（如：APPROVED | TO_ARCHIVE）
 *
 * @author Gemini
 */
@Component
@Slf4j
public class AchievementTaskStatusListener extends BpmTaskStatusEventListener {

    private static final String PROCESS_DEFINITION_KEY = "declare_achievement";

    /**
     * 选择专家按钮ID
     */
    private static final Integer BUTTON_ID_SELECT_EXPERT = 8;

    /**
     * 发起整改按钮ID
     */
    private static final Integer BUTTON_ID_RECTIFICATION_RETURN = 9;

    @Resource
    private AchievementService achievementService;

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
        Long achievementId;
        try {
            achievementId = parseBusinessId(event);
        } catch (Exception e) {
            log.warn("[AchievementTaskStatusListener] 解析成果ID失败: businessKey={}", event.getBusinessKey(), e);
            return;
        }

        String bizStatus = event.getBizStatus();
        Integer buttonId = event.getButtonId();
        log.info("[AchievementTaskStatusListener] 收到任务状态变更事件: achievementId={}, taskKey={}, bizStatus={}, buttonId={}",
                achievementId, event.getTaskDefinitionKey(), bizStatus, buttonId);

        // 判断是否是选择专家按钮（buttonId=8）
        if (BUTTON_ID_SELECT_EXPERT.equals(buttonId)) {
            handleSelectExpert(achievementId, event);
            return;
        }

        // 判断是否是发起整改按钮（buttonId=9）
        if (BUTTON_ID_RECTIFICATION_RETURN.equals(buttonId)) {
            handleRectify(achievementId, event, event.getVariables());
            return;
        }

        if (bizStatus != null && !bizStatus.isEmpty()) {
            achievementService.updateAchievementStatus(achievementId, bizStatus);
            log.info("[AchievementTaskStatusListener] 更新成果状态: achievementId={}, bizStatus={}",
                    achievementId, bizStatus);
        }
    }

    /**
     * 处理选择专家按钮事件
     * 当 buttonId=8 时，创建评审任务记录和评审结果记录
     */
    @Transactional(rollbackFor = Exception.class)
    protected void handleSelectExpert(Long achievementId, BpmTaskStatusEvent event) {
        String processInstanceId = event.getProcessInstanceId();
        log.info("[AchievementTaskStatusListener] 处理选择专家事件: achievementId={}, processInstanceId={}",
                achievementId, processInstanceId);

        // 1. 从流程变量中获取选择的专家ID
        String expertIdsStr = getExpertIdsFromVariables(processInstanceId);
        if (expertIdsStr == null || expertIdsStr.isEmpty()) {
            log.warn("[AchievementTaskStatusListener] 未获取到专家IDs，processInstanceId={}", processInstanceId);
            return;
        }
        log.info("[AchievementTaskStatusListener] 获取到专家IDs: {}", expertIdsStr);

        // 2. 查询刚创建的专家评审任务
        String firstExpertId = expertIdsStr.split(",")[0];
        List<Task> expertTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(firstExpertId)
                .list();

        if (expertTasks.isEmpty()) {
            log.warn("[AchievementTaskStatusListener] 未找到专家任务，processInstanceId={}", processInstanceId);
            return;
        }

        Task expertTask = expertTasks.get(0);

        // 3. 创建评审任务记录
        ReviewTaskDO reviewTask = new ReviewTaskDO();
        reviewTask.setProcessInstanceId(processInstanceId);
        reviewTask.setTaskDefinitionKey(expertTask.getTaskDefinitionKey());
        reviewTask.setTaskName(expertTask.getName());
        reviewTask.setBusinessType(3); // 业务类型：3=成果
        reviewTask.setBusinessId(achievementId);
        reviewTask.setTaskType(4); // 任务类型：4=成果审核
        reviewTask.setExpertIds(expertIdsStr);
        reviewTask.setStatus(1); // 评审中
        reviewTask.setStartTime(LocalDateTime.now());

        reviewTaskMapper.insert(reviewTask);

        log.info("[AchievementTaskStatusListener] 创建评审任务成功: reviewTaskId={}, achievementId={}",
                reviewTask.getId(), achievementId);

        // 4. 为每个专家创建评审结果记录
        String[] userIdArray = expertIdsStr.split(",");
        for (int i = 0; i < userIdArray.length; i++) {
            String userIdStr = userIdArray[i].trim();
            if (userIdStr.isEmpty()) {
                continue;
            }
            Long userId = Long.parseLong(userIdStr);

            Task singleExpertTask = null;
            if (i < expertTasks.size()) {
                singleExpertTask = expertTasks.get(i);
            } else {
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
            reviewResult.setBusinessType(3); // 3=成果
            reviewResult.setBusinessId(achievementId);
            reviewResult.setStatus(0); // 待评审
            reviewResult.setIsConflict(false);
            reviewResult.setIsAvoid(false);

            reviewResultMapper.insert(reviewResult);
            log.info("[AchievementTaskStatusListener] 创建评审结果: resultId={}, userId={}",
                    reviewResult.getId(), userId);
        }

        log.info("[AchievementTaskStatusListener] 创建评审结果完成: taskId={}, expertCount={}",
                reviewTask.getId(), userIdArray.length);
    }

    /**
     * 从流程变量中获取选择的专家ID
     */
    private String getExpertIdsFromVariables(String processInstanceId) {
        Object startUserSelectObj = runtimeService.getVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
        Object approveUserSelectObj = runtimeService.getVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);

        StringBuilder expertIdsBuilder = new StringBuilder();

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
     * 处理发起整改按钮事件
     * 当 bizStatus=NEED_RECTIFY 时，启动整改子流程
     */
    private void handleRectify(Long achievementId, BpmTaskStatusEvent event, Map<String, Object> variables) {
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
        log.info("[handleRectify] 准备启动整改子流程: achievementId={}, rectifyProcessKey={}, initiatorId={}",
                achievementId, rectifyProcessKey, initiatorId);

        // 3. 构建子流程 businessKey: 整改流程Key_achievementId
        String childBusinessKey = rectifyProcessKey + "_" + achievementId;

        // 4. 设置子流程的发起人（Flowable 引擎自动使用此 ID 作为子流程发起人）
        FlowableUtils.setAuthenticatedUserId(initiatorId);

        // 5. 传递变量给子流程
        Map<String, Object> childVariables = new HashMap<>();
        childVariables.put("businessId", achievementId);
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
            log.error("[handleRectify] 启动整改子流程失败: rectifyProcessKey={}, achievementId={}", rectifyProcessKey, achievementId, e);
        }
    }

}
