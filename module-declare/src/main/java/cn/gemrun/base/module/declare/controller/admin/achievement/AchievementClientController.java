package cn.gemrun.base.module.declare.controller.admin.achievement;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.AchievementRespVO;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 用户端 - 成果与推广 Controller
 *
 * @author Gemini
 */
@Tag(name = "用户端 - 成果与推广")
@RestController
@RequestMapping("/client/achievement")
@Validated
@RequiredArgsConstructor
public class AchievementClientController {

    private final AchievementService achievementService;

    @GetMapping("/list")
    @Operation(summary = "获取已发布/已推荐的成果列表")
    public CommonResult<List<AchievementRespVO>> getPublishedList() {
        return success(achievementService.getPublishedAchievementList());
    }

    @GetMapping("/get")
    @Operation(summary = "获取成果详情")
    @Parameter(name = "id", description = "成果ID", required = true, example = "1")
    public CommonResult<AchievementRespVO> getPublishedAchievement(@RequestParam("id") Long id) {
        return success(achievementService.getPublishedAchievement(id));
    }

}
