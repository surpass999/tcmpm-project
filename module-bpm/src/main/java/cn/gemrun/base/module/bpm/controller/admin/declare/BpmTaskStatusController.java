package cn.gemrun.base.module.bpm.controller.admin.declare;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskBatchStatusReqVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskBatchStatusRespVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskByBusinessReqVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmTaskByBusinessRespVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmProcessByBusinessReqVO;
import cn.gemrun.base.module.bpm.controller.admin.declare.vo.BpmProcessByBusinessRespVO;
import cn.gemrun.base.module.bpm.service.declare.BpmTaskStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;
import static cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 流程任务状态查询 Controller
 * 用于批量查询当前用户对指定流程实例的任务审批状态
 *
 * @author jason
 */
@Tag(name = "管理后台 - 流程任务状态")
@RestController
@RequestMapping("/bpm/declare/task-status")
@Validated
public class BpmTaskStatusController {

    @Resource
    private BpmTaskStatusService taskStatusService;

    @PostMapping("/batch-query")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "批量查询流程任务状态", description = "根据流程实例ID列表，批量查询当前用户是否有待办任务以及任务详情")
    public CommonResult<List<BpmTaskBatchStatusRespVO>> getTaskBatchStatus(@Valid @RequestBody BpmTaskBatchStatusReqVO reqVO) {
        Long userId = getLoginUserId();
        List<BpmTaskBatchStatusRespVO> result = taskStatusService.getTaskBatchStatus(userId, reqVO.getProcessInstanceIds());
        return success(result);
    }

    @PostMapping("/query-by-business")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据业务ID查询任务状态", description = "根据业务表分类和业务ID列表，查询当前用户是否有待办/已办任务")
    public CommonResult<List<BpmTaskByBusinessRespVO>> getTaskByBusiness(@Valid @RequestBody BpmTaskByBusinessReqVO reqVO) {
        Long userId = getLoginUserId();
        List<BpmTaskByBusinessRespVO> result = taskStatusService.getTaskByBusiness(userId, reqVO.getTableName(), reqVO.getBusinessIds());
        return success(result);
    }

    @PostMapping("/process-query-by-business")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "根据业务ID查询流程信息", description = "根据业务表分类和业务ID，查询所有关联的流程实例及节点信息，按创建时间倒序")
    public CommonResult<List<BpmProcessByBusinessRespVO>> getProcessByBusiness(@Valid @RequestBody BpmProcessByBusinessReqVO reqVO) {
        List<BpmProcessByBusinessRespVO> result = taskStatusService.getProcessByBusiness(reqVO.getTableName(), reqVO.getBusinessId());
        return success(result);
    }

}
