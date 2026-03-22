package cn.gemrun.base.module.declare.controller.admin.training;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.training.vo.*;
import cn.gemrun.base.module.declare.service.training.TrainingService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 用户端 - 培训活动 Controller
 *
 * @author Gemini
 */
@Tag(name = "用户端 - 培训活动")
@RestController
@RequestMapping("/client/training")
@Validated
@RequiredArgsConstructor
public class TrainingClientController {

    private final TrainingService trainingService;
    private final AdminUserApi adminUserApi;

    @GetMapping("/list")
    @Operation(summary = "获取已发布的活动列表")
    public CommonResult<List<TrainingRespVO>> getPublishedList() {
        return success(trainingService.getPublishedTrainingList());
    }

    @GetMapping("/get")
    @Operation(summary = "获取活动详情")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    public CommonResult<TrainingRespVO> getPublishedTraining(@RequestParam("id") Long id) {
        return success(trainingService.getPublishedTraining(id));
    }

    @PostMapping("/register")
    @Operation(summary = "报名参加活动")
    public CommonResult<Boolean> register(@Validated @RequestBody RegisterReqVO reqVO) {
        Long userId = 1L;
        String userName = adminUserApi.getUser(userId) != null ? adminUserApi.getUser(userId).getNickname() : null;
        trainingService.registerTraining(reqVO, userId, userName);
        return success(true);
    }

    @DeleteMapping("/cancel")
    @Operation(summary = "取消报名")
    @Parameter(name = "trainingId", description = "活动ID", required = true, example = "1")
    public CommonResult<Boolean> cancelRegistration(@RequestParam("trainingId") Long trainingId) {
        // TODO: 获取当前登录用户ID
        Long userId = 1L;
        trainingService.cancelRegistration(trainingId, userId);
        return success(true);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我的报名列表")
    public CommonResult<PageResult<RegistrationRespVO>> getMyRegistrations(
            @ModelAttribute TrainingPageReqVO pageReqVO) {
        // TODO: 获取当前登录用户ID
        Long userId = 1L;
        return success(trainingService.getMyRegistrations(userId, pageReqVO));
    }

    @PutMapping("/feedback")
    @Operation(summary = "提交反馈评分")
    public CommonResult<Boolean> submitFeedback(@Validated @RequestBody FeedbackReqVO reqVO) {
        // TODO: 获取当前登录用户ID
        Long userId = 1L;
        trainingService.submitFeedback(reqVO, userId);
        return success(true);
    }

}
