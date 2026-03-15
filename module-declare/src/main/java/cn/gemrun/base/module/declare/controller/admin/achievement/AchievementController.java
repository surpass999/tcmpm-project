package cn.gemrun.base.module.declare.controller.admin.achievement;

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

import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;

/**
 * 成果信息 Controller
 *
 * @author
 */
@Tag(name = "管理后台 - 成果信息")
@RestController
@RequestMapping("/declare/achievement")
@Validated
public class AchievementController {

    @Resource
    private AchievementService achievementService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建成果信息")
    @PreAuthorize("@ss.hasPermission('declare:achievement:create')")
    public CommonResult<Long> createAchievement(@Valid @RequestBody AchievementSaveReqVO createReqVO) {
        return success(achievementService.createAchievement(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新成果信息")
    @PreAuthorize("@ss.hasPermission('declare:achievement:update')")
    public CommonResult<Boolean> updateAchievement(@Valid @RequestBody AchievementSaveReqVO updateReqVO) {
        achievementService.updateAchievement(updateReqVO);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交成果审核")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> submitAchievement(
            @RequestParam("id") Long id,
            @RequestParam(value = "processDefinitionKey", required = false, defaultValue = "declare_achievement") String processDefinitionKey) {
        String processInstanceId = achievementService.submitAchievement(id, processDefinitionKey);
        return success(processInstanceId);
    }

    @PostMapping("/recommend")
    @Operation(summary = "推荐成果至国家局")
    @PreAuthorize("@ss.hasPermission('declare:achievement:recommend')")
    public CommonResult<Boolean> recommendToNation(
            @RequestParam("id") Long id,
            @RequestParam(value = "opinion", required = false, defaultValue = "") String opinion) {
        achievementService.recommendToNation(id, opinion);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得成果信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:achievement:query')")
    public CommonResult<AchievementRespVO> getAchievement(@RequestParam("id") Long id) {
        AchievementDO achievement = achievementService.getAchievement(id);
        AchievementRespVO respVO = BeanUtils.toBean(achievement, AchievementRespVO.class);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得成果信息分页")
    @PreAuthorize("@ss.hasPermission('declare:achievement:query')")
    public CommonResult<PageResult<AchievementRespVO>> getAchievementPage(@Valid AchievementPageReqVO pageReqVO) {
        PageResult<AchievementDO> pageResult = achievementService.getAchievementPage(pageReqVO);
        List<AchievementRespVO> voList = BeanUtils.toBean(pageResult.getList(), AchievementRespVO.class);
        return success(new PageResult<>(voList, pageResult.getTotal()));
    }

    // ========== 删除操作 ==========

    @DeleteMapping("/delete")
    @Operation(summary = "删除成果信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('declare:achievement:delete')")
    public CommonResult<Boolean> deleteAchievement(@RequestParam("id") Long id) {
        achievementService.deleteAchievement(id);
        return success(true);
    }

}
