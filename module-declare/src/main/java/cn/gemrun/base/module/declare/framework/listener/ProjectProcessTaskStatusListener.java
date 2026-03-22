package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.framework.common.util.json.JsonUtils;
import cn.gemrun.base.framework.common.util.number.NumberUtils;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.bpm.dal.dataobject.business.BpmBusinessTypeDO;
import cn.gemrun.base.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectProcessRespVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectProcessMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewResultMapper;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewTaskMapper;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorValueService;
import cn.gemrun.base.module.declare.service.project.ProcessIndicatorConfigService;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 项目过程任务状态监听器
 * 监听项目相关流程的任务状态事件，更新项目过程记录和项目主表状态
 *
 * @author Gemini
 */
@Component
@Slf4j
public class ProjectProcessTaskStatusListener extends BpmTaskStatusEventListener {

    /**
     * 选择专家按钮ID
     */
    private static final Integer BUTTON_ID_SELECT_EXPERT = 8;

    /**
     * 发起整改按钮ID
     */
    private static final Integer BUTTON_ID_RECTIFICATION_RETURN = 9;

    /**
     * 业务类型前缀，匹配 project 开头的业务类型
     */
    private static final String BUSINESS_TYPE_PREFIX = "project";

    /**
     * 子流程变量名：父流程实例ID
     */
    private static final String PARENT_PROCESS_INSTANCE_ID_VAR = "PARENT_PROCESS_INSTANCE_ID";

    @Resource
    private ProjectProcessService projectProcessService;

    @Resource
    private ProjectService projectService;

    @Resource
    private ReviewTaskMapper reviewTaskMapper;

    @Resource
    private ReviewResultMapper reviewResultMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private BpmBusinessTypeService bpmBusinessTypeService;

    @Resource
    private ProjectProcessMapper projectProcessMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private DeclareIndicatorValueMapper indicatorValueMapper;

    @Resource
    private DeclareIndicatorService declareIndicatorService;

    @Resource
    private DeclareIndicatorValueService declareIndicatorValueService;

    @Resource
    private ProcessIndicatorConfigService processIndicatorConfigService;

