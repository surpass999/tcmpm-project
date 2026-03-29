package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.task.*;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.bpm.service.task.BpmTaskService;
import cn.gemrun.base.module.bpm.service.definition.BpmModelService;
import cn.gemrun.base.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

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
