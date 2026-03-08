package cn.gemrun.base.module.bpm.framework.process.service.impl;

import cn.gemrun.base.module.bpm.api.BpmBusinessTypeApi;
import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.dal.mysql.process.BpmBusinessProcessMapper;
import cn.gemrun.base.module.bpm.enums.BpmActionDef;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.framework.process.service.BpmProcessService;
import cn.gemrun.base.module.bpm.service.definition.BpmModelService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.gemrun.base.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BPM 流程配置服务实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class BpmProcessServiceImpl implements BpmProcessService {

    @Resource
    private BpmBusinessTypeApi bpmBusinessTypeApi;

    @Resource
    private BpmBusinessProcessMapper businessProcessMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private HistoryService historyService;

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmProcessDefinitionService processDefinitionService;

    @Resource
    @Lazy
    private BpmModelService modelService;

    @Resource
    private BpmProcessInstanceEventPublisher eventPublisher;

    @Override
    public BpmBusinessTypeRespDTO getProcessConfig(String businessType) {
        Assert.notNull(businessType, "businessType 不能为空");
        BpmBusinessTypeGetReqDTO reqDTO = new BpmBusinessTypeGetReqDTO();
        reqDTO.setBusinessType(businessType);
        return bpmBusinessTypeApi.getProcessConfig(reqDTO);
    }

    @Override
    public boolean hasProcessConfig(String businessType) {
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        return config != null && config.getEnabled() == 1;
    }

    @Override
    public String getProcessDefinitionKey(String businessType) {
        return bpmBusinessTypeApi.getProcessDefinitionKey(businessType);
    }

    @Override
    @Transactional
    public String startProcessIfConfigured(String businessType, Long businessId, Long userId) {
        // 1. 查询流程配置（通过 API）
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        if (config == null || config.getEnabled() != 1) {
            log.info("未配置流程，跳过: businessType={}", businessType);
            return null;
        }

        // 2. 检查流程定义是否存在
        String processKey = config.getProcessDefinitionKey();
        org.flowable.engine.repository.ProcessDefinition processDefinition =
                processDefinitionService.getActiveProcessDefinition(processKey);
        if (processDefinition == null) {
            log.warn("流程定义不存在，跳过: processKey={}", processKey);
            return null;
        }

        // 3. 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("businessType", businessType);
        variables.put("businessId", businessId);
        variables.put("initiator", userId);

        // 4. 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processKey,
                businessType + ":" + businessId,  // businessKey
                variables
        );

        String processInstanceId = processInstance.getId();
        log.info("流程已启动: businessType={}, businessId={}, processInstanceId={}",
                businessType, businessId, processInstanceId);

        // 5. 获取第一个任务节点信息
        String firstNodeKey = getFirstTaskNodeKey(processInstanceId);
        log.info("第一个任务节点: processInstanceId={}, nodeKey={}", processInstanceId, firstNodeKey);

        // 6. 解析第一个节点的 assign 配置
        String assignType = null;
        String assignSource = null;
        if (firstNodeKey != null) {
            try {
                org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                        processInstance.getProcessDefinitionId());
                if (bpmnModel != null) {
                    FlowElement firstElement = bpmnModel.getFlowElement(firstNodeKey);
                    if (firstElement != null) {
                        String dslConfig = BpmnModelUtils.parseDslConfig(firstElement);
                        if (StrUtil.isNotBlank(dslConfig)) {
                            Map<String, String> assignInfo = parseAssignFromDsl(dslConfig);
                            assignType = assignInfo.get("type");
                            assignSource = assignInfo.get("source");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析第一个节点 assign 配置失败: {}", e.getMessage());
            }
        }

        // 7. 创建业务流程关联记录
        createBusinessProcess(businessType, businessId, processInstanceId, processKey, userId, firstNodeKey,
                assignType, assignSource);

        return processInstanceId;
    }

    @Override
    @Transactional
    public String startProcess(String businessType, Long businessId, Long userId) {
        BpmBusinessTypeRespDTO config = getProcessConfig(businessType);
        if (config == null || config.getEnabled() != 1) {
            throw new IllegalStateException("未配置流程: " + businessType);
        }
        return startProcessIfConfigured(businessType, businessId, userId);
    }

    @Override
    @Transactional
    public void updateProcessInstance(String processInstanceId, String currentNodeKey, String currentStatus) {
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null) {
            businessProcess.setCurrentNodeKey(currentNodeKey);
            businessProcess.setCurrentStatus(currentStatus);
            businessProcessMapper.updateById(businessProcess);
        }
    }

    @Override
    @Transactional
    public void endProcess(String processInstanceId, String result) {
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null) {
            businessProcess.setEndTime(LocalDateTime.now());
            businessProcess.setResult(result);
            businessProcessMapper.updateById(businessProcess);
        }
    }

    @Override
    public String getProcessInstanceId(String businessType, Long businessId) {
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess != null) {
            return businessProcess.getProcessInstanceId();
        }
        return null;
    }

    @Override
    public BpmBusinessProcessDO getBusinessProcess(String businessType, Long businessId) {
        return businessProcessMapper.selectByBusiness(businessType, businessId);
    }

    @Override
    public BpmBusinessProcessDO getBusinessProcessByProcessInstanceId(String processInstanceId) {
        return businessProcessMapper.selectByProcessInstanceId(processInstanceId);
    }

    @Override
    @Transactional
    public void updateCurrentAssign(String processInstanceId, String assignType, String assignSource) {
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null) {
            businessProcess.setCurrentAssignType(assignType);
            businessProcess.setCurrentAssignSource(assignSource);
            businessProcessMapper.updateById(businessProcess);
            log.info("更新任务分配信息: processInstanceId={}, assignType={}, assignSource={}",
                    processInstanceId, assignType, assignSource);
        }
    }

    @Override
    @Transactional
    public void addParticipant(String processInstanceId, Long userId) {
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);
        if (businessProcess != null && userId != null) {
            String currentIds = businessProcess.getInitiatorIds();
            boolean needUpdate = false;
            if (StrUtil.isBlank(currentIds)) {
                businessProcess.setInitiatorIds(String.valueOf(userId));
                needUpdate = true;
            } else {
                // 检查是否已存在
                List<String> idList = StrUtil.split(currentIds, ',');
                if (!idList.contains(String.valueOf(userId))) {
                    idList.add(String.valueOf(userId));
                    businessProcess.setInitiatorIds(StrUtil.join(",", idList));
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                // 只更新 initiatorIds，不影响其他字段
                businessProcessMapper.updateById(businessProcess);
                log.info("添加参与者: processInstanceId={}, userId={}", processInstanceId, userId);
            }
        }
    }

    @Override
    public List<BpmActionRespDTO> getAvailableActions(String businessType, Long businessId, Long userId) {
        log.info("[getAvailableActions] 开始查询: userId={}, businessType={}, businessId={}", userId, businessType, businessId);

        // 1. 查询业务流程关联
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess == null) {
            log.info("[getAvailableActions] 未找到业务流程记录: businessType={}, businessId={}", businessType, businessId);
            return Collections.emptyList();
        }
        if (!"STARTED".equals(businessProcess.getCurrentStatus())) {
            log.info("[getAvailableActions] 流程状态不是 STARTED, 当前状态={}, processInstanceId={}",
                    businessProcess.getCurrentStatus(), businessProcess.getProcessInstanceId());
            return Collections.emptyList();
        }

        // 2. 查询当前用户是否有待处理任务（assignee 或候选人）
        String processInstanceId = businessProcess.getProcessInstanceId();
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .list();
        log.info("[getAvailableActions] assignee查询结果: userId={}, processInstanceId={}, tasks.size={}",
                userId, processInstanceId, tasks.size());
        if (tasks.isEmpty()) {
            // 也尝试候选人查询
            tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskCandidateOrAssigned(String.valueOf(userId))
                    .list();
            log.info("[getAvailableActions] candidateOrAssigned查询结果: userId={}, tasks.size={}", userId, tasks.size());
        }
        if (tasks.isEmpty()) {
            log.info("[getAvailableActions] 当前用户 {} 无待处理任务: processInstanceId={}", userId, processInstanceId);
            return Collections.emptyList();
        }
        Task currentTask = tasks.get(0);

        // 3. 获取 BPMN 模型
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            log.info("[getAvailableActions] 流程实例不存在: processInstanceId={}", processInstanceId);
            return Collections.emptyList();
        }
        org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                processInstance.getProcessDefinitionId());
        if (bpmnModel == null) {
            return Collections.emptyList();
        }

        // 4. 获取当前节点的 DSL 配置
        // 直接从原始 BPMN XML 读取，避免 Flowable StAX 解析器对长文本的截断 bug
        String bpmnXml = modelService.getBpmnXmlByDefinitionId(processInstance.getProcessDefinitionId());
        String dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, currentTask.getTaskDefinitionKey());
        if (StrUtil.isBlank(dslConfig)) {
            // 兜底：从解析后的 BpmnModel 读取（短 DSL 场景）
            FlowElement flowElement = bpmnModel.getFlowElement(currentTask.getTaskDefinitionKey());
            if (flowElement != null) {
                dslConfig = BpmnModelUtils.parseDslConfig(flowElement);
            }
        }
        if (StrUtil.isBlank(dslConfig)) {
            log.info("[getAvailableActions] 节点无 DSL 配置: nodeKey={}", currentTask.getTaskDefinitionKey());
            return Collections.emptyList();
        }

        // 5. 解析 DSL actions
        log.info("[getAvailableActions] userId={}, businessType={}, businessId={}, taskId={}, nodeKey={}, dslConfig={}",
                userId, businessType, businessId, currentTask.getId(), currentTask.getTaskDefinitionKey(), dslConfig);
        try {
            JSONObject dslJson = JSONUtil.parseObj(dslConfig);
            JSONArray actionsJson = dslJson.getJSONArray("actions");
            if (actionsJson == null || actionsJson.isEmpty()) {
                return Collections.emptyList();
            }
            List<BpmActionRespDTO> result = new ArrayList<>();
            for (int i = 0; i < actionsJson.size(); i++) {
                JSONObject actionJson = actionsJson.getJSONObject(i);
                String key = actionJson.getStr("key");
                if (StrUtil.isBlank(key)) {
                    continue;
                }
                BpmActionRespDTO dto = new BpmActionRespDTO();
                dto.setKey(key);
                // 优先使用 DSL 中配置的 label，其次使用 BpmActionDef 枚举中定义的 label
                BpmActionDef actionDef = BpmActionDef.fromKey(key);
                dto.setLabel(actionJson.getStr("label",
                        actionDef != null ? actionDef.getLabel() : key));
                // 优先使用 DSL 中配置的 bizStatus，其次使用枚举中的默认值
                dto.setBizStatus(actionJson.getStr("bizStatus",
                        actionDef != null ? actionDef.getBizStatus() : null));
                // 优先使用 DSL 中配置的 bizStatusLabel，其次使用枚举中的默认值
                dto.setBizStatusLabel(actionJson.getStr("bizStatusLabel",
                        actionDef != null ? actionDef.getBizStatusLabel() : null));
                dto.setBpmAction(actionJson.getStr("bpmAction",
                        actionDef != null && actionDef.getBpmAction() != null
                                ? actionDef.getBpmAction().getValue() : null));
                // 附加 taskId，前端执行操作时需要
                dto.setTaskId(currentTask.getId());
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.warn("[getAvailableActions] 解析 DSL actions 失败: processInstanceId={}, nodeKey={}, error={}",
                    businessProcess.getProcessInstanceId(), currentTask.getTaskDefinitionKey(), e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<Long, List<BpmActionRespDTO>> getAvailableActionsBatch(String businessType, List<Long> businessIds, Long userId) {
        // 批量查询业务流程记录
        if (businessIds == null || businessIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BpmBusinessProcessDO> processes = businessProcessMapper.selectByBusinessIds(businessType, businessIds);
        if (processes.isEmpty()) {
            return Collections.emptyMap();
        }

        // 过滤出 STARTED 状态的流程
        List<BpmBusinessProcessDO> startedProcesses = processes.stream()
                .filter(p -> "STARTED".equals(p.getCurrentStatus()))
                .collect(java.util.stream.Collectors.toList());
        if (startedProcesses.isEmpty()) {
            return Collections.emptyMap();
        }

        // 收集所有流程实例 ID
        List<String> processInstanceIds = startedProcesses.stream()
                .map(BpmBusinessProcessDO::getProcessInstanceId)
                .collect(java.util.stream.Collectors.toList());

        // 查询当前用户的所有待办任务（按流程实例 ID 逐个查询，因为 Flowable 没有 in 查询）
        Map<String, List<Task>> tasksByProcessInstId = new java.util.HashMap<>();
        for (String processInstId : processInstanceIds) {
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstId)
                    .taskAssignee(String.valueOf(userId))
                    .list();
            if (tasks.isEmpty()) {
                tasks = taskService.createTaskQuery()
                        .processInstanceId(processInstId)
                        .taskCandidateOrAssigned(String.valueOf(userId))
                        .list();
            }
            if (!tasks.isEmpty()) {
                tasksByProcessInstId.put(processInstId, tasks);
            }
        }
        if (tasksByProcessInstId.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构建 businessId -> actions 的映射
        Map<Long, List<BpmActionRespDTO>> result = new java.util.HashMap<>();
        for (BpmBusinessProcessDO process : startedProcesses) {
            if (!tasksByProcessInstId.containsKey(process.getProcessInstanceId())) {
                continue;
            }
            // 对每个有任务的 businessId，调用单个查询方法获取 actions
            List<BpmActionRespDTO> actions = getAvailableActions(businessType, process.getBusinessId(), userId);
            result.put(process.getBusinessId(), actions);
        }
        return result;
    }

    @Override
    public String parseBusinessType(String methodName, String className) {
        // 解析模块名：取 .module. 之后的第一部分，如 cn.gemrun.base.module.declare.xxx → declare
        String moduleName;
        int moduleIdx = className.indexOf(".module.");
        if (moduleIdx >= 0) {
            String afterModule = className.substring(moduleIdx + ".module.".length());
            moduleName = afterModule.split("\\.")[0];
        } else {
            String[] packageParts = className.split("\\.");
            moduleName = packageParts.length > 1 ? packageParts[1] : "default";
        }

        // 解析业务名：取类名，去掉 Controller 后缀
        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
        if (simpleClassName.endsWith("Controller")) {
            simpleClassName = simpleClassName.substring(0, simpleClassName.length() - 10);
        } else if (simpleClassName.endsWith("DO")) {
            simpleClassName = simpleClassName.substring(0, simpleClassName.length() - 2);
        }
        String tableName = StrUtil.lowerFirst(simpleClassName);

        // 解析动作名
        String actionName = methodName;
        if (actionName.startsWith("submit")) {
            actionName = "submit";
        } else if (actionName.startsWith("start")) {
            actionName = "start";
        } else if (actionName.startsWith("create")) {
            actionName = "create";
        } else if (actionName.startsWith("update")) {
            actionName = "update";
        } else if (actionName.startsWith("delete")) {
            actionName = "delete";
        } else if (actionName.startsWith("audit")) {
            actionName = "audit";
        } else if (actionName.startsWith("approve")) {
            actionName = "approve";
        } else if (actionName.startsWith("reject")) {
            actionName = "reject";
        } else if (actionName.startsWith("withdraw")) {
            actionName = "withdraw";
        } else if (actionName.startsWith("resubmit")) {
            actionName = "resubmit";
        }

        return moduleName + ":" + tableName + ":" + actionName;
    }

    @Override
    @Transactional
    public void submitAction(String businessType, Long businessId, String actionKey, Long userId, String reason) {
        log.info("[submitAction] 开始提交操作: userId={}, businessType={}, businessId={}, actionKey={}",
                userId, businessType, businessId, actionKey);

        // 1. 查询业务流程关联记录
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess == null) {
            throw new IllegalArgumentException("未找到业务流程记录: businessType=" + businessType + ", businessId=" + businessId);
        }

        String processInstanceId = businessProcess.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new IllegalStateException("流程实例ID为空");
        }

        // 2. 查询当前用户的待处理任务
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .list();
        if (tasks.isEmpty()) {
            // 也尝试候选人查询
            tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskCandidateOrAssigned(String.valueOf(userId))
                    .list();
        }
        if (tasks.isEmpty()) {
            throw new IllegalStateException("当前用户没有待处理任务");
        }
        Task currentTask = tasks.get(0);

        // 3. 获取当前节点的 DSL 配置，解析出 actionKey 对应的 bizStatus 和 bizStatusLabel
        String currentNodeKey = currentTask.getTaskDefinitionKey();
        Map<String, String> actionBizStatus = getActionBizStatus(processInstanceId, currentNodeKey, actionKey);
        String bizStatus = actionBizStatus.get("bizStatus");
        String bizStatusLabel = actionBizStatus.get("bizStatusLabel");

        // 4. 完成任务（Flowable）
        // 设置审批意见到流程变量
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        if (StrUtil.isNotBlank(reason)) {
            variables.put("reason", reason);
        }
        taskService.complete(currentTask.getId(), variables);

        // 5. 更新业务流程关联记录
        // 获取下一个任务节点信息
        String nextNodeKey = getNextTaskNodeKey(processInstanceId);
        if (StrUtil.isNotBlank(nextNodeKey)) {
            businessProcess.setCurrentNodeKey(nextNodeKey);
        }
        // 更新参与者
        addParticipant(processInstanceId, userId);
        // 获取下一个节点的分配信息并更新
        Map<String, String> nextAssignConfig = getNextNodeAssignConfig(processInstanceId, nextNodeKey);
        if (nextAssignConfig != null) {
            businessProcess.setCurrentAssignType(nextAssignConfig.get("type"));
            businessProcess.setCurrentAssignSource(nextAssignConfig.get("source"));
            log.info("[submitAction] 更新分配信息: assignType={}, assignSource={}",
                    nextAssignConfig.get("type"), nextAssignConfig.get("source"));
        }
        // 更新流程状态
        businessProcess.setCurrentStatus("STARTED");
        businessProcessMapper.updateById(businessProcess);

        // 6. 检查流程是否结束，发布事件
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            // 流程已结束
            businessProcess.setCurrentStatus("COMPLETED");
            businessProcessMapper.updateById(businessProcess);
            // 发布流程结束事件（传递 bizStatus）
            publishProcessEvent(businessProcess, 2, bizStatus, bizStatusLabel, actionKey);
        } else {
            // 流程仍在进行，检查是否到达下一个任务
            List<Task> nextTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            if (nextTasks.isEmpty()) {
                // 没有待办任务，可能是结束节点或自动节点
                businessProcess.setCurrentStatus("COMPLETED");
                businessProcessMapper.updateById(businessProcess);
                publishProcessEvent(businessProcess, 2, bizStatus, bizStatusLabel, actionKey);
            } else {
                // 流程继续，发布任务完成事件（传递 bizStatus）
                publishProcessEvent(businessProcess, 1, bizStatus, bizStatusLabel, actionKey);
            }
        }

        log.info("[submitAction] 提交操作完成: userId={}, businessType={}, businessId={}, actionKey={}",
                userId, businessType, businessId, actionKey);
    }

    /**
     * 从当前节点的 DSL 配置中获取指定 actionKey 对应的 bizStatus 和 bizStatusLabel
     */
    private Map<String, String> getActionBizStatus(String processInstanceId, String nodeKey, String actionKey) {
        Map<String, String> result = new java.util.HashMap<>();
        try {
            // 获取流程定义
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (processInstance == null) {
                return result;
            }

            // 从 BPMN XML 中读取 DSL 配置
            String bpmnXml = modelService.getBpmnXmlByDefinitionId(processInstance.getProcessDefinitionId());
            String dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, nodeKey);
            if (StrUtil.isBlank(dslConfig)) {
                return result;
            }

            // 解析 actions 数组，找到对应的 action
            JSONObject dslJson = JSONUtil.parseObj(dslConfig);
            JSONArray actionsJson = dslJson.getJSONArray("actions");
            if (actionsJson == null || actionsJson.isEmpty()) {
                return result;
            }

            for (int i = 0; i < actionsJson.size(); i++) {
                JSONObject actionJson = actionsJson.getJSONObject(i);
                if (actionKey.equals(actionJson.getStr("key"))) {
                    result.put("bizStatus", actionJson.getStr("bizStatus"));
                    result.put("bizStatusLabel", actionJson.getStr("bizStatusLabel"));
                    log.info("[getActionBizStatus] 找到 actionKey={} 的业务状态配置: bizStatus={}, bizStatusLabel={}",
                            actionKey, result.get("bizStatus"), result.get("bizStatusLabel"));
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("[getActionBizStatus] 解析 DSL 配置失败: processInstanceId={}, nodeKey={}, actionKey={}, error={}",
                    processInstanceId, nodeKey, actionKey, e.getMessage());
        }
        return result;
    }

    /**
     * 获取下一个任务节点Key
     */
    private String getNextTaskNodeKey(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime().asc()
                .list();
        if (tasks != null && !tasks.isEmpty()) {
            return tasks.get(0).getTaskDefinitionKey();
        }
        return null;
    }

    /**
     * 发布流程事件（带业务状态信息）
     */
    private void publishProcessEvent(BpmBusinessProcessDO businessProcess, Integer status,
                                      String bizStatus, String bizStatusLabel, String actionKey) {
        try {
            BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(this);
            event.setId(businessProcess.getProcessInstanceId());
            event.setProcessDefinitionKey(businessProcess.getProcessDefinitionKey());
            event.setStatus(status);
            event.setBusinessKey(businessProcess.getBusinessType() + ":" + businessProcess.getBusinessId());
            event.setBizStatus(bizStatus);
            event.setBizStatusLabel(bizStatusLabel);
            event.setActionKey(actionKey);
            eventPublisher.sendProcessInstanceResultEvent(event);
            log.info("[publishProcessEvent] 发布事件: processInstanceId={}, status={}, businessKey={}, bizStatus={}, bizStatusLabel={}, actionKey={}",
                    event.getId(), status, event.getBusinessKey(), bizStatus, bizStatusLabel, actionKey);
        } catch (Exception e) {
            log.error("[publishProcessEvent] 发布事件失败: processInstanceId={}, error={}",
                    businessProcess.getProcessInstanceId(), e.getMessage(), e);
        }
    }

    /**
     * 创建业务流程关联记录
     */
    private void createBusinessProcess(String businessType, Long businessId,
                                        String processInstanceId, String processDefinitionKey,
                                        Long userId, String firstNodeKey,
                                        String assignType, String assignSource) {
        BpmBusinessProcessDO businessProcess = new BpmBusinessProcessDO();
        businessProcess.setBusinessType(businessType);
        businessProcess.setBusinessId(businessId);
        businessProcess.setProcessInstanceId(processInstanceId);
        businessProcess.setProcessDefinitionKey(processDefinitionKey);
        businessProcess.setCurrentNodeKey(firstNodeKey);
        businessProcess.setCurrentAssignType(assignType);
        businessProcess.setCurrentAssignSource(assignSource);
        businessProcess.setInitiatorId(userId);
        // 初始化参与者列表，包含发起人
        businessProcess.setInitiatorIds(String.valueOf(userId));
        businessProcess.setStartTime(LocalDateTime.now());
        businessProcess.setCurrentStatus("STARTED");
        businessProcessMapper.insert(businessProcess);
    }

    /**
     * 获取流程的第一个任务节点Key
     * 启动流程后，查询当前正在等待的任务
     */
    private String getFirstTaskNodeKey(String processInstanceId) {
        // 查询当前流程实例中正在等待的任务
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime().asc()
                .list();

        if (tasks != null && !tasks.isEmpty()) {
            Task firstTask = tasks.get(0);
            log.info("获取到第一个任务: taskId={}, taskName={}, taskDefKey={}", 
                    firstTask.getId(), firstTask.getName(), firstTask.getTaskDefinitionKey());
            return firstTask.getTaskDefinitionKey();
        }

        log.warn("流程启动后未找到任务节点: processInstanceId={}", processInstanceId);
        return null;
    }

    /**
     * 根据流程实例ID获取下一个节点的 DSL 配置
     * 用于在任务完成后更新 current_assign_type/source
     *
     * @param processInstanceId 流程实例ID
     * @param nextNodeKey 下一个任务节点Key（可选）
     * @return DSL 配置中的 assign 信息，包含 type 和 source
     */
    public Map<String, String> getNextNodeAssignConfig(String processInstanceId, String nextNodeKey) {
        try {
            // 1. 获取流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                log.warn("[getNextNodeAssignConfig] 流程实例不存在: {}", processInstanceId);
                return null;
            }

            // 2. 获取 BPMN 模型
            org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                    processInstance.getProcessDefinitionId());
            if (bpmnModel == null) {
                log.warn("[getNextNodeAssignConfig] BPMN模型不存在: {}", processInstance.getProcessDefinitionId());
                return null;
            }

            // 3. 如果已知下一个节点Key，直接获取该节点
            UserTask nextUserTask = null;
            if (StrUtil.isNotBlank(nextNodeKey)) {
                FlowElement element = bpmnModel.getFlowElement(nextNodeKey);
                if (element instanceof UserTask) {
                    nextUserTask = (UserTask) element;
                }
            }

            // 如果没找到下一个节点，尝试通过当前活动ID查找
            if (nextUserTask == null) {
                String currentActivityId = processInstance.getActivityId();
                if (currentActivityId != null) {
                    FlowElement currentElement = bpmnModel.getFlowElement(currentActivityId);
                    if (currentElement != null) {
                        nextUserTask = findNextUserTask(bpmnModel, currentElement);
                    }
                }
            }

            // 如果还是没有，查询下一个待办任务
            if (nextUserTask == null) {
                List<Task> nextTasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .orderByTaskCreateTime().asc()
                        .list();
                if (nextTasks != null && !nextTasks.isEmpty()) {
                    String taskDefKey = nextTasks.get(0).getTaskDefinitionKey();
                    FlowElement element = bpmnModel.getFlowElement(taskDefKey);
                    if (element instanceof UserTask) {
                        nextUserTask = (UserTask) element;
                    }
                }
            }

            if (nextUserTask == null) {
                log.info("[getNextNodeAssignConfig] 没有找到下一个用户任务: {}", processInstanceId);
                return null;
            }

            // 4. 解析 DSL 配置
            String dslConfig = BpmnModelUtils.parseDslConfig(nextUserTask);
            if (StrUtil.isBlank(dslConfig)) {
                log.info("[getNextNodeAssignConfig] 下一个节点无DSL配置: {}", nextUserTask.getId());
                return null;
            }

            // 5. 解析 assign 信息（简单解析）
            Map<String, String> assignInfo = parseAssignFromDsl(dslConfig);
            log.info("[getNextNodeAssignConfig] 解析assign信息: {}", assignInfo);
            return assignInfo;
        } catch (Exception e) {
            log.error("[getNextNodeAssignConfig] 解析失败: {}", processInstanceId, e);
            return null;
        }
    }

    /**
     * 查找下一个用户任务节点
     */
    private UserTask findNextUserTask(org.flowable.bpmn.model.BpmnModel bpmnModel, FlowElement currentElement) {
        // 简化实现：遍历所有用户任务，假设按顺序执行
        // 实际应该根据流程图解析输出顺序流
        List<UserTask> allUserTasks = BpmnModelUtils.getBpmnModelElements(bpmnModel, UserTask.class);
        boolean found = false;
        for (UserTask userTask : allUserTasks) {
            if (found) {
                return userTask;
            }
            if (userTask.getId().equals(currentElement.getId())) {
                found = true;
            }
        }
        return null;
    }

    /**
     * 从 DSL 配置中解析 assign 信息
     */
    private Map<String, String> parseAssignFromDsl(String dslConfig) {
        Map<String, String> result = new HashMap<>();
        try {
            // 简单字符串解析，实际应使用 JSON 解析
            if (dslConfig.contains("\"assign\"")) {
                int assignStart = dslConfig.indexOf("\"assign\"");
                int braceStart = dslConfig.indexOf("{", assignStart);
                int braceEnd = dslConfig.indexOf("}", braceStart);
                if (braceStart >= 0 && braceEnd >= 0) {
                    String assignStr = dslConfig.substring(braceStart + 1, braceEnd);
                    // 解析 type
                    if (assignStr.contains("\"type\"")) {
                        int typeStart = assignStr.indexOf("\"type\"");
                        int typeValueStart = assignStr.indexOf("\"", typeStart + 6);
                        int typeValueEnd = assignStr.indexOf("\"", typeValueStart + 1);
                        if (typeValueStart >= 0 && typeValueEnd >= 0) {
                            result.put("type", assignStr.substring(typeValueStart + 1, typeValueEnd));
                        }
                    }
                    // 解析 source
                    if (assignStr.contains("\"source\"")) {
                        int sourceStart = assignStr.indexOf("\"source\"");
                        int sourceValueStart = assignStr.indexOf("\"", sourceStart + 8);
                        int sourceValueEnd = assignStr.indexOf("\"", sourceValueStart + 1);
                        if (sourceValueStart >= 0 && sourceValueEnd >= 0) {
                            result.put("source", assignStr.substring(sourceValueStart + 1, sourceValueEnd));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[parseAssignFromDsl] 解析失败", e);
        }
        return result;
    }

    @Override
    @Transactional
    public void withdrawProcess(String businessType, Long businessId, Long userId) {
        log.info("[withdrawProcess] 开始撤回流程: userId={}, businessType={}, businessId={}",
                userId, businessType, businessId);

        // 1. 查询业务流程关联记录
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess == null) {
            throw new IllegalArgumentException("未找到业务流程记录: businessType=" + businessType + ", businessId=" + businessId);
        }

        String processInstanceId = businessProcess.getProcessInstanceId();
        if (StrUtil.isBlank(processInstanceId)) {
            throw new IllegalStateException("流程实例ID为空，无法撤回");
        }

        // 2. 验证是否是发起人本人撤回（或者管理员）
        if (!String.valueOf(userId).equals(String.valueOf(businessProcess.getInitiatorId()))) {
            // TODO: 这里可以扩展为管理员也能撤回
            throw new IllegalStateException("只有发起人本人才能撤回流程");
        }

        // 3. 找到当前用户在该流程中的最近一次已办任务，作为“撤回目标节点”
        HistoricTaskInstance lastTask = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(0, 1)
                .stream().findFirst().orElse(null);
        if (lastTask == null) {
            throw new IllegalStateException("当前用户在该流程中没有可撤回的任务");
        }
        log.info("[withdrawProcess] 找到可撤回任务: processInstanceId={}, taskId={}, taskDefKey={}",
                processInstanceId, lastTask.getId(), lastTask.getTaskDefinitionKey());

        // 4. 调用通用任务撤回逻辑，将流程回退到该任务节点
        bpmTaskService.withdrawTask(userId, lastTask.getId());

        // 5. 更新业务流程记录：当前节点回到 lastTask 对应的节点，状态保持 STARTED
        String currentNodeKey = lastTask.getTaskDefinitionKey();
        businessProcess.setCurrentNodeKey(currentNodeKey);
        businessProcess.setCurrentStatus("STARTED");

        // 重新解析当前节点的 assign 配置，刷新 current_assign_type/source
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (processInstance != null) {
                org.flowable.bpmn.model.BpmnModel bpmnModel =
                        modelService.getBpmnModelByDefinitionId(processInstance.getProcessDefinitionId());
                if (bpmnModel != null) {
                    FlowElement element = bpmnModel.getFlowElement(currentNodeKey);
                    if (element != null) {
                        String dslConfig = BpmnModelUtils.parseDslConfig(element);
                        if (StrUtil.isNotBlank(dslConfig)) {
                            Map<String, String> assignInfo = parseAssignFromDsl(dslConfig);
                            businessProcess.setCurrentAssignType(assignInfo.get("type"));
                            businessProcess.setCurrentAssignSource(assignInfo.get("source"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[withdrawProcess] 解析撤回后节点 assign 配置失败: processInstanceId={}, nodeKey={}, error={}",
                    processInstanceId, currentNodeKey, e.getMessage());
        }

        // 撤回不认为流程结束，不设置 endTime / result
        businessProcessMapper.updateById(businessProcess);
        log.info("[withdrawProcess] 流程撤回完成: businessType={}, businessId={}, processInstanceId={}, currentNodeKey={}",
                businessType, businessId, processInstanceId, currentNodeKey);
    }

}