    @Override
    protected String getProcessDefinitionKey() {
        return "";
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        log.info("[ProjectProcessTaskStatusListener] 收到任务状态变更: event={}", event);

        // ========== 修复：支持子流程中触发选择专家事件 ==========
        // 检查是否是整改子流程
        boolean isChildProcess = event.getVariables() != null
                && event.getVariables().containsKey(PARENT_PROCESS_INSTANCE_ID_VAR);

        String businessType = getBusinessType(event);
        if (businessType == null) {
            businessType = resolveBusinessTypeFromProcessDefinitionKey(event.getProcessDefinitionKey());
            log.info("[ProjectProcessTaskStatusListener] 事件中无 businessType，按 processDefinitionKey 反查: businessType={}", businessType);
        }

        // 如果是子流程，尝试从父流程获取 businessType
        if (isChildProcess && (businessType == null || !businessType.startsWith(BUSINESS_TYPE_PREFIX))) {
            String parentProcessInstanceId = (String) event.getVariables().get(PARENT_PROCESS_INSTANCE_ID_VAR);
            if (parentProcessInstanceId != null) {
                Object parentBusinessType = runtimeService.getVariable(parentProcessInstanceId, "businessType");
                if (parentBusinessType != null) {
                    businessType = parentBusinessType.toString();
                    log.info("[ProjectProcessTaskStatusListener] 从父流程获取到 businessType={}", businessType);
                }
            }
        }

        if (businessType == null || !businessType.startsWith(BUSINESS_TYPE_PREFIX)) {
            log.info("[ProjectProcessTaskStatusListener] businessType={} 不匹配，跳过", businessType);
            return;
        }
        log.info("[ProjectProcessTaskStatusListener] 处理任务状态变更: businessType={}", businessType);
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        Long processId;
        try {
            processId = parseBusinessId(event);
        } catch (Exception e) {
            log.warn("[ProjectProcessTaskStatusListener] 解析过程记录ID失败: businessKey={}", businessKey, e);
            return;
        }

        String bizStatus = event.getBizStatus();
        Integer buttonId = event.getButtonId();
        log.info("[ProjectProcessTaskStatusListener] 收到任务状态变更: processId={}, bizStatus={}, buttonId={}, taskStatus={}",
                processId, bizStatus, buttonId, event.getStatus());

        // 判断是否是选择专家按钮（buttonId=8）
        if (BUTTON_ID_SELECT_EXPERT.equals(buttonId)) {
            handleSelectExpert(processId, event);
            // 选择专家后仍需要更新过程记录状态
        }

        // 判断是否是发起整改按钮（buttonId=9）
        if (BUTTON_ID_RECTIFICATION_RETURN.equals(buttonId)) {
            handleRectify(processId, event, event.getVariables());
            return;
        }

        // ========== 1. 更新过程记录表 ==========
        String taskDefinitionKey = event.getTaskDefinitionKey();
        if (bizStatus == null || bizStatus.isEmpty()) {
            // bizStatus 为空时，只有发起人节点才更新状态为 SUBMITTED
            // 中间节点 bizStatus 为空时跳过，不覆盖已有状态
            if (isStartUserNode(taskDefinitionKey)) {
                projectProcessService.updateProjectProcessStatus(processId, "SUBMITTED");
                log.info("[ProjectProcessTaskStatusListener] 发起人节点完成，更新状态为SUBMITTED: processId={}", processId);
            } else {
                log.info("[ProjectProcessTaskStatusListener] 中间节点 bizStatus 为空，跳过状态更新: processId={}, taskKey={}", processId, taskDefinitionKey);
            }
            return;
        }
        // 定义临时变量ProjectProcessStatus 做为最终更新状态
        String ProjectProcessStatus = "";
        // 判断bizStatus是否包含竖线
        if (bizStatus.contains("|")) {
            String[] bizStatusArray = bizStatus.split("\\|");
            ProjectProcessStatus = bizStatusArray[0];
        } else {
            ProjectProcessStatus = bizStatus;
        }
        
        // bizStatus 有值 → 直接用 bizStatus 更新状态  ****|ARCHIVE 中间有竖线 要分离
        projectProcessService.updateProjectProcessStatus(processId, ProjectProcessStatus);
        log.info("[ProjectProcessTaskStatusListener] 更新过程记录状态: processId={}, status={}", processId, ProjectProcessStatus);

        // ========== 2. 更新项目主表状态为过程类型ID（表示项目当前进展到哪个阶段）==========
        ProjectProcessRespVO processVO = projectProcessService.getProjectProcess(processId);
        if (processVO != null && processVO.getProjectId() != null && processVO.getProcessType() != null) {
            projectService.updateProjectStatus(processVO.getProjectId(), String.valueOf(processVO.getProcessType()));
            log.info("[ProjectProcessTaskStatusListener] 更新项目主表状态: projectId={}, processType={}",
                    processVO.getProjectId(), processVO.getProcessType());

            // ========== 3. 根据过程类型更新项目进度 ==========
            Integer progress = calculateProgressByProcessType(processVO.getProcessType());
            if (progress != null) {
                projectService.updateProjectProgress(processVO.getProjectId(), progress);
            }

            // ===== 新增：检测 bizStatus 是否包含 FINISH 标识，标记项目完成 =====
            if (bizStatus != null && bizStatus.contains("FINISH")) {
                projectService.markProjectCompleted(processVO.getProjectId());
                log.info("[ProjectProcessTaskStatusListener] 检测到FINISH标识，标记项目完成: projectId={}, bizStatus={}",
                        processVO.getProjectId(), bizStatus);
            }

            // ===== 新增：检测 bizStatus 是否包含 ARCHIVE 标识，同步指标到项目表和指标值表 =====
            if (bizStatus != null && bizStatus.contains("ARCHIVE")) {
                syncIndicatorsOnArchive(processId, processVO, event);
            }
        }
    }

