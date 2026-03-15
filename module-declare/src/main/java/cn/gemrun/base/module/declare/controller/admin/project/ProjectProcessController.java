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
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;

/**
 * 项目过程记录 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 项目过程记录")
@RestController
@RequestMapping("/declare/project-process")
@Validated
public class ProjectProcessController {

    @Resource
    private ProjectProcessService projectProcessService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建项目过程记录")
    @PreAuthorize("@ss.hasPermission('declare:project-process:create')")
    public CommonResult<Long> createProjectProcess(@Valid @RequestBody ProjectProcessSaveReqVO createReqVO) {
        return success(projectProcessService.createProjectProcess(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目过程记录")
    @PreAuthorize("@ss.hasPermission('declare:project-process:update')")
    public CommonResult<Boolean> updateProjectProcess(@Valid @RequestBody ProjectProcessSaveReqVO updateReqVO) {
        projectProcessService.updateProjectProcess(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目过程记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:delete')")
    public CommonResult<Boolean> deleteProjectProcess(@RequestParam("id") Long id) {
        projectProcessService.deleteProjectProcess(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除项目过程记录")
    @PreAuthorize("@ss.hasPermission('declare:project-process:delete')")
    public CommonResult<Boolean> deleteProjectProcessListByIds(@RequestParam("ids") Set<Long> ids) {
        projectProcessService.deleteProjectProcessListByIds(ids);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得项目过程记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:query')")
    public CommonResult<ProjectProcessRespVO> getProjectProcess(@RequestParam("id") Long id) {
        return success(projectProcessService.getProjectProcess(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目过程记录分页")
    @PreAuthorize("@ss.hasPermission('declare:project-process:query')")
    public CommonResult<PageResult<ProjectProcessRespVO>> getProjectProcessPage(@Valid ProjectProcessPageReqVO pageReqVO) {
        PageResult<ProjectProcessRespVO> pageResult = projectProcessService.getProjectProcessPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID查询过程记录列表")
    @Parameter(name = "projectId", description = "项目ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project-process:query')")
    public CommonResult<List<ProjectProcessRespVO>> getProjectProcessListByProjectId(@RequestParam("projectId") Long projectId) {
        return success(projectProcessService.getProjectProcessListByProjectId(projectId));
    }

}
