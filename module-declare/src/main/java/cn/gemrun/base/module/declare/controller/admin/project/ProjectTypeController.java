package cn.gemrun.base.module.declare.controller.admin.project;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.project.vo.*;
import cn.gemrun.base.module.declare.service.project.ProjectTypeService;
import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 项目类型Controller
 *
 * @author Gemini
 */
@RestController
@RequestMapping("/declare/project-type")
@Validated
public class ProjectTypeController {

    @Resource
    private ProjectTypeService projectTypeService;

    /**
     * 创建项目类型
     */
    @PostMapping("/create")
    public CommonResult<Long> createProjectType(@Valid @RequestBody ProjectTypeCreateReqVO reqVO) {
        return success(projectTypeService.createProjectType(reqVO));
    }

    /**
     * 更新项目类型
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateProjectType(@Valid @RequestBody ProjectTypeUpdateReqVO reqVO) {
        projectTypeService.updateProjectType(reqVO);
        return success(true);
    }

    /**
     * 删除项目类型
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteProjectType(@RequestParam("id") Long id) {
        projectTypeService.deleteProjectType(id);
        return success(true);
    }

    /**
     * 获取项目类型详情
     */
    @GetMapping("/get")
    public CommonResult<ProjectTypeVO> getProjectType(@RequestParam("id") Long id) {
        return success(projectTypeService.getProjectType(id));
    }

    /**
     * 获取项目类型分页列表
     */
    @GetMapping("/page")
    public CommonResult<PageResult<ProjectTypeVO>> getProjectTypePage(@Valid ProjectTypePageReqVO reqVO) {
        return success(projectTypeService.getProjectTypePage(reqVO));
    }

    /**
     * 获取项目类型列表（全部，启用状态）
     */
    @GetMapping("/list")
    public CommonResult<List<ProjectTypeVO>> getProjectTypeList() {
        return success(projectTypeService.getProjectTypeList());
    }

    /**
     * 获取项目类型简化列表（用于下拉选择）
     */
    @GetMapping("/simple-list")
    public CommonResult<List<ProjectTypeVO>> getProjectTypeSimpleList() {
        return success(projectTypeService.getProjectTypeSimpleList());
    }

}
