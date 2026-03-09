package cn.gemrun.base.module.bpm.api.task;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailReqDTO;
import cn.gemrun.base.module.bpm.api.task.dto.BpmApprovalDetailRespDTO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.dal.mysql.process.BpmBusinessProcessMapper;
import cn.gemrun.base.module.bpm.framework.dsl.DslApprovalDetailBuilder;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.service.definition.BpmModelService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
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
    private BpmBusinessProcessMapper businessProcessMapper;
    @Resource
    private BpmModelService modelService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private DslApprovalDetailBuilder dslApprovalDetailBuilder;

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
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        if (processInstance == null) {
            log.warn("[getApprovalDetail] 流程实例不存在: processInstanceId={}", processInstanceId);
            return fallbackToOriginal(userId, reqDTO);
        }

        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(processInstance.getProcessDefinitionId());
        if (bpmnModel == null) {
            log.warn("[getApprovalDetail] 获取 BpmnModel 失败: processDefinitionId={}", processInstance.getProcessDefinitionId());
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
        List<BpmApprovalDetailRespVO.ActivityNodeTask> historicTaskVos = BeanUtils.toBean(historicTasks,
                BpmApprovalDetailRespVO.ActivityNodeTask.class);

        // 4.4 获取当前用户的待办任务
        BpmApprovalDetailRespVO.ActivityNodeTask currentTaskVo = null;
        BpmTaskRespVO todoTask = null;
        if (reqDTO.getTaskId() != null) {
            // 通过 taskId 获取
            Task currentTask = taskService.getTask(reqDTO.getTaskId());
            if (currentTask != null) {
                currentTaskVo = BeanUtils.toBean(currentTask, BpmApprovalDetailRespVO.ActivityNodeTask.class);
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
