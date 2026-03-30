package cn.gemrun.base.module.bpm.service.declare;

import cn.hutool.core.collection.CollUtil;
import cn.gemrun.base.framework.common.util.number.NumberUtils;
import java.util.Objects;
import cn.gemrun.base.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskBatchStatusRespVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskByBusinessRespVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmProcessByBusinessRespVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.gemrun.base.module.bpm.dal.dataobject.definition.BpmFormDO;
import cn.gemrun.base.module.bpm.enums.definition.BpmSimpleModelNodeTypeEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnModelConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.service.definition.BpmFormService;
import cn.gemrun.base.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.gemrun.base.framework.common.util.collection.CollectionUtils.convertMultiMap;

/**
 * 流程任务状态查询服务
 * 用于批量查询当前用户对指定流程实例的任务审批状态
 *
 * @author jason
 */
@Slf4j
@Service
public class BpmTaskStatusService {

    /**
     * 整改子流程变量名：父流程实例ID
     */
    private static final String PARENT_PROCESS_INSTANCE_ID_VAR = "PARENT_PROCESS_INSTANCE_ID";

    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @Resource
    private BpmFormService formService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmBusinessTypeService bpmBusinessTypeService;
    @Resource
    private BpmProcessInstanceService processInstanceService;

    /**
     * 批量查询流程实例的任务状态
     *
     * @param userId              用户ID
     * @param processInstanceIds  流程实例ID列表
     * @return 每个流程实例的任务状态
     */
    public List<BpmTaskBatchStatusRespVO> getTaskBatchStatus(Long userId, List<String> processInstanceIds) {
        if (CollUtil.isEmpty(processInstanceIds)) {
            return Collections.emptyList();
        }

        // 调试日志：查看所有待办任务
        List<Task> allTasks = taskService.createTaskQuery()
                .processInstanceIdIn(processInstanceIds)
                .orderByTaskCreateTime().desc()
                .list();
        log.debug("[BPM] 所有待办任务数量: {}, processInstanceIds: {}", allTasks.size(), processInstanceIds);
        for (Task task : allTasks) {
            log.debug("[BPM] 待办任务: taskId={}, name={}, assignee={}, processInstanceId={}",
                    task.getId(), task.getName(), task.getAssignee(), task.getProcessInstanceId());
        }

        // 1. 获取用户待办任务
        List<Task> todoTasks = taskService.createTaskQuery()
                .processInstanceIdIn(processInstanceIds)
                .taskAssignee(userId.toString())
                .orderByTaskCreateTime().desc()
                .list();
        log.debug("[BPM] 用户({})的待办任务数量: {}", userId, todoTasks.size());

        // 2. 获取用户已办任务
        List<HistoricTaskInstance> doneTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceIdIn(processInstanceIds)
                .taskAssignee(userId.toString())
                .finished()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        // 3. 获取所有已办任务（用于审批记录展示，显示整个流程的审批历史）
        List<HistoricTaskInstance> allDoneTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceIdIn(processInstanceIds)
                .finished()
                .includeTaskLocalVariables()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        // 4. 按流程实例ID分组
        Map<String, List<Task>> todoTaskMap = convertMultiMap(todoTasks, Task::getProcessInstanceId);
        Map<String, List<HistoricTaskInstance>> doneTaskMap = convertMultiMap(doneTasks, HistoricTaskInstance::getProcessInstanceId);
        Map<String, List<HistoricTaskInstance>> allDoneTaskMap = convertMultiMap(allDoneTasks, HistoricTaskInstance::getProcessInstanceId);

        // 5. 构建返回结果
        List<BpmTaskBatchStatusRespVO> result = new ArrayList<>();
        for (String processInstanceId : processInstanceIds) {
            BpmTaskBatchStatusRespVO respVO = new BpmTaskBatchStatusRespVO();
            respVO.setProcessInstanceId(processInstanceId);

            // 待办任务
            List<Task> instanceTodoTasks = todoTaskMap.get(processInstanceId);
            if (CollUtil.isNotEmpty(instanceTodoTasks)) {
                respVO.setHasTodoTask(true);
                respVO.setTodoTasks(convertTodoTasks(instanceTodoTasks));
            } else {
                respVO.setHasTodoTask(false);
                respVO.setTodoTasks(Collections.emptyList());
            }

            // 用户已办任务
            List<HistoricTaskInstance> instanceDoneTasks = doneTaskMap.get(processInstanceId);
            if (CollUtil.isNotEmpty(instanceDoneTasks)) {
                respVO.setDoneTasks(convertDoneTasks(instanceDoneTasks));
            } else {
                respVO.setDoneTasks(Collections.emptyList());
            }

            // 所有已办任务（用于审批记录展示）
            List<HistoricTaskInstance> instanceAllDoneTasks = allDoneTaskMap.get(processInstanceId);
            if (CollUtil.isNotEmpty(instanceAllDoneTasks)) {
                respVO.setAllDoneTasks(convertDoneTasks(instanceAllDoneTasks));
            } else {
                respVO.setAllDoneTasks(Collections.emptyList());
            }

            result.add(respVO);
        }

        return result;
    }

