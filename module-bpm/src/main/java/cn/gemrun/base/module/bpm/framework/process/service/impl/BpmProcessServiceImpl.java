package cn.gemrun.base.module.bpm.framework.process.service.impl;

import cn.gemrun.base.module.bpm.api.BpmBusinessTypeApi;
import cn.gemrun.base.module.bpm.api.dto.BpmActionRespDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeGetReqDTO;
import cn.gemrun.base.module.bpm.api.dto.BpmBusinessTypeRespDTO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.gemrun.base.module.bpm.dal.mysql.process.BpmBusinessProcessMapper;
import cn.gemrun.base.module.bpm.enums.BpmActionDef;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
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

        // 打印获取到的流程定义详细信息
        log.info("[startProcessIfConfigured] ====== 流程定义详细信息 ======");
        log.info("[startProcessIfConfigured] processDefinitionId={}, processDefinitionKey={}, version={}, deploymentId={}, name={}",
                processDefinition.getId(), processDefinition.getKey(),
                processDefinition.getVersion(), processDefinition.getDeploymentId(),
                processDefinition.getName());

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
        log.info("流程已启动: businessType={}, businessId={}, processInstanceId={}", businessType, businessId, processInstanceId);
        // 打印流程实例关联的流程定义信息
        log.info("[startProcessIfConfigured] ====== 流程实例详细信息 ======");
        log.info("[startProcessIfConfigured] processInstanceId={}, processDefinitionId={}, processDefinitionKey={}",
                processInstance.getId(), processInstance.getProcessDefinitionId(), processInstance.getProcessDefinitionKey());
        // 重点：对比 processDefinition.getId() 和 processInstance.getProcessDefinitionId() 是否一致
        if (!processDefinition.getId().equals(processInstance.getProcessDefinitionId())) {
            log.error("[startProcessIfConfigured] ⚠️ 版本不一致问题！processDefinition.getId()={}, processInstance.getProcessDefinitionId()={}",
                    processDefinition.getId(), processInstance.getProcessDefinitionId());
        } else {
            log.info("[startProcessIfConfigured] ✓ 版本一致");
        }

        // 5. 获取第一个任务节点信息
        String firstNodeKey = getFirstTaskNodeKey(processInstanceId);
        log.info("第一个任务节点: processInstanceId={}, nodeKey={}", processInstanceId, firstNodeKey);

        // 6. 解析第一个节点的 assign 配置
        String assignType = null;
        String assignSource = null;
        String firstNodeDslJson = null;
        if (firstNodeKey != null) {
            // 直接从已部署流程定义的 XML 获取完整 DSL
            String dslConfig = null;
            try {
                String bpmnXml = modelService.getBpmnXmlByDefinitionId(processInstance.getProcessDefinitionId());
                log.info("[startProcessIfConfigured] ====== 开始解析 DSL ====== processDefinitionId={}, nodeKey={}, bpmnXml长度={}",
                        processInstance.getProcessDefinitionId(), firstNodeKey,
                        bpmnXml != null ? bpmnXml.length() : 0);

                // 打印 BPMN XML 中所有的 userTask 节点 ID
                if (StrUtil.isNotBlank(bpmnXml)) {
                    log.info("[startProcessIfConfigured] ====== BPMN XML 中所有 userTask 节点 ======");
                    // 使用正则表达式提取所有 userTask 的 id
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<userTask[^>]*id=\"([^\"]+)\"[^>]*>");
                    java.util.regex.Matcher matcher = pattern.matcher(bpmnXml);
                    java.util.List<String> userTaskIds = new java.util.ArrayList<>();
                    while (matcher.find()) {
                        userTaskIds.add(matcher.group(1));
                    }
                    log.info("[startProcessIfConfigured] 找到 {} 个 userTask 节点: {}", userTaskIds.size(), userTaskIds);
                    log.info("[startProcessIfConfigured] ====== BPMN XML 节点列表结束 ======");

                    dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, firstNodeKey);
                    log.info("[startProcessIfConfigured] 从已部署流程定义获取 DSL, processDefinitionId={}, nodeKey={}, dsl长度={}",
                            processInstance.getProcessDefinitionId(), firstNodeKey,
                            dslConfig != null ? dslConfig.length() : 0);
                    // 打印完整的 DSL 内容
                    if (dslConfig != null) {
                        log.info("[startProcessIfConfigured] DSL 完整内容 START:\n{}\nDSL 完整内容 END", dslConfig);
                    }
                }
            } catch (Exception e) {
                log.warn("[startProcessIfConfigured] 获取已部署流程 XML 失败: {}", e.getMessage(), e);
            }
            // 兜底：从 BpmnModel 获取
            if (StrUtil.isBlank(dslConfig)) {
                try {
                    org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                            processInstance.getProcessDefinitionId());
                    if (bpmnModel != null) {
                        FlowElement firstElement = bpmnModel.getFlowElement(firstNodeKey);
                        if (firstElement != null) {
                            dslConfig = BpmnModelUtils.parseDslConfig(firstElement);
                            log.info("[startProcessIfConfigured] 兜底从 BpmnModel 获取 DSL, nodeKey={}, dsl长度={}",
                                    firstNodeKey, dslConfig != null ? dslConfig.length() : 0);
                            // 打印完整的 DSL 内容
                            if (dslConfig != null) {
                                log.info("[startProcessIfConfigured] 兜底 DSL 完整内容 START:\n{}\nDSL 完整内容 END", dslConfig);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[startProcessIfConfigured] 获取 BpmnModel 失败: {}", e.getMessage(), e);
                }
            }
            log.info("[startProcessIfConfigured] 解析 DSL 结果: dslConfig={}",
                    dslConfig != null ? dslConfig.substring(0, Math.min(200, dslConfig.length())) + "..." : "null");
            // 仅保存合法 JSON（以 { 开头），否则 MySQL JSON 列报错
            firstNodeDslJson = isValidDslJson(dslConfig) ? dslConfig : null;
            log.info("[startProcessIfConfigured] DSL 校验结果: isValid={}, firstNodeDslJson长度={}",
                    isValidDslJson(dslConfig), firstNodeDslJson != null ? firstNodeDslJson.length() : 0);
            if (isValidDslJson(dslConfig)) {
                Map<String, String> assignInfo = parseAssignFromDsl(dslConfig);
                assignType = assignInfo.get("type");
                assignSource = assignInfo.get("source");
                log.info("[startProcessIfConfigured] 解析 assign 信息: type={}, source={}", assignType, assignSource);
            }
        }

        // 7. 创建业务流程关联记录
        createBusinessProcess(businessType, businessId, processInstanceId, processKey, userId, firstNodeKey,
                assignType, assignSource, firstNodeDslJson);

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
        log.info("[getAvailableActions] ====== 开始查询可用操作 ====== userId={}, businessType={}, businessId={}", userId, businessType, businessId);

        // 1. 查询业务流程关联
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByBusiness(businessType, businessId);
        if (businessProcess == null) {
            log.info("[getAvailableActions] 未找到业务流程记录: businessType={}, businessId={}", businessType, businessId);
            return Collections.emptyList();
        }

        // 打印业务表中的关键信息
        log.info("[getAvailableActions] 业务表记录信息: id={}, processInstanceId={}, processDefinitionKey={}, currentNodeKey={}, currentStatus={}",
                businessProcess.getId(), businessProcess.getProcessInstanceId(),
                businessProcess.getProcessDefinitionKey(), businessProcess.getCurrentNodeKey(),
                businessProcess.getCurrentStatus());
        log.info("[getAvailableActions] 业务表中存储的 DSL: dslJson长度={}",
                businessProcess.getDslJson() != null ? businessProcess.getDslJson().length() : 0);
        if (businessProcess.getDslJson() != null) {
            log.info("[getAvailableActions] 业务表中存储的 DSL 内容 START:\n{}\nDSL 内容 END", businessProcess.getDslJson());
        }

        // 2. 查询当前用户是否有待处理任务（assignee 或候选人）
        // 检查流程状态是否为进行中（STARTED 或 SUBMITTED 等）
        String currentStatus = businessProcess.getCurrentStatus();
        if (!isProcessRunning(currentStatus)) {
            log.info("[getAvailableActions] 流程状态不是进行中, 当前状态={}, processInstanceId={}",
                    currentStatus, businessProcess.getProcessInstanceId());
            return Collections.emptyList();
        }

        String processInstanceId = businessProcess.getProcessInstanceId();
        String currentNodeKey = businessProcess.getCurrentNodeKey();

        // 查询当前节点的任务
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(currentNodeKey)
                .taskAssignee(String.valueOf(userId))
                .list();
        log.info("[getAvailableActions] assignee查询结果: userId={}, processInstanceId={}, currentNodeKey={}, tasks.size={}",
                userId, processInstanceId, currentNodeKey, tasks.size());
        if (tasks.isEmpty()) {
            // 也尝试候选人查询
            tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(currentNodeKey)
                    .taskCandidateOrAssigned(String.valueOf(userId))
                    .list();
            log.info("[getAvailableActions] candidateOrAssigned查询结果: userId={}, currentNodeKey={}, tasks.size={}",
                    userId, currentNodeKey, tasks.size());
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
        log.info("[getAvailableActions] ====== 流程实例详细信息 ======");
        log.info("[getAvailableActions] processInstanceId={}, processDefinitionId={}, processDefinitionKey={}",
                processInstance.getId(), processInstance.getProcessDefinitionId(),
                processInstance.getProcessDefinitionKey());

        org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                processInstance.getProcessDefinitionId());
        if (bpmnModel == null) {
            log.warn("[getAvailableActions] 获取 BpmnModel 失败: processDefinitionId={}", processInstance.getProcessDefinitionId());
            return Collections.emptyList();
        }
        log.info("[getAvailableActions] BpmnModel 获取成功, processDefinitionId={}", processInstance.getProcessDefinitionId());

        // 4. 获取当前节点的 DSL 配置
        // 优先从业务表存储的完整 DSL 获取，避免模型 XML 解析截断问题
        // 注意：dsl_json 字段存储的是当前节点(current_node_key)的 DSL 配置
        String dslConfig = businessProcess.getDslJson();
        String dslSource = "business_table";  // DSL 来源标识
        if (StrUtil.isNotBlank(dslConfig)) {
            log.info("[getAvailableActions] 从业务表 dsl_json 获取 DSL, nodeKey={}, dsl长度={}",
                    currentNodeKey, dslConfig.length());
            log.info("[getAvailableActions] 业务表 DSL 完整内容 START:\n{}\nDSL 完整内容 END", dslConfig);
        }
        // 兜底：从已部署流程定义的 XML 获取（而不是从模型获取，模型可能已被修改）
        if (StrUtil.isBlank(dslConfig)) {
            dslSource = "bpmn_xml";
            try {
                String bpmnXml = modelService.getBpmnXmlByDefinitionId(processInstance.getProcessDefinitionId());
                log.info("[getAvailableActions] 从已部署流程定义获取 BPMN XML, processDefinitionId={}, bpmnXml长度={}",
                        processInstance.getProcessDefinitionId(), bpmnXml != null ? bpmnXml.length() : 0);
                if (StrUtil.isNotBlank(bpmnXml)) {
                    dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, currentTask.getTaskDefinitionKey());
                    log.info("[getAvailableActions] 从已部署流程定义获取 DSL, processDefinitionId={}, nodeKey={}, dsl长度={}",
                            processInstance.getProcessDefinitionId(), currentTask.getTaskDefinitionKey(),
                            dslConfig != null ? dslConfig.length() : 0);
                    if (dslConfig != null) {
                        log.info("[getAvailableActions] BPMN XML DSL 完整内容 START:\n{}\nDSL 完整内容 END", dslConfig);
                    }
                }
            } catch (Exception e) {
                log.warn("[getAvailableActions] 获取已部署流程 XML 失败: {}", e.getMessage(), e);
            }
            // 再次兜底：从已部署的 BpmnModel 获取（短 DSL 场景）
            if (StrUtil.isBlank(dslConfig)) {
                dslSource = "bpmn_model";
                FlowElement flowElement = bpmnModel.getFlowElement(currentTask.getTaskDefinitionKey());
                if (flowElement != null) {
                    dslConfig = BpmnModelUtils.parseDslConfig(flowElement);
                    log.info("[getAvailableActions] 从 BpmnModel 获取 DSL, nodeKey={}, dsl长度={}",
                            currentTask.getTaskDefinitionKey(), dslConfig != null ? dslConfig.length() : 0);
                    if (dslConfig != null) {
                        log.info("[getAvailableActions] BpmnModel DSL 完整内容 START:\n{}\nDSL 完整内容 END", dslConfig);
                    }
                }
            }
        }
        log.info("[getAvailableActions] 最终使用的 DSL 来源: {}, currentNodeKey={}, dsl长度={}",
                dslSource, currentTask.getTaskDefinitionKey(), dslConfig != null ? dslConfig.length() : 0);

        if (StrUtil.isBlank(dslConfig)) {
            log.info("[getAvailableActions] 节点无 DSL 配置: nodeKey={}", currentTask.getTaskDefinitionKey());
            return Collections.emptyList();
        }

        // 5. 解析 DSL actions
        log.info("[getAvailableActions] ====== 解析 DSL actions ======");
        log.info("[getAvailableActions] userId={}, businessType={}, businessId={}, taskId={}, nodeKey={}",
                userId, businessType, businessId, currentTask.getId(), currentTask.getTaskDefinitionKey());
        log.info("[getAvailableActions] 即将解析的 DSL 内容 START:\n{}\nDSL 内容 END", dslConfig);
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
                // 从 action.vars 中读取参数（专家数量、可修改字段等）
                JSONObject varsJson = actionJson.getJSONObject("vars");
                if (varsJson != null && !varsJson.isEmpty()) {
                    Map<String, Object> varsMap = new HashMap<>();
                    varsJson.forEach((k, v) -> varsMap.put(k, v));
                    dto.setVars(varsMap);
                }
                // 附加 taskId，前端执行操作时需要
                    dto.setTaskId(currentTask.getId());
                result.add(dto);
            }
            log.info("[getAvailableActions] 返回 actions: size={}, details={}", result.size(), result);
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

        // 过滤出进行中状态的流程
        List<BpmBusinessProcessDO> startedProcesses = processes.stream()
                .filter(p -> isProcessRunning(p.getCurrentStatus()))
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
    public void submitAction(String businessType, Long businessId, String actionKey, Long userId, String reason, List<Long> expertUserIds) {
        log.info("[submitAction] 开始提交操作: userId={}, businessType={}, businessId={}, actionKey={}, expertUserIds={}",
                userId, businessType, businessId, actionKey, expertUserIds);

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
            variables.put(BpmnVariableConstants.TASK_VARIABLE_REASON, reason);
        }

        // 4.1 处理选择专家操作 - 将选中的专家用户ID存入流程变量
        if ("selectExpert".equals(actionKey) && expertUserIds != null && !expertUserIds.isEmpty()) {
            // 获取下一个节点ID
            String nextNodeKeyForExpert = getNextTaskNodeKey(processInstanceId);
            if (StrUtil.isNotBlank(nextNodeKeyForExpert)) {
                // 设置审批人自选变量 APPROVE_USER_SELECT_ASSIGNEES
                Map<String, List<Long>> approveUserSelectAssignees = new HashMap<>();
                approveUserSelectAssignees.put(nextNodeKeyForExpert, expertUserIds);
                variables.put("PROCESS_APPROVE_USER_SELECT_ASSIGNEES", approveUserSelectAssignees);
                log.info("[submitAction] 设置选择专家变量: nextNodeKey={}, expertUserIds={}",
                        nextNodeKeyForExpert, expertUserIds);
            }
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
        // 获取下一个节点的分配信息并更新 DSL JSON
        updateNextNodeInfo(businessProcess, processInstanceId, nextNodeKey);
        // 更新流程状态为当前提交的节点对应的业务状态
        businessProcess.setCurrentStatus(bizStatus);
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
     * 获取下一个节点的分配信息并更新业务进程记录
     *
     * @param businessProcess 业务进程记录
     * @param processInstanceId 流程实例ID
     * @param nextNodeKey 下一个节点Key（可为空，为空时从运行时任务列表中推断）
     */
    private void updateNextNodeInfo(BpmBusinessProcessDO businessProcess, String processInstanceId, String nextNodeKey) {
        if (businessProcess == null || StrUtil.isBlank(processInstanceId)) {
            return;
        }
        log.info("[updateNextNodeInfo] ====== 更新下一个节点信息 ======");
        log.info("[updateNextNodeInfo] processInstanceId={}, nextNodeKey={}", processInstanceId, nextNodeKey);
        log.info("[updateNextNodeInfo] 业务表原始信息: id={}, currentNodeKey={}, currentStatus={}, processDefinitionKey={}",
                businessProcess.getId(), businessProcess.getCurrentNodeKey(),
                businessProcess.getCurrentStatus(), businessProcess.getProcessDefinitionKey());
        log.info("[updateNextNodeInfo] 业务表原 dslJson 长度={}",
                businessProcess.getDslJson() != null ? businessProcess.getDslJson().length() : 0);
        if (businessProcess.getDslJson() != null) {
            log.info("[updateNextNodeInfo] 业务表原 dslJson 内容 START:\n{}\ndslJson 内容 END", businessProcess.getDslJson());
        }

        try {
            // 获取流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (processInstance == null) {
                log.warn("[updateNextNodeInfo] 流程实例不存在: processInstanceId={}", processInstanceId);
                return;
            }
            log.info("[updateNextNodeInfo] 流程实例信息: processInstanceId={}, processDefinitionId={}, processDefinitionKey={}",
                    processInstance.getId(), processInstance.getProcessDefinitionId(),
                    processInstance.getProcessDefinitionKey());

            // 获取 BPMN 模型
            org.flowable.bpmn.model.BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(
                    processInstance.getProcessDefinitionId());
            if (bpmnModel == null) {
                log.warn("[updateNextNodeInfo] 获取 BpmnModel 失败: processDefinitionId={}", processInstance.getProcessDefinitionId());
                return;
            }

            // 1. 确定“下一个用户任务”节点：
            //    - 优先使用调用方传入的 nextNodeKey（一般是刚完成任务后第一个待办任务的 definitionKey）
            //    - 为空时，从运行时任务列表中取创建时间最早的一个
            UserTask nextUserTask = null;
            String targetNodeKey = nextNodeKey;
            if (StrUtil.isBlank(targetNodeKey)) {
                List<Task> nextTasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .orderByTaskCreateTime().asc()
                        .list();
                if (nextTasks != null && !nextTasks.isEmpty()) {
                    targetNodeKey = nextTasks.get(0).getTaskDefinitionKey();
                }
            }
            if (StrUtil.isNotBlank(targetNodeKey)) {
                FlowElement element = bpmnModel.getFlowElement(targetNodeKey);
                if (element instanceof UserTask) {
                    nextUserTask = (UserTask) element;
                }
            }
            if (nextUserTask == null) {
                return;
            }

            // 如果通过运行时任务推断出了目标节点，则顺便刷新业务表中的 currentNodeKey，避免偏移
            if (StrUtil.isNotBlank(targetNodeKey)) {
                businessProcess.setCurrentNodeKey(targetNodeKey);
            }

            // 从已部署流程定义的 XML 获取完整 DSL，避免使用模型最新版本导致版本不一致问题
            String dslConfig = null;
            try {
                // 从已部署的流程定义获取 XML（而不是从模型获取，模型可能已被修改）
                String bpmnXml = modelService.getBpmnXmlByDefinitionId(processInstance.getProcessDefinitionId());
                if (StrUtil.isNotBlank(bpmnXml)) {
                    dslConfig = BpmnModelUtils.parseDslConfigFromXml(bpmnXml, nextUserTask.getId());
                    log.info("[updateNextNodeInfo] 从已部署流程定义获取 DSL, processDefinitionId={}, nodeKey={}, dsl长度={}, dsl内容={}",
                            processInstance.getProcessDefinitionId(), nextUserTask.getId(),
                            dslConfig != null ? dslConfig.length() : 0,
                            dslConfig != null ? dslConfig.substring(0, Math.min(100, dslConfig.length())) : "null");
                }
            } catch (Exception e) {
                log.warn("[updateNextNodeInfo] 获取已部署流程 XML 失败: {}", e.getMessage());
            }
            // 兜底：从 BpmnModel 获取（通过 Flowable 解析后的对象）
            if (StrUtil.isBlank(dslConfig) || !isValidDslJson(dslConfig)) {
                String fallbackDsl = BpmnModelUtils.parseDslConfig(nextUserTask);
                log.info("[updateNextNodeInfo] 兜底获取 DSL, nodeKey={}, dsl长度={}, isValid={}, dsl内容={}",
                        nextUserTask.getId(), fallbackDsl != null ? fallbackDsl.length() : 0,
                        isValidDslJson(fallbackDsl),
                        fallbackDsl != null ? fallbackDsl.substring(0, Math.min(100, fallbackDsl.length())) : "null");
                if (isValidDslJson(fallbackDsl)) {
                    dslConfig = fallbackDsl;
                } else {
                    // DSL 无效或被截断，尝试从扩展属性重建完整 DSL
                    String rebuiltDsl = rebuildDslFromExtension(nextUserTask);
                    if (isValidDslJson(rebuiltDsl)) {
                        dslConfig = rebuiltDsl;
                        log.info("[updateNextNodeInfo] 从扩展属性重建 DSL 成功: nodeKey={}, dsl长度={}",
                                nextUserTask.getId(), dslConfig.length());
                    }
                }
            }
            // 解析 assign 信息
            Map<String, String> assignInfo = null;
            if (isValidDslJson(dslConfig)) {
                // DSL 有效，从 DSL 解析
                assignInfo = parseAssignFromDsl(dslConfig);
                businessProcess.setDslJson(dslConfig);
                log.info("[updateNextNodeInfo] DSL有效，保存下一个节点的DSL: nodeKey={}, nodeKeyInDsl={}",
                        nextUserTask.getId(), JSONUtil.parseObj(dslConfig).getStr("nodeKey"));
            } else if (nextUserTask != null) {
                // DSL 无效，从 Flowable 扩展属性读取（用于没有配置 DSL 的节点）
                assignInfo = parseAssignFromExtension(nextUserTask);
                // 清除无效的 DSL（下一个节点没有配置 DSL）
                log.info("[updateNextNodeInfo] 下一个节点无DSL配置，清除DSL: nextNodeKey={}", nextUserTask.getId());
                businessProcess.setDslJson(null);
            }
            // 更新业务进程记录
            if (assignInfo != null) {
                businessProcess.setCurrentAssignType(assignInfo.get("type"));
                businessProcess.setCurrentAssignSource(assignInfo.get("source"));
            }
            log.info("[updateNextNodeInfo] 更新分配信息: assignType={}, assignSource={}",
                    businessProcess.getCurrentAssignType(), businessProcess.getCurrentAssignSource());
            log.info("[updateNextNodeInfo] 更新后的 dslJson 长度={}",
                    businessProcess.getDslJson() != null ? businessProcess.getDslJson().length() : 0);
            if (businessProcess.getDslJson() != null) {
                log.info("[updateNextNodeInfo] 更新后的 dslJson 完整内容 START:\n{}\ndslJson 完整内容 END", businessProcess.getDslJson());
            }
        } catch (Exception e) {
            log.warn("[updateNextNodeInfo] 更新下一个节点信息失败: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage(), e);
        }
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
     * 判断是否为可写入 MySQL JSON 列的合法 JSON 字符串（必须以 { 开头）
     */
    private static boolean isValidDslJson(String s) {
        if (StrUtil.isBlank(s)) {
            return false;
        }
        String trimmed = s.trim();
        return trimmed.startsWith("{") && trimmed.length() > 1;
    }

    /**
     * 判断流程是否为进行中状态
     * 进行中状态：STARTED、SUBMITTED、AUDITING 等
     * 结束状态：COMPLETED、REJECTED、CANCELLED 等
     */
    private static boolean isProcessRunning(String status) {
        if (StrUtil.isBlank(status)) {
            return false;
        }
        // 进行中状态列表
        return "STARTED".equals(status) || "SUBMITTED".equals(status)
                || "AUDITING".equals(status) || "APPROVING".equals(status);
    }

    /**
     * 创建业务流程关联记录
     */
    private void createBusinessProcess(String businessType, Long businessId,
                                        String processInstanceId, String processDefinitionKey,
                                        Long userId, String firstNodeKey,
                                        String assignType, String assignSource,
                                        String dslJson) {
        log.info("[createBusinessProcess] ====== 创建业务流程记录 ======");
        log.info("[createBusinessProcess] businessType={}, businessId={}, processInstanceId={}, processDefinitionKey={}",
                businessType, businessId, processInstanceId, processDefinitionKey);
        log.info("[createBusinessProcess] userId={}, firstNodeKey={}, assignType={}, assignSource={}",
                userId, firstNodeKey, assignType, assignSource);
        log.info("[createBusinessProcess] dslJson 长度={}", dslJson != null ? dslJson.length() : 0);
        if (dslJson != null) {
            log.info("[createBusinessProcess] dslJson 完整内容 START:\n{}\ndslJson 完整内容 END", dslJson);
        }

        BpmBusinessProcessDO businessProcess = new BpmBusinessProcessDO();
        businessProcess.setBusinessType(businessType);
        businessProcess.setBusinessId(businessId);
        businessProcess.setProcessInstanceId(processInstanceId);
        businessProcess.setProcessDefinitionKey(processDefinitionKey);
        businessProcess.setCurrentNodeKey(firstNodeKey);
        businessProcess.setCurrentAssignType(assignType);
        businessProcess.setCurrentAssignSource(assignSource);
        businessProcess.setDslJson(isValidDslJson(dslJson) ? dslJson : null);
        businessProcess.setInitiatorId(userId);
        // 初始化参与者列表，包含发起人
        businessProcess.setInitiatorIds(String.valueOf(userId));
        businessProcess.setStartTime(LocalDateTime.now());
        businessProcess.setCurrentStatus("STARTED");
        businessProcessMapper.insert(businessProcess);
        log.info("[createBusinessProcess] 业务流程记录创建成功, id={}", businessProcess.getId());
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
     * 使用 JSON 解析器方案，更稳固可靠
     */
    private Map<String, String> parseAssignFromDsl(String dslConfig) {
        Map<String, String> result = new HashMap<>();
        try {
            if (StrUtil.isBlank(dslConfig)) {
                return result;
            }

            // 使用 Hutool JSON 解析器解析整个 DSL
            JSONObject dslJson = JSONUtil.parseObj(dslConfig);

            // 获取 assign 对象
            JSONObject assignJson = dslJson.getJSONObject("assign");
            if (assignJson == null) {
                return result;
            }

            // 安全获取 type 和 source
            String type = assignJson.getStr("type");
            String source = assignJson.getStr("source");

            if (StrUtil.isNotBlank(type)) {
                result.put("type", type);
            }
            if (StrUtil.isNotBlank(source)) {
                result.put("source", source);
            }

            log.debug("[parseAssignFromDsl] 解析成功: type={}, source={}", type, source);

        } catch (Exception e) {
            log.error("[parseAssignFromDsl] JSON解析失败: {}", e.getMessage());
        }
        return result;
    }

    /*

    // ========== 原 indexOf 方案（已废弃）==========

    // 原始的 indexOf 字符串解析方案存在以下问题：
    // 1. 无法处理嵌套JSON
    // 2. 无法处理转义字符
    // 3. 无法处理空白符
    // 4. 无法处理数组
    // 5. 鲁棒性差，任何格式变化都可能导致解析失败

    private Map<String, String> parseAssignFromDsl_OLD(String dslConfig) {
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

    */

    /**
     * 从 Flowable 扩展属性中解析 assign 类型和来源
     * 用于没有配置 DSL 的节点，从 Flowable 原生扩展属性读取候选人策略
     *
     * @return 包含 type 和 source 的 Map，如果解析失败返回 null
     */
    private Map<String, String> parseAssignFromExtension(UserTask userTask) {
        if (userTask == null) {
            return null;
        }
        // 读取候选人策略（对应 assignType）
        Integer candidateStrategy = BpmnModelUtils.parseCandidateStrategy(userTask);
        if (candidateStrategy == null) {
            return null;
        }
        // 读取候选人参数（对应 assignSource，如岗位ID）
        String candidateParam = BpmnModelUtils.parseCandidateParam(userTask);

        // 将数字策略转换成字符串名称（如 24 -> DEPT_POST）
        String assignType = convertStrategyToString(candidateStrategy);

        Map<String, String> result = new HashMap<>();
        result.put("type", assignType);
        if (StrUtil.isNotBlank(candidateParam)) {
            result.put("source", candidateParam);
        }
        log.info("[parseAssignFromExtension] 从扩展属性解析: candidateStrategy={}, assignType={}, candidateParam={}",
                candidateStrategy, assignType, candidateParam);
        return result;
    }

    /**
     * 将数字策略转换成字符串名称
     */
    private String convertStrategyToString(Integer strategy) {
        if (strategy == null) {
            return null;
        }
        for (BpmTaskCandidateStrategyEnum e : BpmTaskCandidateStrategyEnum.values()) {
            if (e.getStrategy().equals(strategy)) {
                return e.name(); // 返回枚举名称，如 DEPT_POST
            }
        }
        // 如果找不到对应的枚举，返回数字字符串
        return String.valueOf(strategy);
    }

    /**
     * 从 Flowable 扩展属性重建完整的 DSL 配置
     * 当 DSL 被 Flowable 截断或丢失时，使用扩展属性中的信息重建
     *
     * @param userTask 用户任务节点
     * @return 重建的完整 DSL JSON 字符串，如果无法重建则返回 null
     */
    private String rebuildDslFromExtension(UserTask userTask) {
        if (userTask == null) {
            return null;
        }
        try {
            // 读取扩展属性
            Integer candidateStrategy = BpmnModelUtils.parseCandidateStrategy(userTask);
            String candidateParam = BpmnModelUtils.parseCandidateParam(userTask);
            if (candidateStrategy == null) {
                return null;
            }

            // 构建基本 DSL 结构
            JSONObject dsl = new JSONObject();
            dsl.set("nodeKey", userTask.getId());
            // 根据策略设置 cap 类型
            String cap = "AUDIT"; // 默认
            if (candidateStrategy == 34 || candidateStrategy == 35) {
                cap = "EXPERT_SELECT"; // 审批人自选或发起人自选
            }
            dsl.set("cap", cap);

            // 从 buttonsSetting 读取 actions
            JSONArray actions = new JSONArray();
            String buttonsSetting = BpmnModelUtils.parseExtensionElement(userTask, "buttonsSetting");
            if (StrUtil.isNotBlank(buttonsSetting)) {
                // 解析 buttonsSetting JSON
                JSONArray buttons = JSONUtil.parseArray(buttonsSetting);
                for (Object btn : buttons) {
                    JSONObject button = (JSONObject) btn;
                    JSONObject action = new JSONObject();
                    action.set("key", button.getStr("id"));
                    action.set("label", button.getStr("displayName"));
                    action.set("bizStatus", button.getStr("bizStatus"));
                    action.set("bizStatusLabel", button.getStr("bizStatusLabel"));
                    action.set("bpmAction", button.getStr("bpmAction"));
                    actions.add(action);
                }
            } else {
                // 默认添加一个"通过"动作
                JSONObject submitAction = new JSONObject();
                submitAction.set("key", "submit");
                submitAction.set("label", "提交");
                submitAction.set("bizStatus", "SUBMITTED");
                submitAction.set("bizStatusLabel", "待审核");
                submitAction.set("bpmAction", "complete");
                actions.add(submitAction);
            }
            dsl.set("actions", actions);

            // 设置 roles（暂不设置，从扩展属性无法获取）
            dsl.set("roles", new JSONArray());

            // 设置 assign
            JSONObject assign = new JSONObject();
            assign.set("type", convertStrategyToString(candidateStrategy));
            assign.set("source", candidateParam);
            dsl.set("assign", assign);

            // 设置 backStrategy
            String rejectReturnTaskId = BpmnModelUtils.parseExtensionElement(userTask, "rejectReturnTaskId");
            dsl.set("backStrategy", StrUtil.isNotBlank(rejectReturnTaskId) ? "TO_PREV" : "TO_START");

            // 设置 enable
            dsl.set("enable", true);

            String rebuiltDsl = dsl.toString();
            log.info("[rebuildDslFromExtension] 重建 DSL 成功: nodeKey={}, dsl长度={}",
                    userTask.getId(), rebuiltDsl.length());
            return rebuiltDsl;
        } catch (Exception e) {
            log.warn("[rebuildDslFromExtension] 重建 DSL 失败: nodeKey={}, error={}",
                    userTask.getId(), e.getMessage());
            return null;
        }
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

        // 重新解析当前节点的 assign 配置，刷新 current_assign_type/source 和 dsl_json
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
                        // 仅持久化合法 JSON，避免 MySQL JSON 列报错
                        businessProcess.setDslJson(isValidDslJson(dslConfig) ? dslConfig : null);
                        if (isValidDslJson(dslConfig)) {
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
