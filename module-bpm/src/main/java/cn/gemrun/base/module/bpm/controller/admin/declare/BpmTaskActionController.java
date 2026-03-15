package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.*;
import cn.gemrun.base.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.bpm.service.definition.BpmModelService;
import cn.gemrun.base.module.system.service.user.AdminUserService;
import cn.gemrun.base.module.system.dal.dataobject.user.AdminUserDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;
import static cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 流程任务操作 Controller
 * 提供通用的任务操作接口（通过/拒绝/退回/委派/转派/加签/减签/抄送）
 *
 * @author jason
 */
@Tag(name = "管理后台 - 流程任务操作")
@RestController
@RequestMapping("/bpm/declare/task")
@Validated
public class BpmTaskActionController {

    @Resource
    private BpmTaskService taskService;

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private BpmModelService modelService;

    @Resource
    private AdminUserService adminUserService;

    @PutMapping("/approve")
    @Operation(summary = "通过任务")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> approveTask(@Valid @RequestBody BpmTaskApproveReqVO reqVO) {
        taskService.approveTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "拒绝/不通过任务")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> rejectTask(@Valid @RequestBody BpmTaskRejectReqVO reqVO) {
        taskService.rejectTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/return")
    @Operation(summary = "退回任务", description = "将任务退回到指定的节点")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> returnTask(@Valid @RequestBody BpmTaskReturnReqVO reqVO) {
        taskService.returnTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/delegate")
    @Operation(summary = "委派任务", description = "将任务委派给其他人处理，处理完成后回到原审批人")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> delegateTask(@Valid @RequestBody BpmTaskDelegateReqVO reqVO) {
        taskService.delegateTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/transfer")
    @Operation(summary = "转派任务", description = "将任务转派给其他人，转派后不再回到原审批人")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> transferTask(@Valid @RequestBody BpmTaskTransferReqVO reqVO) {
        taskService.transferTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/create-sign")
    @Operation(summary = "加签", description = "在当前任务节点增加审批人，before 前加签，after 后加签")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> createSignTask(@Valid @RequestBody BpmTaskSignCreateReqVO reqVO) {
        taskService.createSignTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @DeleteMapping("/delete-sign")
    @Operation(summary = "减签", description = "减少当前任务节点的审批人")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> deleteSignTask(@Valid @RequestBody BpmTaskSignDeleteReqVO reqVO) {
        taskService.deleteSignTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/copy")
    @Operation(summary = "抄送任务", description = "将任务抄送给指定用户")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> copyTask(@Valid @RequestBody BpmTaskCopyReqVO reqVO) {
        taskService.copyTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/next-assignees")
    @Operation(summary = "设置下一个节点的审批人", description = "用于审批人单独设置下一个节点的审批人（如选择专家），不触发流程流转")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> setNextAssignees(@Valid @RequestBody BpmTaskNextAssigneesReqVO reqVO) {
        taskService.setNextAssignees(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/select-expert")
    @Operation(summary = "选择专家", description = "简化接口，用于审批人选择专家，设置完下一个节点审批人后自动通过当前任务")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> selectExpert(@Valid @RequestBody SelectExpertReqVO reqVO) {
        Long userId = getLoginUserId();

        // 1. 获取任务信息
        Task task = taskService.getTask(reqVO.getId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 2. 获取流程实例
        ProcessInstance instance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        // 3. 获取 BPMN 模型和当前节点
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        org.flowable.bpmn.model.FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());

        // 4. 获取下一个节点列表
        Map<String, Object> variables = instance.getProcessVariables() != null
                ? new HashMap<>(instance.getProcessVariables())
                : new HashMap<>();
        List<FlowNode> nextFlowNodes = BpmnModelUtils.getNextFlowNodes(flowElement, bpmnModel, variables);

        if (nextFlowNodes == null || nextFlowNodes.isEmpty()) {
            throw new RuntimeException("无法获取下一个流程节点");
        }

        // 5. 将选择的专家设置到下一个节点
        Map<String, List<Long>> nextAssignees = new HashMap<>();
        for (FlowNode nextNode : nextFlowNodes) {
            nextAssignees.put(nextNode.getId(), reqVO.getUserIds());
        }

        // 6. 获取用户昵称
        List<AdminUserDO> users = adminUserService.getUserList(reqVO.getUserIds());
        String userNames = users.stream()
                .map(AdminUserDO::getNickname)
                .collect(Collectors.joining(", "));

        // 7. 构建通过任务请求，同时设置下一个节点的审批人
        BpmTaskApproveReqVO approveReqVO = new BpmTaskApproveReqVO();
        approveReqVO.setId(reqVO.getId());
        approveReqVO.setNextAssignees(nextAssignees);
        approveReqVO.setReason("设置专家审批人: " + userNames);
        approveReqVO.setButtonId(reqVO.getButtonId());

        // 8. 调用通过任务接口，完成当前任务并流转到下一个节点
        taskService.approveTask(userId, approveReqVO);

        return success(true);
    }

    @GetMapping("/list-by-return")
    @Operation(summary = "获取所有可退回的节点", description = "用于流程详情中退回按钮，可选择退回的节点")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<BpmTaskRespVO>> getTaskListByReturn(@RequestParam("id") String id) {
        List<org.flowable.bpmn.model.UserTask> userTaskList = taskService.getUserTaskListByReturn(id);
        List<BpmTaskRespVO> result = new java.util.ArrayList<>();
        for (org.flowable.bpmn.model.UserTask userTask : userTaskList) {
            result.add(new BpmTaskRespVO().setName(userTask.getName()).setTaskDefinitionKey(userTask.getId()));
        }
        return success(result);
    }

}
