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
import cn.gemrun.base.module.declare.service.project.ProjectService;

/**
 * 已立项项目核心信息 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 已立项项目核心信息")
@RestController
@RequestMapping("/declare/project")
@Validated
public class ProjectController {

    @Resource
    private ProjectService projectService;

    // ========== 基础操作 ==========

    @PostMapping("/create")
    @Operation(summary = "创建已立项项目核心信息")
    @PreAuthorize("@ss.hasPermission('declare:project:create')")
    public CommonResult<Long> createProject(@Valid @RequestBody ProjectSaveReqVO createReqVO) {
        return success(projectService.createProject(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新已立项项目核心信息")
    @PreAuthorize("@ss.hasPermission('declare:project:update')")
    public CommonResult<Boolean> updateProject(@Valid @RequestBody ProjectSaveReqVO updateReqVO) {
        projectService.updateProject(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除已立项项目核心信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project:delete')")
    public CommonResult<Boolean> deleteProject(@RequestParam("id") Long id) {
        projectService.deleteProject(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除已立项项目核心信息")
    @PreAuthorize("@ss.hasPermission('declare:project:delete')")
    public CommonResult<Boolean> deleteProjectListByIds(@RequestParam("ids") Set<Long> ids) {
        projectService.deleteProjectListByIds(ids);
        return success(true);
    }

    // ========== 查询操作 ==========

    @GetMapping("/get")
    @Operation(summary = "获得已立项项目核心信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project:query')")
    public CommonResult<ProjectRespVO> getProject(@RequestParam("id") Long id) {
        return success(projectService.getProject(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得已立项项目核心信息分页")
    @PreAuthorize("@ss.hasPermission('declare:project:query')")
    public CommonResult<PageResult<ProjectRespVO>> getProjectPage(@Valid ProjectPageReqVO pageReqVO) {
        PageResult<ProjectRespVO> pageResult = projectService.getProjectPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/list-by-filing")
    @Operation(summary = "根据备案ID查询项目列表")
    @Parameter(name = "filingId", description = "备案ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:project:query')")
    public CommonResult<List<ProjectRespVO>> getProjectListByFilingId(@RequestParam("filingId") Long filingId) {
        return success(projectService.getProjectListByFilingId(filingId));
    }

}
