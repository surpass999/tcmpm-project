package cn.gemrun.base.module.declare.controller.admin.project;

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

import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.service.project.RectificationService;

/**
 * 整改记录 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 整改记录")
@RestController
@RequestMapping("/declare/rectification")
@Validated
public class RectificationController {

    @Resource
    private RectificationService rectificationService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建整改记录")
    @PreAuthorize("@ss.hasPermission('declare:rectification:create')")
    public CommonResult<Long> createRectification(@Valid @RequestBody RectificationSaveReqVO createReqVO) {
        return success(rectificationService.createRectification(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新整改记录")
    @PreAuthorize("@ss.hasPermission('declare:rectification:update')")
    public CommonResult<Boolean> updateRectification(@Valid @RequestBody RectificationSaveReqVO updateReqVO) {
        rectificationService.updateRectification(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除整改记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:rectification:delete')")
    public CommonResult<Boolean> deleteRectification(@RequestParam("id") Long id) {
        rectificationService.deleteRectification(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除整改记录")
    @PreAuthorize("@ss.hasPermission('declare:rectification:delete')")
    public CommonResult<Boolean> deleteRectificationListByIds(@RequestParam("ids") Set<Long> ids) {
        rectificationService.deleteRectificationListByIds(ids);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得整改记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:rectification:query')")
    public CommonResult<RectificationRespVO> getRectification(@RequestParam("id") Long id) {
        return success(rectificationService.getRectification(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得整改记录分页")
    @PreAuthorize("@ss.hasPermission('declare:rectification:query')")
    public CommonResult<PageResult<RectificationRespVO>> getRectificationPage(@Valid RectificationPageReqVO pageReqVO) {
        PageResult<RectificationRespVO> pageResult = rectificationService.getRectificationPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID查询整改记录列表")
    @Parameter(name = "projectId", description = "项目ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:rectification:query')")
    public CommonResult<List<RectificationRespVO>> getRectificationListByProjectId(@RequestParam("projectId") Long projectId) {
        return success(rectificationService.getRectificationListByProjectId(projectId));
    }

    @GetMapping("/list-by-process")
    @Operation(summary = "根据过程记录ID查询整改记录列表")
    @Parameter(name = "processId", description = "过程记录ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:rectification:query')")
    public CommonResult<List<RectificationRespVO>> getRectificationListByProcessId(@RequestParam("processId") Long processId) {
        return success(rectificationService.getRectificationListByProcessId(processId));
    }

}