    /**
     * 根据过程类型计算项目进度
     *
     * @param processType 过程类型（1=建设过程, 2=半年报, 3=年度总结, 4=中期评估, 5=整改记录, 6=验收申请）
     * @return 进度值（0-100），返回 null 表示不更新进度
     */
    private Integer calculateProgressByProcessType(Integer processType) {
        if (processType == null) {
            return null;
        }
        switch (processType) {
            case 1:
                return 20;  // 建设过程
            case 2:
                return 30;  // 半年报
            case 3:
                return 40;  // 年度总结
            case 4:
                return 50;  // 中期评估
            case 5:
                return null; // 整改记录不影响进度
            case 6:
                return 100; // 验收申请
            default:
                return null;
        }
    }

    /**
     * 处理选择专家按钮事件
     * 当 buttonId=8 时，创建评审任务记录和评审结果记录
     * 支持主流程和整改子流程
     */
    @Transactional(rollbackFor = Exception.class)
    protected void handleSelectExpert(Long processId, BpmTaskStatusEvent event) {
        String processInstanceId = event.getProcessInstanceId();
        log.info("[ProjectProcessTaskStatusListener] 处理选择专家事件: processId={}, processInstanceId={}",
                processId, processInstanceId);

        // ========== 修复：支持子流程，从流程变量中获取正确的 businessId ==========
        // 检查是否是整改子流程（通过 PARENT_PROCESS_INSTANCE_ID 变量判断）
        Object parentProcessInstanceId = runtimeService.getVariable(processInstanceId, "PARENT_PROCESS_INSTANCE_ID");
        if (parentProcessInstanceId != null) {
            // 从子流程变量中获取正确的 businessId
            Object businessIdObj = runtimeService.getVariable(processInstanceId, "businessId");
            if (businessIdObj != null) {
                try {
                    if (businessIdObj instanceof Long) {
                        processId = (Long) businessIdObj;
                    } else {
                        processId = Long.parseLong(businessIdObj.toString());
                    }
                    log.info("[ProjectProcessTaskStatusListener] 从子流程变量获取到正确的processId={}", processId);
                } catch (NumberFormatException e) {
                    log.warn("[ProjectProcessTaskStatusListener] 子流程businessId格式错误: {}", businessIdObj);
                }
            }
        }
        // ========== 修复结束 ==========

        // 获取过程记录信息来确定项目ID
        ProjectProcessRespVO processVO = projectProcessService.getProjectProcess(processId);
        if (processVO == null) {
            log.warn("[ProjectProcessTaskStatusListener] 过程记录不存在，无法创建评审任务: processId={}", processId);
            return;
        }

        // 根据过程类型确定任务类型：2=中期评估，3=验收评审
        Integer taskType = getTaskTypeFromProcess(processVO);

        // 1. 从流程变量中获取选择的专家ID
        String expertIdsStr = getExpertIdsFromVariables(processInstanceId);
        if (expertIdsStr == null || expertIdsStr.isEmpty()) {
            log.warn("[ProjectProcessTaskStatusListener] 未获取到专家IDs，processInstanceId={}", processInstanceId);
            return;
        }
        log.info("[ProjectProcessTaskStatusListener] 获取到专家IDs: {}", expertIdsStr);

        // 2. 查询刚创建的专家评审任务
        String firstExpertId = expertIdsStr.split(",")[0];
        List<Task> expertTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(firstExpertId)
                .list();

        if (expertTasks.isEmpty()) {
            log.warn("[ProjectProcessTaskStatusListener] 未找到专家任务，processInstanceId={}", processInstanceId);
            return;
        }

        Task expertTask = expertTasks.get(0);

        // 3. 创建评审任务记录
        ReviewTaskDO reviewTask = new ReviewTaskDO();
        reviewTask.setProcessInstanceId(processInstanceId);
        reviewTask.setTaskDefinitionKey(expertTask.getTaskDefinitionKey());
        reviewTask.setTaskName(expertTask.getName());
        reviewTask.setBusinessType(2); // 业务类型：2=项目
        reviewTask.setBusinessId(processId);
        reviewTask.setTaskType(taskType);
        reviewTask.setExpertIds(expertIdsStr);
        reviewTask.setStatus(1); // 评审中
        reviewTask.setStartTime(LocalDateTime.now());

        reviewTaskMapper.insert(reviewTask);

        log.info("[ProjectProcessTaskStatusListener] 创建评审任务成功: reviewTaskId={}, processId={}, taskType={}",
                reviewTask.getId(), processId, taskType);

        // 4. 为每个专家创建评审结果记录
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
            reviewResult.setBusinessType(2); // 2=项目
            reviewResult.setBusinessId(processId);
            reviewResult.setStatus(0); // 待评审
            reviewResult.setIsConflict(false);
            reviewResult.setIsAvoid(false);

            reviewResultMapper.insert(reviewResult);
            log.info("[ProjectProcessTaskStatusListener] 创建评审结果: resultId={}, userId={}",
                    reviewResult.getId(), userId);
        }

