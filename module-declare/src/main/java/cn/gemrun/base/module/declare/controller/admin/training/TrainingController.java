package cn.gemrun.base.module.declare.controller.admin.training;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.training.vo.*;
import cn.gemrun.base.module.declare.service.training.TrainingService;
import cn.gemrun.base.module.declare.service.training.TrainingStatisticsRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 培训活动管理 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 培训活动管理")
@RestController
@RequestMapping("/declare/training")
@Validated
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/create")
    @Operation(summary = "创建培训活动")
    @PreAuthorize("@ss.hasPermission('declare:training:create')")
    public CommonResult<Long> createTraining(@Validated @RequestBody TrainingSaveReqVO createReqVO) {
        return success(trainingService.createTraining(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新培训活动")
    @PreAuthorize("@ss.hasPermission('declare:training:update')")
    public CommonResult<Boolean> updateTraining(@Validated @RequestBody TrainingSaveReqVO updateReqVO) {
        trainingService.updateTraining(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除培训活动")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('declare:training:delete')")
    public CommonResult<Boolean> deleteTraining(@RequestParam("id") Long id) {
        trainingService.deleteTraining(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取培训活动详情")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    public CommonResult<TrainingRespVO> getTraining(@RequestParam("id") Long id) {
        return success(trainingService.getTraining(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取培训活动分页列表")
    public CommonResult<PageResult<TrainingRespVO>> getTrainingPage(@Validated TrainingPageReqVO pageReqVO) {
        return success(trainingService.getTrainingPage(pageReqVO));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布培训活动")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('declare:training:publish')")
    public CommonResult<Boolean> publishTraining(@RequestParam("id") Long id) {
        trainingService.publishTraining(id);
        return success(true);
    }

    @PostMapping("/unpublish")
    @Operation(summary = "取消发布培训活动")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('declare:training:publish')")
    public CommonResult<Boolean> unpublishTraining(@RequestParam("id") Long id) {
        trainingService.unpublishTraining(id);
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取活动统计数据")
    @Parameter(name = "id", description = "活动ID", required = true, example = "1")
    public CommonResult<TrainingStatisticsRespVO> getStatistics(@RequestParam("id") Long id) {
        return success(trainingService.getStatistics(id));
    }

}