    /**
     * 转换待办任务
     */
    private List<BpmTaskBatchStatusRespVO.TodoTask> convertTodoTasks(List<Task> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }

        // 获取流程定义ID（去重）
        Set<String> processDefinitionIds = tasks.stream()
                .map(Task::getProcessDefinitionId)
                .collect(Collectors.toSet());

        // 批量获取 BpmnModel
        Map<String, BpmnModel> bpmnModelMap = new HashMap<>();
        for (String processDefinitionId : processDefinitionIds) {
            BpmnModel bpmnModel = bpmProcessDefinitionService.getProcessDefinitionBpmnModel(processDefinitionId);
            if (bpmnModel != null) {
                bpmnModelMap.put(processDefinitionId, bpmnModel);
            }
        }

        // 获取表单信息
        Set<Long> formIds = tasks.stream()
                .map(task -> NumberUtils.parseLong(task.getFormKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BpmFormDO> formMap = formService.getFormMap(formIds);

        // 转换
        return tasks.stream().map(task -> {
            BpmTaskBatchStatusRespVO.TodoTask todoTask = new BpmTaskBatchStatusRespVO.TodoTask();
            todoTask.setTaskId(task.getId());
            todoTask.setTaskName(task.getName());
            todoTask.setTaskDefinitionKey(task.getTaskDefinitionKey());
            todoTask.setCreateTime(task.getCreateTime() != null ? task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
            todoTask.setFormId(NumberUtils.parseLong(task.getFormKey()));

            // 获取表单信息
            if (todoTask.getFormId() != null) {
                BpmFormDO form = formMap.get(todoTask.getFormId());
                if (form != null) {
                    todoTask.setFormName(form.getName());
                    todoTask.setFormConf(form.getConf());
                }
            }

            // 获取按钮设置
            BpmnModel bpmnModel = bpmnModelMap.get(task.getProcessDefinitionId());
            if (bpmnModel != null) {
                Map<Integer, BpmTaskRespVO.OperationButtonSetting> buttonSettings =
                        BpmnModelUtils.parseButtonsSetting(bpmnModel, task.getTaskDefinitionKey());
                if (buttonSettings != null) {
                    Map<Integer, BpmTaskBatchStatusRespVO.ButtonSetting> convertedSettings = new HashMap<>();
                    for (Map.Entry<Integer, BpmTaskRespVO.OperationButtonSetting> entry : buttonSettings.entrySet()) {
                        BpmTaskBatchStatusRespVO.ButtonSetting buttonSetting = new BpmTaskBatchStatusRespVO.ButtonSetting();
                        buttonSetting.setDisplayName(entry.getValue().getDisplayName());
                        buttonSetting.setEnable(entry.getValue().getEnable());
                        buttonSetting.setBizStatus(entry.getValue().getBizStatus());
                        convertedSettings.put(entry.getKey(), buttonSetting);
                    }
                    todoTask.setButtonSettings(convertedSettings);
                }

                // 获取其他设置
                todoTask.setSignEnable(parseSignEnable(bpmnModel, task.getTaskDefinitionKey()));
                todoTask.setReasonRequire(parseReasonRequire(bpmnModel, task.getTaskDefinitionKey()));

                // 获取节点类型
                FlowElement flowElement = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
                if (flowElement instanceof UserTask) {
                    UserTask userTask = (UserTask) flowElement;
                    String nodeTypeStr = userTask.getAttributeValue(BpmnModelConstants.NAMESPACE, "nodeType");
                    if (nodeTypeStr != null) {
                        BpmSimpleModelNodeTypeEnum nodeTypeEnum = BpmSimpleModelNodeTypeEnum.valueOf(Integer.parseInt(nodeTypeStr));
                        todoTask.setNodeType(nodeTypeEnum != null ? nodeTypeEnum.getType() : null);
                    }
                }
            }

            return todoTask;
        }).collect(Collectors.toList());
    }

    /**
     * 转换已办任务
     */
    private List<BpmTaskBatchStatusRespVO.DoneTask> convertDoneTasks(List<HistoricTaskInstance> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }

        // 获取用户信息
        Set<Long> userIds = tasks.stream()
                .map(task -> NumberUtils.parseLong(task.getAssignee()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        return tasks.stream().map(task -> {
            BpmTaskBatchStatusRespVO.DoneTask doneTask = new BpmTaskBatchStatusRespVO.DoneTask();
            doneTask.setTaskId(task.getId());
            doneTask.setTaskName(task.getName());
            doneTask.setTaskDefinitionKey(task.getTaskDefinitionKey());
            doneTask.setEndTime(task.getEndTime() != null ? task.getEndTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);

            // 获取审批结果和意见
            Map<String, Object> taskLocalVariables = task.getTaskLocalVariables();
            if (taskLocalVariables != null) {
                doneTask.setApproved((Boolean) taskLocalVariables.get("approved"));
                doneTask.setReason((String) taskLocalVariables.get(BpmnVariableConstants.TASK_VARIABLE_REASON));
                // 获取任务状态（用于判断退回等操作）
                Object statusObj = taskLocalVariables.get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
                if (statusObj != null) {
                    doneTask.setStatus(statusObj instanceof Integer ? (Integer) statusObj : Integer.parseInt(statusObj.toString()));
                }
            }

            // 获取审批人信息
            Long assigneeId = NumberUtils.parseLong(task.getAssignee());
            if (assigneeId != null && userMap.containsKey(assigneeId)) {
                AdminUserRespDTO user = userMap.get(assigneeId);
                UserSimpleBaseVO assigneeUserVO = new UserSimpleBaseVO();
                assigneeUserVO.setId(user.getId());
                assigneeUserVO.setNickname(user.getNickname());
                assigneeUserVO.setAvatar(user.getAvatar());
                assigneeUserVO.setDeptId(user.getDeptId());
                doneTask.setAssigneeUser(assigneeUserVO);
            }

            return doneTask;
        }).collect(Collectors.toList());
    }

    /**
     * 解析是否需要签名
     */
    private Boolean parseSignEnable(BpmnModel bpmnModel, String taskDefinitionKey) {
        try {
            return BpmnModelUtils.parseSignEnable(bpmnModel, taskDefinitionKey);
        } catch (Exception e) {
            log.warn("解析签名设置失败, taskDefinitionKey: {}", taskDefinitionKey, e);
            return false;
        }
    }

    /**
     * 解析是否需要填写审批意见
     */
    private Boolean parseReasonRequire(BpmnModel bpmnModel, String taskDefinitionKey) {
        try {
            return BpmnModelUtils.parseReasonRequire(bpmnModel, taskDefinitionKey);
        } catch (Exception e) {
            log.warn("解析审批意见设置失败, taskDefinitionKey: {}", taskDefinitionKey, e);
            return false;
        }
    }

    /**
     * 根据业务ID批量查询任务状态
     * <p>
     * 用于项目列表、备案列表等页面，获取每个业务ID上当前用户是否有待办/已办任务
     *
     * @param userId       用户ID
     * @param tableName    业务表分类（对应 bpm_business_type.process_category，如 project, filing）
     * @param businessType 业务类型（对应 bpm_business_type.business_type，如 project_process:type:1），可选
     * @param businessIds 业务ID列表
     * @return 每个业务ID对应的任务状态
     */
    public List<BpmTaskByBusinessRespVO> getTaskByBusiness(Long userId, String tableName, String businessType, List<Long> businessIds) {
        if (CollUtil.isEmpty(businessIds)) {
            return Collections.emptyList();
        }

        // 1. 根据 businessType 或 tableName 查询 bpm_business_type 表获取已启用的 processDefinitionKey
        List<String> processDefinitionKeys;
        if (businessType != null && !businessType.isEmpty()) {
            // 精确匹配：使用 businessType 查找对应的流程定义Key
            String processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessType);
            processDefinitionKeys = processDefinitionKey != null ? Collections.singletonList(processDefinitionKey) : Collections.emptyList();
        } else {
            // 模糊匹配：使用 tableName（process_category）查找所有已启用的流程定义Key
            processDefinitionKeys = bpmBusinessTypeService.getProcessDefinitionKeysByCategory(tableName);
        }

        if (CollUtil.isEmpty(processDefinitionKeys)) {
            log.warn("[getTaskByBusiness] 没有找到有效的流程定义: tableName={}, businessType={}, processDefinitionKeys={}",
                    tableName, businessType, processDefinitionKeys);
            // 没有关联的流程定义，返回空结果
            return businessIds.stream()
                    .map(businessId -> {
                        BpmTaskByBusinessRespVO respVO = new BpmTaskByBusinessRespVO();
                        respVO.setBusinessId(businessId);
                        respVO.setHasTodoTask(false);
                        respVO.setTodoTasks(Collections.emptyList());
                        respVO.setDoneTasks(Collections.emptyList());
                        respVO.setAllDoneTasks(Collections.emptyList());
                        respVO.setHasRectificationTodoTask(false);
                        respVO.setRectificationTodoTasks(Collections.emptyList());
                        return respVO;
                    })
                    .collect(Collectors.toList());
        }

        log.info("[getTaskByBusiness] 找到流程定义: tableName={}, businessType={}, processDefinitionKeys={}",
                tableName, businessType, processDefinitionKeys);

        // 2. 构建 businessKey 列表
        // 格式: {processDefinitionKey}_{businessId} - 与发起流程时保持一致
        // 遍历 processDefinitionKeys，为每个 businessId 构建对应的 businessKey
        Map<String, Long> businessKeyToBusinessId = new HashMap<>();
        List<String> businessKeys = new ArrayList<>();
        for (Long businessId : businessIds) {
            for (String processDefinitionKey : processDefinitionKeys) {
                String businessKey = processDefinitionKey + "_" + businessId;
                businessKeys.add(businessKey);
                businessKeyToBusinessId.put(businessKey, businessId);
            }
        }

        // 3. 批量查询主流程实例（包含运行中 + 已结束）
        // 注意：流程结束后实例从 runtime 表移到 history 表，必须同时查询两表
        // 使用 LinkedHashSet 去重：Flowable 在同一事务内可能同时在 runtime/history 两表存在同一实例
        Set<String> allProcessInstanceIds = new java.util.LinkedHashSet<>();
        Map<String, Long> processInstanceIdToBusinessId = new HashMap<>();
        log.info("[getTaskByBusiness] 开始查询主流程实例: businessKeys={}", businessKeys);
        for (String businessKey : businessKeys) {
            // 查询运行中的流程实例
            List<org.flowable.engine.runtime.ProcessInstance> runningPis = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();
            log.debug("[getTaskByBusiness] businessKey={}, 运行中流程实例数量={}", businessKey, runningPis.size());
            for (org.flowable.engine.runtime.ProcessInstance pi : runningPis) {
                allProcessInstanceIds.add(pi.getId());
            }

            // 查询已结束的流程实例（Flowable 流程结束后实例进入 history 表）
            List<org.flowable.engine.history.HistoricProcessInstance> endedPis = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();
            log.debug("[getTaskByBusiness] businessKey={}, 已结束流程实例数量={}", businessKey, endedPis.size());
            for (org.flowable.engine.history.HistoricProcessInstance hpi : endedPis) {
                allProcessInstanceIds.add(hpi.getId());
            }
        }
        log.info("[getTaskByBusiness] 主流程实例查询完成: 总数={}", allProcessInstanceIds.size());

        // 4. 构建 processInstanceId -> businessId 的映射
        // 通过 historyService 查询每个实例的 businessKey（runtime 流程查不到时，历史表仍可查到）
        for (String processInstanceId : allProcessInstanceIds) {
            org.flowable.engine.history.HistoricProcessInstance hpi = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (hpi != null && hpi.getBusinessKey() != null
                    && businessKeyToBusinessId.containsKey(hpi.getBusinessKey())) {
                processInstanceIdToBusinessId.put(processInstanceId, businessKeyToBusinessId.get(hpi.getBusinessKey()));
            }
        }

        // 5. 查询关联的整改子流程实例（通过 PARENT_PROCESS_INSTANCE_ID 关联）
        List<org.flowable.engine.runtime.ProcessInstance> rectificationInstances;
        List<String> parentProcessInstanceIds = new ArrayList<>(processInstanceIdToBusinessId.keySet());
        if (CollUtil.isNotEmpty(parentProcessInstanceIds)) {
            rectificationInstances = queryRectificationChildProcesses(parentProcessInstanceIds);
            log.info("[getTaskByBusiness] 查询到整改子流程实例: 数量={}", rectificationInstances.size());

            // 将整改子流程实例添加到映射
            for (org.flowable.engine.runtime.ProcessInstance rectInstance : rectificationInstances) {
                String rectBusinessKey = rectInstance.getBusinessKey();
                if (rectBusinessKey != null) {
                    String[] parts = rectBusinessKey.split("_");
                    if (parts.length >= 2) {
                        Long rectBusinessId = NumberUtils.parseLong(parts[parts.length - 1]);
                        if (rectBusinessId != null) {
                            processInstanceIdToBusinessId.put(rectInstance.getId(), rectBusinessId);
                        }
                    }
                }
            }
        } else {
            rectificationInstances = Collections.emptyList();
        }

        // 6. 如果没有流程实例，直接返回空结果
        if (CollUtil.isEmpty(processInstanceIdToBusinessId)) {
            return businessIds.stream()
                    .map(businessId -> {
                        BpmTaskByBusinessRespVO respVO = new BpmTaskByBusinessRespVO();
                        respVO.setBusinessId(businessId);
                        respVO.setHasTodoTask(false);
                        respVO.setTodoTasks(Collections.emptyList());
                        respVO.setDoneTasks(Collections.emptyList());
                        respVO.setAllDoneTasks(Collections.emptyList());
                        respVO.setHasRectificationTodoTask(false);
                        respVO.setRectificationTodoTasks(Collections.emptyList());
                        return respVO;
                    })
                    .collect(Collectors.toList());
        }

        // 7. 批量查询任务状态
        List<BpmTaskBatchStatusRespVO> batchStatusList = getTaskBatchStatus(userId, new ArrayList<>(allProcessInstanceIds));

        // 8. 按 businessId 封装结果
        Map<String, BpmTaskBatchStatusRespVO> processInstanceIdToStatus = batchStatusList.stream()
                .collect(Collectors.toMap(BpmTaskBatchStatusRespVO::getProcessInstanceId, v -> v));

        // 9. 构建返回结果，保持 businessIds 的顺序
        return businessIds.stream()
                .map(businessId -> {
                    BpmTaskByBusinessRespVO respVO = new BpmTaskByBusinessRespVO();
                    respVO.setBusinessId(businessId);

                    // 查找对应的流程实例ID
                    String matchedProcessInstanceId = null;
                    for (Map.Entry<String, Long> entry : processInstanceIdToBusinessId.entrySet()) {
                        if (entry.getValue().equals(businessId)) {
                            matchedProcessInstanceId = entry.getKey();
                            break;
                        }
                    }

                    if (matchedProcessInstanceId != null && processInstanceIdToStatus.containsKey(matchedProcessInstanceId)) {
                        BpmTaskBatchStatusRespVO batchStatus = processInstanceIdToStatus.get(matchedProcessInstanceId);
                        respVO.setProcessInstanceId(matchedProcessInstanceId);

                        // 合并主流程和整改子流程的待办任务
                        List<BpmTaskBatchStatusRespVO.TodoTask> allTodoTasks = new ArrayList<>();
                        List<BpmTaskBatchStatusRespVO.TodoTask> rectificationTasks = new ArrayList<>();
                        if (batchStatus.getTodoTasks() != null) {
                            allTodoTasks.addAll(batchStatus.getTodoTasks());
                        }

                        // 查找该业务ID对应的整改子流程任务并合并
                        for (org.flowable.engine.runtime.ProcessInstance rectInstance : rectificationInstances) {
                            String rectBusinessKey = rectInstance.getBusinessKey();
                            if (rectBusinessKey != null) {
                                String[] parts = rectBusinessKey.split("_");
                                if (parts.length >= 2) {
                                    Long rectBusinessId = NumberUtils.parseLong(parts[parts.length - 1]);
                                    if (rectBusinessId != null && rectBusinessId.equals(businessId)) {
                                        if (processInstanceIdToStatus.containsKey(rectInstance.getId())) {
                                            BpmTaskBatchStatusRespVO rectStatus = processInstanceIdToStatus.get(rectInstance.getId());
                                            if (rectStatus.getTodoTasks() != null) {
                                                allTodoTasks.addAll(rectStatus.getTodoTasks());
                                                rectificationTasks.addAll(rectStatus.getTodoTasks());
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        respVO.setHasTodoTask(CollUtil.isNotEmpty(allTodoTasks));
                        respVO.setTodoTasks(convertTodoTasksForBusiness(allTodoTasks));
                        respVO.setDoneTasks(convertDoneTasksForBusiness(batchStatus.getDoneTasks()));
                        respVO.setAllDoneTasks(convertDoneTasksForBusiness(batchStatus.getAllDoneTasks()));
                        respVO.setHasRectificationTodoTask(CollUtil.isNotEmpty(rectificationTasks));
                        respVO.setRectificationTodoTasks(convertTodoTasksForBusiness(rectificationTasks));
                    } else {
                        respVO.setHasTodoTask(false);
                        respVO.setTodoTasks(Collections.emptyList());
                        respVO.setDoneTasks(Collections.emptyList());
                        respVO.setAllDoneTasks(Collections.emptyList());
                        respVO.setHasRectificationTodoTask(false);
                        respVO.setRectificationTodoTasks(Collections.emptyList());
                    }

                    return respVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 BpmTaskBatchStatusRespVO.TodoTask 转换为 BpmTaskByBusinessRespVO.TodoTask
     */
    private List<BpmTaskByBusinessRespVO.TodoTask> convertTodoTasksForBusiness(List<BpmTaskBatchStatusRespVO.TodoTask> todoTasks) {
        if (CollUtil.isEmpty(todoTasks)) {
            return Collections.emptyList();
        }

        return todoTasks.stream()
                .map(task -> {
                    BpmTaskByBusinessRespVO.TodoTask converted = new BpmTaskByBusinessRespVO.TodoTask();
                    converted.setTaskId(task.getTaskId());
                    converted.setTaskName(task.getTaskName());
                    converted.setTaskDefinitionKey(task.getTaskDefinitionKey());
                    converted.setCreateTime(task.getCreateTime());
                    // 手动构建 ButtonSetting
                    Map<Integer, BpmTaskByBusinessRespVO.ButtonSetting> buttonSettings = new HashMap<>();
                    Map<Integer, BpmTaskBatchStatusRespVO.ButtonSetting> originalSettings = task.getButtonSettings();
                    if (originalSettings != null) {
                        for (Map.Entry<Integer, BpmTaskBatchStatusRespVO.ButtonSetting> entry : originalSettings.entrySet()) {
                            BpmTaskByBusinessRespVO.ButtonSetting setting = new BpmTaskByBusinessRespVO.ButtonSetting();
                            setting.setDisplayName(entry.getValue().getDisplayName());
                            setting.setEnable(entry.getValue().getEnable());
                            setting.setBizStatus(entry.getValue().getBizStatus());
                            buttonSettings.put(entry.getKey(), setting);
                        }
                    }
                    converted.setButtonSettings(buttonSettings);
                    converted.setSignEnable(task.getSignEnable());
                    converted.setReasonRequire(task.getReasonRequire());
                    converted.setNodeType(task.getNodeType());
                    converted.setFormId(task.getFormId());
                    converted.setFormName(task.getFormName());
                    converted.setFormConf(task.getFormConf());
                    return converted;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 BpmTaskBatchStatusRespVO.DoneTask 转换为 BpmTaskByBusinessRespVO.DoneTask
     */
    private List<BpmTaskByBusinessRespVO.DoneTask> convertDoneTasksForBusiness(List<BpmTaskBatchStatusRespVO.DoneTask> doneTasks) {
        if (CollUtil.isEmpty(doneTasks)) {
            return Collections.emptyList();
        }

        return doneTasks.stream()
                .map(task -> {
                    BpmTaskByBusinessRespVO.DoneTask converted = new BpmTaskByBusinessRespVO.DoneTask();
                    converted.setTaskId(task.getTaskId());
                    converted.setTaskName(task.getTaskName());
                    converted.setTaskDefinitionKey(task.getTaskDefinitionKey());
                    converted.setEndTime(task.getEndTime());
                    converted.setApproved(task.getApproved());
                    converted.setStatus(task.getStatus());
                    converted.setReason(task.getReason());
                    converted.setAssigneeUser(task.getAssigneeUser());
                    return converted;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据业务ID查询流程信息
     *
     * @param tableName   业务表分类
     * @param businessType 业务类型（可选）
     * @param businessId 业务ID
     * @return 流程列表，按启动时间倒序
     */
    public List<BpmProcessByBusinessRespVO> getProcessByBusiness(String tableName, String businessType, Long businessId) {
        if (tableName == null || businessId == null) {
            return Collections.emptyList();
        }

        // 1. 根据 businessType 或 tableName 查询 bpm_business_type 表获取已启用的 processDefinitionKey
        List<String> processDefinitionKeys;
        if (businessType != null && !businessType.isEmpty()) {
            // 精确匹配：使用 businessType 查找对应的流程定义Key
            String processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessType);
            processDefinitionKeys = processDefinitionKey != null ? Collections.singletonList(processDefinitionKey) : Collections.emptyList();
        } else {
            // 模糊匹配：使用 tableName（process_category）查找所有已启用的流程定义Key
            processDefinitionKeys = bpmBusinessTypeService.getProcessDefinitionKeysByCategory(tableName);
        }

        if (CollUtil.isEmpty(processDefinitionKeys)) {
            return Collections.emptyList();
        }

        // 2. 构建 businessKey 列表并查询流程实例
        // 格式: {processDefinitionKey}_{businessId} - 与发起流程时保持一致
        List<Object> allProcessInstances = new ArrayList<>();

        for (String processDefinitionKey : processDefinitionKeys) {
            String businessKey = processDefinitionKey + "_" + businessId;

            // 查询运行中的流程实例
            List<org.flowable.engine.runtime.ProcessInstance> runningInstances = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .orderByStartTime().desc()
                    .list();
            allProcessInstances.addAll(runningInstances);

            // 查询已结束的流程实例
            List<org.flowable.engine.history.HistoricProcessInstance> endedInstances = historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .orderByProcessInstanceStartTime().desc()
                    .list();
            allProcessInstances.addAll(endedInstances);
        }

        // 3. 按启动时间倒序排序并去重
        List<String> sortedProcessInstanceIds = allProcessInstances.stream()
                .map(instance -> {
                    if (instance instanceof org.flowable.engine.runtime.ProcessInstance) {
                        return ((org.flowable.engine.runtime.ProcessInstance) instance).getId();
                    } else {
                        return ((org.flowable.engine.history.HistoricProcessInstance) instance).getId();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(sortedProcessInstanceIds)) {
            return Collections.emptyList();
        }

        // 4. 重新按启动时间倒序排序
        sortedProcessInstanceIds.sort((id1, id2) -> {
            org.flowable.engine.history.HistoricProcessInstance instance1 = historyService.createHistoricProcessInstanceQuery().processInstanceId(id1).singleResult();
            org.flowable.engine.history.HistoricProcessInstance instance2 = historyService.createHistoricProcessInstanceQuery().processInstanceId(id2).singleResult();
            if (instance1 == null || instance2 == null) {
                return 0;
            }
            return instance2.getStartTime().compareTo(instance1.getStartTime());
        });

        // 5. 获取每个流程的详细信息
        List<BpmProcessByBusinessRespVO> result = new ArrayList<>();
        for (String processInstanceId : sortedProcessInstanceIds) {
            try {
                BpmApprovalDetailRespVO approvalDetail = processInstanceService.getApprovalDetail(null,
                        new BpmApprovalDetailReqVO().setProcessInstanceId(processInstanceId));

                if (approvalDetail == null) {
                    continue;
                }

                BpmProcessByBusinessRespVO respVO = new BpmProcessByBusinessRespVO();
                respVO.setProcessInstanceId(processInstanceId);

                // 设置流程定义信息
                if (approvalDetail.getProcessDefinition() != null) {
                    respVO.setProcessDefinitionKey(approvalDetail.getProcessDefinition().getKey());
                    respVO.setProcessDefinitionName(approvalDetail.getProcessDefinition().getName());
                }

                // 设置流程状态
                respVO.setStatus(approvalDetail.getStatus() != null && approvalDetail.getStatus() == 1 ? "RUNNING" : "ENDED");

                // 设置时间信息
                if (approvalDetail.getProcessInstance() != null) {
                    respVO.setStartTime(approvalDetail.getProcessInstance().getStartTime());
                    respVO.setEndTime(approvalDetail.getProcessInstance().getEndTime());
                }

                // 设置活动节点列表
                if (CollUtil.isNotEmpty(approvalDetail.getActivityNodes())) {
                    List<BpmProcessByBusinessRespVO.ActivityNodeVO> activityNodes = approvalDetail.getActivityNodes().stream()
                            .map(this::convertActivityNode)
                            .collect(Collectors.toList());
                    respVO.setActivityNodes(activityNodes);
                }

                result.add(respVO);
            } catch (Exception e) {
                log.warn("[BpmTaskStatusService] 获取流程详情失败: processInstanceId={}", processInstanceId, e);
            }
        }

        return result;
    }

    /**
     * 将 ActivityNode 转换为 ActivityNodeVO
     */
    private BpmProcessByBusinessRespVO.ActivityNodeVO convertActivityNode(BpmApprovalDetailRespVO.ActivityNode activityNode) {
        BpmProcessByBusinessRespVO.ActivityNodeVO vo = new BpmProcessByBusinessRespVO.ActivityNodeVO();
        vo.setId(activityNode.getId());
        vo.setName(activityNode.getName());
        vo.setNodeType(activityNode.getNodeType());
        vo.setStatus(activityNode.getStatus());
        vo.setStartTime(activityNode.getStartTime());
        vo.setEndTime(activityNode.getEndTime());

        if (CollUtil.isNotEmpty(activityNode.getTasks())) {
            List<BpmProcessByBusinessRespVO.ActivityNodeTaskVO> tasks = activityNode.getTasks().stream()
                    .map(this::convertActivityNodeTask)
                    .collect(Collectors.toList());
            vo.setTasks(tasks);
        }

        return vo;
    }

    /**
     * 将 ActivityNodeTask 转换为 ActivityNodeTaskVO
     */
    private BpmProcessByBusinessRespVO.ActivityNodeTaskVO convertActivityNodeTask(BpmApprovalDetailRespVO.ActivityNodeTask task) {
        BpmProcessByBusinessRespVO.ActivityNodeTaskVO vo = new BpmProcessByBusinessRespVO.ActivityNodeTaskVO();
        vo.setId(task.getId());
        vo.setOwnerUser(task.getOwnerUser());
        vo.setAssigneeUser(task.getAssigneeUser());
        vo.setReason(task.getReason());
        return vo;
    }

    /**
     * 查询主流程的整改子流程实例
     * 通过 PARENT_PROCESS_INSTANCE_ID 变量关联
     *
     * @param parentProcessInstanceIds 主流程实例ID列表
     * @return 整改子流程实例列表
     */
    private List<org.flowable.engine.runtime.ProcessInstance> queryRectificationChildProcesses(
            List<String> parentProcessInstanceIds) {
        if (CollUtil.isEmpty(parentProcessInstanceIds)) {
            return Collections.emptyList();
        }

        try {
            List<org.flowable.engine.runtime.ProcessInstance> matchedChildren = new ArrayList<>();

            for (String parentId : parentProcessInstanceIds) {
                // 查询所有包含 PARENT_PROCESS_INSTANCE_ID 变量的运行中流程实例
                List<org.flowable.engine.runtime.ProcessInstance> childProcesses = runtimeService
                        .createProcessInstanceQuery()
                        .variableValueEquals(PARENT_PROCESS_INSTANCE_ID_VAR, parentId)
                        .list();

                if (CollUtil.isNotEmpty(childProcesses)) {
                    for (org.flowable.engine.runtime.ProcessInstance child : childProcesses) {
                        Object actualParentId = runtimeService.getVariable(child.getId(), PARENT_PROCESS_INSTANCE_ID_VAR);
                        if (actualParentId != null && actualParentId.toString().equals(parentId)) {
                            matchedChildren.add(child);
                            log.debug("[queryRectificationChildProcesses] 匹配到整改子流程: childId={}, parentId={}",
                                    child.getId(), actualParentId);
                        }
                    }
                }
            }

            return matchedChildren;
        } catch (Exception e) {
            log.warn("[queryRectificationChildProcesses] 查询整改子流程失败: parentProcessInstanceIds={}", parentProcessInstanceIds, e);
            return Collections.emptyList();
        }
    }

}
