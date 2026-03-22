package cn.gemrun.base.module.declare.controller.admin.review;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

import cn.gemrun.base.module.declare.controller.admin.review.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewResultDO;
import cn.gemrun.base.module.declare.service.review.ReviewResultService;

/**
 * 评审结果 Controller（专家端）
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 评审结果（专家端）")
@RestController
@RequestMapping("/declare/review-result")
@Validated
public class ReviewResultController {

    @Resource
    private ReviewResultService reviewResultService;

    // ========== 专家侧操作 ==========

    @GetMapping("/my-tasks")
    @Operation(summary = "获取我的评审任务列表（专家工作台）")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<ReviewResultRespVO>> getMyReviewTasks(ReviewResultPageReqVO pageReqVO) {
        // 从登录信息获取用户ID
        Long userId = getCurrentUserId();
        if (userId == null) {
            return success(new PageResult<>(0L));
        }

        // 直接用用户ID查询（expertId 存的就是用户ID）
        pageReqVO.setExpertId(userId);
        PageResult<ReviewResultRespVO> pageResult = reviewResultService.getReviewResultPage(pageReqVO);
        return success(pageResult);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        return SecurityFrameworkUtils.getLoginUser() != null
                ? SecurityFrameworkUtils.getLoginUser().getId()
                : null;
    }

    @GetMapping("/get")
    @Operation(summary = "获得评审结果")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ReviewResultRespVO> getReviewResult(@RequestParam("id") Long id) {
        ReviewResultDO reviewResult = reviewResultService.getReviewResult(id);
        ReviewResultRespVO respVO = BeanUtils.toBean(reviewResult, ReviewResultRespVO.class);
        return success(respVO);
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得评审结果详情（含专家信息）")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ReviewResultRespVO> getReviewResultDetail(@RequestParam("id") Long id) {
        return success(reviewResultService.getReviewResultDetail(id));
    }

    @GetMapping("/get-by-flowable-task")
    @Operation(summary = "根据Flowable任务ID获取评审结果")
    @Parameter(name = "flowableTaskId", description = "Flowable任务ID", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<ReviewResultRespVO> getReviewResultByFlowableTaskId(@RequestParam("flowableTaskId") String flowableTaskId) {
        ReviewResultDO result = reviewResultService.getReviewResultByFlowableTaskId(flowableTaskId);
        ReviewResultRespVO respVO = BeanUtils.toBean(result, ReviewResultRespVO.class);
        return success(respVO);
    }

    @PostMapping("/receive")
    @Operation(summary = "专家接收评审任务")
    @Parameter(name = "id", description = "评审结果ID", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> receiveTask(@RequestParam("id") Long id) {
        reviewResultService.receiveTask(id);
        return success(true);
    }

    @PostMapping("/start")
    @Operation(summary = "开始评审（专家点击开始评审）")
    @Parameter(name = "id", description = "评审结果ID", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> startReview(@RequestParam("id") Long id) {
        reviewResultService.startReview(id);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交评审结果")
    @Parameter(name = "id", description = "评审结果ID", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> submitReview(
            @RequestParam("id") Long id,
            @RequestBody ReviewResultSaveReqVO saveReqVO) {
        reviewResultService.submitReview(id, saveReqVO);
        return success(true);
    }

    @PostMapping("/apply-avoid")
    @Operation(summary = "专家申请回避")
    @Parameter(name = "id", description = "评审结果ID", required = true)
    @Parameter(name = "reason", description = "回避原因", required = true)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> applyAvoid(
            @RequestParam("id") Long id,
            @RequestParam("reason") String reason) {
        reviewResultService.applyAvoid(id, reason);
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "专家拒绝评审任务")
    @Parameter(name = "id", description = "评审结果ID", required = true)
    @Parameter(name = "reason", description = "拒绝原因")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> rejectTask(
            @RequestParam("id") Long id,
            @RequestParam(value = "reason", required = false) String reason) {
        reviewResultService.rejectTask(id, reason);
        return success(true);
    }

    @GetMapping("/pending-count")
    @Operation(summary = "获取专家待评审数量")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Long> getExpertPendingCount() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return success(0L);
        }

        // 直接用用户ID查询
        return success(reviewResultService.getExpertPendingCount(userId));
    }

    @GetMapping("/list-by-business")
    @Operation(summary = "获取业务下的评审汇总（按流程阶段分组）")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ReviewSummaryRespVO>> getReviewSummaryByBusiness(
            @RequestParam("businessType") Integer businessType,
            @RequestParam("businessId") Long businessId) {
        return success(reviewResultService.getReviewSummaryByBusiness(businessType, businessId));
    }

}
