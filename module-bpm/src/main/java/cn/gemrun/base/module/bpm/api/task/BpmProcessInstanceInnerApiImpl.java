package cn.gemrun.base.module.bpm.api.task;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailReqDTO;
import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailRespDTO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceRespVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.gemrun.base.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.dal.mysql.process.BpmBusinessProcessMapper;
import cn.gemrun.base.module.bpm.framework.dsl.DslApprovalDetailBuilder;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.gemrun.base.module.bpm.service.definition.BpmModelService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import cn.gemrun.base.module.system.api.dept.DeptApi;
import cn.gemrun.base.module.system.api.dept.dto.DeptRespDTO;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程实例内部 API 实现类
 * 基于 DSL 获取审批详情
 *
 * @author Gemini
 */
@Slf4j
@Service
@Validated
public class BpmProcessInstanceInnerApiImpl implements BpmProcessInstanceInnerApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmTaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private BpmBusinessProcessMapper businessProcessMapper;
    @Resource
    private BpmModelService modelService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private DslApprovalDetailBuilder dslApprovalDetailBuilder;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Override
    public BpmApprovalDetailRespDTO getApprovalDetail(Long userId, @Valid BpmApprovalDetailReqDTO reqDTO) {
        // 1. 获取流程实例信息
        String processInstanceId = reqDTO.getProcessInstanceId();
        // 如果 processInstanceId 为空但 taskId 不为空，通过 taskId 获取
        if (processInstanceId == null && reqDTO.getTaskId() != null) {
            Task task = taskService.getTask(reqDTO.getTaskId());
            if (task != null) {
                processInstanceId = task.getProcessInstanceId();
                reqDTO.setProcessInstanceId(processInstanceId);
            }
        }
        if (processInstanceId == null) {
            return null;
        }

        log.info("[getApprovalDetail] 开始获取审批详情: userId={}, processInstanceId={}", userId, processInstanceId);

        // 2. 从业务表获取 DSL 配置
        BpmBusinessProcessDO businessProcess = businessProcessMapper.selectByProcessInstanceId(processInstanceId);

        // 3. 如果不支持 DSL，回退到原有实现
        if (!dslApprovalDetailBuilder.isSupportDslApprovalDetail(businessProcess)) {
            log.info("[getApprovalDetail] 业务表未找到 DSL 配置, processInstanceId={}, 回退到原有实现", processInstanceId);
            return fallbackToOriginal(userId, reqDTO);
        }

        // 4. 基于 DSL 构建审批详情
        try {
            return buildDslApprovalDetail(userId, reqDTO, businessProcess);
        } catch (Exception e) {
            log.warn("[getApprovalDetail] DSL 构建审批详情失败: processInstanceId={}, error={}", processInstanceId, e.getMessage());
            log.info("[getApprovalDetail] 回退到原有实现");
            return fallbackToOriginal(userId, reqDTO);
        }
    }

    /**
     * 基于 DSL 构建审批详情
     */
    private BpmApprovalDetailRespDTO buildDslApprovalDetail(Long userId, BpmApprovalDetailReqDTO reqDTO,
                                                            BpmBusinessProcessDO businessProcess) {
        String processInstanceId = reqDTO.getProcessInstanceId();

        // 4.1 获取 BPMN 模型
        // 优先从运行时获取，如果流程已结束则从历史中获取
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        String processDefinitionId = null;

        if (processInstance != null) {
            // 流程仍在运行中
            processDefinitionId = processInstance.getProcessDefinitionId();
        } else {
            // 流程已结束，从历史记录中获取流程定义ID
            log.info("[getApprovalDetail] 流程实例已结束，从历史记录中获取: processInstanceId={}", processInstanceId);
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (historicProcessInstance == null) {
                log.warn("[getApprovalDetail] 流程实例不存在: processInstanceId={}", processInstanceId);
                return fallbackToOriginal(userId, reqDTO);
            }
            processDefinitionId = historicProcessInstance.getProcessDefinitionId();
        }

        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(processDefinitionId);
        if (bpmnModel == null) {
            log.warn("[getApprovalDetail] 获取 BpmnModel 失败: processDefinitionId={}", processDefinitionId);
            return fallbackToOriginal(userId, reqDTO);
        }

        // 4.2 获取当前节点 DSL 配置
        DslConfig currentNodeDsl = dslApprovalDetailBuilder.getCurrentNodeDsl(businessProcess);
        if (currentNodeDsl != null) {
            log.info("[getApprovalDetail] 当前节点 DSL 配置: nodeKey={}, cap={}, actions={}",
                    currentNodeDsl.getNodeKey(), currentNodeDsl.getCap(), currentNodeDsl.getActions());
        }

        // 4.3 获取历史任务列表
        List<HistoricTaskInstance> historicTasks = taskService.getTaskListByProcessInstanceId(processInstanceId, true);
        log.info("[getApprovalDetail] 历史任务列表: processInstanceId={}, count={}", processInstanceId, historicTasks.size());
        for (HistoricTaskInstance task : historicTasks) {
            log.info("[getApprovalDetail] 历史任务: id={}, name={}, taskDefinitionKey={}, startTime={}, endTime={}",
                    task.getId(), task.getName(), task.getTaskDefinitionKey(), task.getStartTime(), task.getEndTime());
        }
        List<BpmApprovalDetailRespVO.ActivityNodeTask> historicTaskVos = BeanUtils.toBean(historicTasks,
                BpmApprovalDetailRespVO.ActivityNodeTask.class);

        // 4.3.1 从历史任务的本地变量中提取 reason、assigneeUser、ownerUser
        for (int i = 0; i < historicTasks.size(); i++) {
            HistoricTaskInstance historicTask = historicTasks.get(i);
            BpmApprovalDetailRespVO.ActivityNodeTask taskVo = historicTaskVos.get(i);
            if (taskVo != null && historicTask.getTaskLocalVariables() != null) {
                // 提取 reason
                Object reasonObj = historicTask.getTaskLocalVariables().get("reason");
                if (reasonObj != null) {
                    taskVo.setReason(String.valueOf(reasonObj));
                }

                // 从任务变量中提取 assigneeUser（JSON 格式）
                Object assigneeUserObj = historicTask.getTaskLocalVariables().get("assigneeUser");
                if (assigneeUserObj != null) {
                    try {
                        JSONObject assigneeUserJson = JSONUtil.parseObj(String.valueOf(assigneeUserObj));
                        UserSimpleBaseVO assigneeUser = new UserSimpleBaseVO();
                        assigneeUser.setId(assigneeUserJson.getLong("id"));
                        assigneeUser.setNickname(assigneeUserJson.getStr("nickname"));
                        assigneeUser.setAvatar(assigneeUserJson.getStr("avatar"));
                        assigneeUser.setDeptId(assigneeUserJson.getLong("deptId"));
                        taskVo.setAssigneeUser(assigneeUser);
                        // 同时设置 assignee ID，方便后续处理
                        if (assigneeUser.getId() != null) {
                            taskVo.setAssignee(assigneeUser.getId());
                        }
                    } catch (Exception e) {
                        log.warn("[getApprovalDetail] 解析 assigneeUser 失败: {}", e.getMessage());
                    }
                }

                // 从任务变量中提取 ownerUser（JSON 格式）
                Object ownerUserObj = historicTask.getTaskLocalVariables().get("ownerUser");
                if (ownerUserObj != null) {
                    try {
                        JSONObject ownerUserJson = JSONUtil.parseObj(String.valueOf(ownerUserObj));
                        UserSimpleBaseVO ownerUser = new UserSimpleBaseVO();
                        ownerUser.setId(ownerUserJson.getLong("id"));
                        ownerUser.setNickname(ownerUserJson.getStr("nickname"));
                        ownerUser.setAvatar(ownerUserJson.getStr("avatar"));
                        ownerUser.setDeptId(ownerUserJson.getLong("deptId"));
                        taskVo.setOwnerUser(ownerUser);
                        // 同时设置 owner ID，方便后续处理
                        if (ownerUser.getId() != null) {
                            taskVo.setOwner(ownerUser.getId());
                        }
                    } catch (Exception e) {
                        log.warn("[getApprovalDetail] 解析 ownerUser 失败: {}", e.getMessage());
                    }
                }
            }
        }

        // 4.3.2 查询用户信息，填充 ownerUser 和 assigneeUser（历史任务）
        Set<Long> userIds = historicTasks.stream()
                .filter(t -> t.getAssignee() != null)
                .map(t -> Long.parseLong(t.getAssignee()))
                .collect(Collectors.toSet());
        userIds.addAll(historicTasks.stream()
                .filter(t -> t.getOwner() != null)
                .map(t -> Long.parseLong(t.getOwner()))
                .collect(Collectors.toSet()));

        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                userMap.values().stream()
                        .map(AdminUserRespDTO::getDeptId)
                        .collect(Collectors.toSet()));

        // 填充历史任务的 ownerUser 和 assigneeUser
        for (BpmApprovalDetailRespVO.ActivityNodeTask taskVo : historicTaskVos) {
            if (taskVo.getAssignee() != null) {
                AdminUserRespDTO user = userMap.get(taskVo.getAssignee());
                if (user != null) {
                    UserSimpleBaseVO userVo = new UserSimpleBaseVO();
                    userVo.setId(user.getId());
                    userVo.setNickname(user.getNickname());
                    userVo.setAvatar(user.getAvatar());
                    DeptRespDTO dept = deptMap.get(user.getDeptId());
                    if (dept != null) {
                        userVo.setDeptName(dept.getName());
                    }
                    taskVo.setAssigneeUser(userVo);
                }
            }
            if (taskVo.getOwner() != null) {
                AdminUserRespDTO user = userMap.get(taskVo.getOwner());
                if (user != null) {
                    UserSimpleBaseVO userVo = new UserSimpleBaseVO();
                    userVo.setId(user.getId());
                    userVo.setNickname(user.getNickname());
                    userVo.setAvatar(user.getAvatar());
                    DeptRespDTO dept = deptMap.get(user.getDeptId());
                    if (dept != null) {
                        userVo.setDeptName(dept.getName());
                    }
                    taskVo.setOwnerUser(userVo);
                }
            }
        }

        // 4.4 获取当前用户的待办任务
        BpmApprovalDetailRespVO.ActivityNodeTask currentTaskVo = null;
        BpmTaskRespVO todoTask = null;
        if (reqDTO.getTaskId() != null) {
            // 通过 taskId 获取
            Task currentTask = taskService.getTask(reqDTO.getTaskId());
            if (currentTask != null) {
                currentTaskVo = BeanUtils.toBean(currentTask, BpmApprovalDetailRespVO.ActivityNodeTask.class);
                // 填充当前任务的 ownerUser 和 assigneeUser
                if (currentTaskVo.getAssignee() != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(currentTaskVo.getAssignee());
                    if (user != null) {
                        UserSimpleBaseVO userVo = new UserSimpleBaseVO();
                        userVo.setId(user.getId());
                        userVo.setNickname(user.getNickname());
                        userVo.setAvatar(user.getAvatar());
                        DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                        if (dept != null) {
                            userVo.setDeptName(dept.getName());
                        }
                        currentTaskVo.setAssigneeUser(userVo);
                    }
                }
                if (currentTaskVo.getOwner() != null) {
                    AdminUserRespDTO user = adminUserApi.getUser(currentTaskVo.getOwner());
                    if (user != null) {
                        UserSimpleBaseVO userVo = new UserSimpleBaseVO();
                        userVo.setId(user.getId());
                        userVo.setNickname(user.getNickname());
                        userVo.setAvatar(user.getAvatar());
                        DeptRespDTO dept = deptApi.getDept(user.getDeptId());
                        if (dept != null) {
                            userVo.setDeptName(dept.getName());
                        }
                        currentTaskVo.setOwnerUser(userVo);
                    }
                }
                // 转换为 todoTask
                todoTask = new BpmTaskRespVO();
                todoTask.setId(currentTask.getId());
                todoTask.setName(currentTask.getName());
                todoTask.setTaskDefinitionKey(currentTask.getTaskDefinitionKey());
                // 将 assignee 从 String 转换为 Long
                if (currentTask.getAssignee() != null) {
                    todoTask.setAssignee(Long.parseLong(currentTask.getAssignee()));
                }
            }
        }

        // 4.5 构建 DSL 审批详情节点列表
        List<BpmApprovalDetailRespVO.ActivityNode> activityNodes = dslApprovalDetailBuilder.buildActivityNodes(
                businessProcess, bpmnModel, historicTaskVos, currentTaskVo);

        // 4.6 如果 DSL 构建失败，回退到原有实现
        if (activityNodes == null || activityNodes.isEmpty()) {
            log.info("[getApprovalDetail] DSL 构建节点列表为空，回退到原有实现");
            return fallbackToOriginal(userId, reqDTO);
        }

        // 4.7 构建返回结果
        BpmApprovalDetailRespVO respVO = new BpmApprovalDetailRespVO();
        respVO.setActivityNodes(activityNodes);
        respVO.setTodoTask(todoTask);
        respVO.setStatus(1); // 进行中

        // 4.8 设置流程实例信息（供前端获取 processInstanceId 调用任务列表接口）
        BpmProcessInstanceRespVO processInstanceVO = new BpmProcessInstanceRespVO();
        processInstanceVO.setId(processInstanceId);
        respVO.setProcessInstance(processInstanceVO);

        log.info("[getApprovalDetail] DSL 构建审批详情成功: processInstanceId={}, 节点数={}", processInstanceId, activityNodes.size());

        return convertToDTO(respVO);
    }

    /**
     * 将 RespVO 转换为 DTO
     */
    private BpmApprovalDetailRespDTO convertToDTO(BpmApprovalDetailRespVO respVO) {
        if (respVO == null) {
            return null;
        }

        BpmApprovalDetailRespDTO dto = new BpmApprovalDetailRespDTO();
        BeanUtils.copyProperties(respVO, dto);
        return dto;
    }

    /**
     * 回退到原有实现
     */
    private BpmApprovalDetailRespDTO fallbackToOriginal(Long userId, BpmApprovalDetailReqDTO reqDTO) {
        // 确保 processInstanceId 有值（如果只有 taskId，需要通过 task 获取 processInstanceId）
        if (StrUtil.isBlank(reqDTO.getProcessInstanceId()) && StrUtil.isNotBlank(reqDTO.getTaskId())) {
            Task task = taskService.getTask(reqDTO.getTaskId());
            if (task != null) {
                reqDTO.setProcessInstanceId(task.getProcessInstanceId());
            }
        }

        // 将 DTO 转换为 VO
        BpmApprovalDetailReqVO reqVO = BeanUtils.toBean(reqDTO, BpmApprovalDetailReqVO.class);

        // 调用原有实现
        BpmApprovalDetailRespVO respVO = processInstanceService.getApprovalDetail(userId, reqVO);

        // 将 VO 转换为 DTO
        return convertToDTO(respVO);
    }

}