        log.info("[ProjectProcessTaskStatusListener] 创建评审结果完成: taskId={}, expertCount={}",
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
     * 根据过程类型确定评审任务类型
     * taskType: 2=中期评估，3=验收评审
     */
    private Integer getTaskTypeFromProcess(ProjectProcessRespVO processVO) {
        if (processVO.getProcessType() == null) {
            return 2; // 默认中期评估
        }
        // 根据 processType 判断：假设 1=开题，2=中期，3=验收
        switch (processVO.getProcessType()) {
            case 3:
                return 3; // 验收评审
            case 2:
            default:
                return 2; // 中期评估
        }
    }

    /**
     * 判断是否是发起人节点
     * 发起人节点完成时设置 SUBMITTED，其他节点不处理
     */
    private boolean isStartUserNode(String taskDefinitionKey) {
        return "StartUserNode".equals(taskDefinitionKey)
                || "startEvent1".equals(taskDefinitionKey)
                || (taskDefinitionKey != null && taskDefinitionKey.toLowerCase().contains("start"));
    }

    /**
     * 按 processDefinitionKey 从 bpm_business_type 表反查 businessType
     */
    private String resolveBusinessTypeFromProcessDefinitionKey(String processDefinitionKey) {
        if (processDefinitionKey == null || processDefinitionKey.isEmpty()) {
            return null;
        }
        try {
            List<BpmBusinessTypeDO> list = bpmBusinessTypeService.getBusinessTypeListByProcessKey(processDefinitionKey);
            if (CollUtil.isNotEmpty(list)) {
                return list.get(0).getBusinessType();
            }
        } catch (Exception e) {
            log.warn("[ProjectProcessTaskStatusListener] 反查 businessType 失败: processDefinitionKey={}", processDefinitionKey, e);
        }
        return null;
    }

    /**
     * 处理发起整改按钮事件
     * 当 bizStatus=NEED_RECTIFY 时，启动整改子流程
     */
    private void handleRectify(Long processId, BpmTaskStatusEvent event, Map<String, Object> variables) {
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
        log.info("[handleRectify] 准备启动整改子流程: processId={}, rectifyProcessKey={}, initiatorId={}",
                processId, rectifyProcessKey, initiatorId);

        // 3. 构建子流程 businessKey: 整改流程Key_processId
        String childBusinessKey = rectifyProcessKey + "_" + processId;

        // 4. 设置子流程的发起人（Flowable 引擎自动使用此 ID 作为子流程发起人）
        FlowableUtils.setAuthenticatedUserId(initiatorId);

        // 5. 传递变量给子流程
        Map<String, Object> childVariables = new HashMap<>();
        childVariables.put("businessId", processId);
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
            log.error("[handleRectify] 启动整改子流程失败: rectifyProcessKey={}, processId={}", rectifyProcessKey, processId, e);
        }
    }

