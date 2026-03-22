package cn.gemrun.base.module.declare.controller.admin.review;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import java.util.*;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewTaskDO;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.service.review.ReviewTaskService;
import cn.gemrun.base.module.declare.service.review.ReviewResultService;

/**
 * 评审任务 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 评审任务")
@RestController
@RequestMapping("/declare/review-task")
@Validated
public class ReviewTaskController {

    @Resource
    private ReviewTaskService reviewTaskService;

    @Resource
    private ReviewResultService reviewResultService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建评审任务")
    @PreAuthorize("@ss.hasPermission('declare:review-task:create')")
    public CommonResult<Long> createReviewTask(@Valid @RequestBody ReviewTaskSaveReqVO createReqVO) {
        return success(reviewTaskService.createReviewTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新评审任务")
    @PreAuthorize("@ss.hasPermission('declare:review-task:update')")
    public CommonResult<Boolean> updateReviewTask(@Valid @RequestBody ReviewTaskSaveReqVO updateReqVO) {
        reviewTaskService.updateReviewTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除评审任务")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:review-task:delete')")
    public CommonResult<Boolean> deleteReviewTask(@RequestParam("id") Long id) {
        reviewTaskService.deleteReviewTask(id);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得评审任务")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<ReviewTaskRespVO> getReviewTask(@RequestParam("id") Long id) {
        ReviewTaskDO reviewTask = reviewTaskService.getReviewTask(id);
        ReviewTaskRespVO respVO = BeanUtils.toBean(reviewTask, ReviewTaskRespVO.class);
        return success(respVO);
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得评审任务详情（含扩展信息）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<ReviewTaskRespVO> getReviewTaskDetail(@RequestParam("id") Long id) {
        return success(reviewTaskService.getReviewTaskDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得评审任务分页")
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<PageResult<ReviewTaskRespVO>> getReviewTaskPage(@Valid ReviewTaskPageReqVO pageReqVO) {
        PageResult<ReviewTaskRespVO> pageResult = reviewTaskService.getReviewTaskPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-business")
    @Operation(summary = "根据业务ID获取评审任务")
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<List<ReviewTaskRespVO>> getReviewTaskByBusiness(
            @RequestParam("businessType") Integer businessType,
            @RequestParam("businessId") Long businessId) {
        List<ReviewTaskDO> list = reviewTaskService.getReviewTaskByBusiness(businessType, businessId);
        List<ReviewTaskRespVO> voList = BeanUtils.toBean(list, ReviewTaskRespVO.class);
        return success(voList);
    }

    // ========== 专家分配操作 ==========

    @PutMapping("/assign-experts")
    @Operation(summary = "分配评审专家")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @Parameter(name = "expertIds", description = "专家ID列表（逗号分隔）", required = true)
    @PreAuthorize("@ss.hasPermission('declare:review-task:assign')")
    public CommonResult<Boolean> assignExperts(
            @RequestParam("taskId") Long taskId,
            @RequestParam("expertIds") String expertIds) {
        reviewTaskService.assignExperts(taskId, expertIds);
        return success(true);
    }

    // ========== 评审结果操作 ==========

    @GetMapping("/result/page")
    @Operation(summary = "获得评审结果分页")
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<PageResult<ReviewResultRespVO>> getReviewResultPage(@Valid ReviewResultPageReqVO pageReqVO) {
        PageResult<ReviewResultRespVO> pageResult = reviewResultService.getReviewResultPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/result/list-by-task")
    @Operation(summary = "获取任务的评审结果列表")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('declare:review-task:query')")
    public CommonResult<List<ReviewResultRespVO>> getReviewResultByTaskId(@RequestParam("taskId") Long taskId) {
        List<ReviewResultDO> list = reviewResultService.getReviewResultByTaskId(taskId);
        List<ReviewResultRespVO> voList = BeanUtils.toBean(list, ReviewResultRespVO.class);
        return success(voList);
    }

}