    /**
     * 归档时同步指标数据
     * 1. 更新项目表的4个财务字段
     * 2. 同步全部指标值到 declare_indicator_value 表
     */
    private void syncIndicatorsOnArchive(Long processId, ProjectProcessRespVO processVO, BpmTaskStatusEvent event) {
        try {
            // 1. 获取过程记录
            ProjectProcessDO process = projectProcessMapper.selectById(processId);
            if (process == null) {
                log.warn("[syncIndicatorsOnArchive] 过程记录不存在: processId={}", processId);
                return;
            }

            // 2. 解析 indicator_values JSON
            Map<String, Object> indicatorValues = parseIndicatorValues(process.getIndicatorValues());
            if (indicatorValues == null || indicatorValues.isEmpty()) {
                log.info("[syncIndicatorsOnArchive] 指标值为空: processId={}", processId);
                return;
            }

            // 3. 获取项目信息
            ProjectDO project = projectMapper.selectById(process.getProjectId());
            if (project == null) {
                log.warn("[syncIndicatorsOnArchive] 项目不存在: projectId={}", process.getProjectId());
                return;
            }

            // 4. 获取该过程类型配置的指标列表
            List<ProcessIndicatorConfigDO> configs = processIndicatorConfigService
                    .getConfigListByProcessTypeAndProjectType(process.getProcessType(), project.getProjectType());

            if (configs == null || configs.isEmpty()) {
                log.info("[syncIndicatorsOnArchive] 无指标配置，跳过同步: processId={}, processType={}, projectType={}",
                        processId, process.getProcessType(), project.getProjectType());
                return;
            }

            // 5. 获取指标元数据 Map (indicatorId -> DeclareIndicatorDO)
            Set<Long> indicatorIds = configs.stream().map(ProcessIndicatorConfigDO::getIndicatorId).collect(Collectors.toSet());
            Map<Long, DeclareIndicatorDO> indicatorMap = declareIndicatorService.getIndicatorMap(indicatorIds);

            // 6. 构建更新对象
            ProjectDO updateProject = new ProjectDO();
            updateProject.setId(project.getId());
            boolean hasProjectUpdate = false;

            // 业务类型: 直接使用 processType（1=建设过程, 2=半年报, 3=年度总结, 4=中期评估, 5=整改记录, 6=验收申请）
            int businessType = process.getProcessType();
            Long fillerId = event.getUserId() != null ? event.getUserId() : 0L;

            // 7. 遍历配置，同步每个指标
            for (ProcessIndicatorConfigDO config : configs) {
                String key = "indicator_" + config.getIndicatorId();
                Object value = indicatorValues.get(key);
                if (value == null) continue;

                DeclareIndicatorDO indicator = indicatorMap.get(config.getIndicatorId());
                if (indicator == null) continue;

                String indicatorCode = indicator.getIndicatorCode();

                // 更新项目表财务字段
                BigDecimal numValue = toBigDecimal(value);
                if (numValue != null) {
                    switch (indicatorCode) {
                        case "201":
                            updateProject.setTotalInvestment(numValue);
                            hasProjectUpdate = true;
                            break;
                        case "20101":
                            updateProject.setCentralFundArrive(numValue);
                            hasProjectUpdate = true;
                            break;
                        case "202":
                            updateProject.setAccumulatedInvestment(numValue);
                            hasProjectUpdate = true;
                            break;
                        case "20201":
                            updateProject.setCentralFundUsed(numValue);
                            hasProjectUpdate = true;
                            break;
                    }
                }

                // 同步全部指标值到 declare_indicator_value 表
                declareIndicatorValueService.saveIndicatorValue(
                        businessType,
                        project.getId(),  // businessId = projectId
                        config.getIndicatorId(),
                        indicatorCode,
                        indicator.getValueType(),
                        value,
                        fillerId
                );

                log.debug("[syncIndicatorsOnArchive] 同步指标: processId={}, indicatorCode={}, value={}",
                        processId, indicatorCode, value);
            }

            // 8. 更新项目表
            if (hasProjectUpdate) {
                projectMapper.updateById(updateProject);
                log.info("[syncIndicatorsOnArchive] 更新项目表财务字段: processId={}, projectId={}",
                        processId, project.getId());
            }

            log.info("[syncIndicatorsOnArchive] 归档同步完成: processId={}, projectId={}, configCount={}",
                    processId, project.getId(), configs.size());

        } catch (Exception e) {
            log.error("[syncIndicatorsOnArchive] 归档同步失败: processId={}", processId, e);
        }
    }

    /**
     * 解析 indicator_values JSON
     */
    private Map<String, Object> parseIndicatorValues(String indicatorValuesJson) {
        if (indicatorValuesJson == null || indicatorValuesJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtils.parseObject(indicatorValuesJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("[parseIndicatorValues] 解析失败: {}", indicatorValuesJson, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 转换为 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

}
