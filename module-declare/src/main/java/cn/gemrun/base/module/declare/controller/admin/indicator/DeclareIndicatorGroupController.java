package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 指标分组管理 Controller
 *
 * @author Gemini
 */
@Validated
@RestController
@RequestMapping("/declare/indicator-group")
public class DeclareIndicatorGroupController {

    @Resource
    private DeclareIndicatorGroupService groupService;

    /**
     * 创建分组
     */
    @PostMapping("/create")
    public CommonResult<Long> createGroup(@Valid @RequestBody DeclareIndicatorGroupSaveReqVO createReqVO) {
        return success(groupService.createGroup(createReqVO));
    }

    /**
     * 更新分组
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateGroup(@Valid @RequestBody DeclareIndicatorGroupSaveReqVO updateReqVO) {
        groupService.updateGroup(updateReqVO);
        return success(true);
    }

    /**
     * 删除分组
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteGroup(@RequestParam("id") Long id) {
        groupService.deleteGroup(id);
        return success(true);
    }

    /**
     * 获取分组
     */
    @GetMapping("/get")
    public CommonResult<DeclareIndicatorGroupRespVO> getGroup(@RequestParam("id") Long id) {
        return success(groupService.getGroupResp(id));
    }

    /**
     * 获取分组分页
     */
    @GetMapping("/page")
    public CommonResult<PageResult<DeclareIndicatorGroupRespVO>> getGroupPage(@Valid DeclareIndicatorGroupPageReqVO pageReqVO) {
        PageResult<DeclareIndicatorGroupDO> page = groupService.getGroupPage(pageReqVO);
        PageResult<DeclareIndicatorGroupRespVO> voPage = BeanUtils.toBean(page, DeclareIndicatorGroupRespVO.class);
        // 填充项目类型名称
        if (voPage != null && voPage.getList() != null) {
            for (DeclareIndicatorGroupRespVO vo : voPage.getList()) {
                vo.setProjectTypeName(getProjectTypeName(vo.getProjectType()));
            }
        }
        return success(voPage);
    }

    /**
     * 获取树形分组列表
     */
    @GetMapping("/tree")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getGroupTree() {
        return success(groupService.getGroupTree());
    }

    /**
     * 获取所有启用的分组列表（扁平）
     */
    @GetMapping("/list")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getGroupList() {
        List<DeclareIndicatorGroupDO> list = groupService.getAllEnabledList();
        List<DeclareIndicatorGroupRespVO> voList = BeanUtils.toBean(list, DeclareIndicatorGroupRespVO.class);
        // 填充项目类型名称
        for (DeclareIndicatorGroupRespVO vo : voList) {
            vo.setProjectTypeName(getProjectTypeName(vo.getProjectType()));
        }
        return success(voList);
    }

    /**
     * 获取一级分组列表
     * @param projectType 项目类型，可选，用于过滤
     */
    @GetMapping("/list-level-one")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getLevelOneList(
            @RequestParam(value = "projectType", required = false) Integer projectType) {
        List<DeclareIndicatorGroupDO> list = groupService.getLevelOneList(projectType);
        List<DeclareIndicatorGroupRespVO> voList = BeanUtils.toBean(list, DeclareIndicatorGroupRespVO.class);
        // 填充项目类型名称
        for (DeclareIndicatorGroupRespVO vo : voList) {
            vo.setProjectTypeName(getProjectTypeName(vo.getProjectType()));
        }
        return success(voList);
    }

    /**
     * 获取二级分组列表
     */
    @GetMapping("/list-level-two")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getLevelTwoList() {
        List<DeclareIndicatorGroupDO> list = groupService.getLevelTwoList();
        return success(BeanUtils.toBean(list, DeclareIndicatorGroupRespVO.class));
    }

    /**
     * 根据一级分组ID获取二级分组
     */
    @GetMapping("/list-by-parent-id")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getLevelTwoListByParentId(@RequestParam("parentId") Long parentId) {
        List<DeclareIndicatorGroupDO> list = groupService.getLevelTwoListByParentId(parentId);
        return success(BeanUtils.toBean(list, DeclareIndicatorGroupRespVO.class));
    }

    /**
     * 根据项目类型获取一级分组
     */
    @GetMapping("/list-by-project-type")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getLevelOneListByProjectType(@RequestParam("projectType") Integer projectType) {
        List<DeclareIndicatorGroupDO> list = groupService.getLevelOneListByProjectType(projectType);
        List<DeclareIndicatorGroupRespVO> voList = BeanUtils.toBean(list, DeclareIndicatorGroupRespVO.class);
        // 填充项目类型名称
        for (DeclareIndicatorGroupRespVO vo : voList) {
            vo.setProjectTypeName(getProjectTypeName(vo.getProjectType()));
        }
        return success(voList);
    }

    /**
     * 根据项目类型获取树形分组列表
     */
    @GetMapping("/tree-by-project-type")
    public CommonResult<List<DeclareIndicatorGroupRespVO>> getGroupTreeByProjectType(
            @RequestParam(value = "projectType", required = false) Integer projectType) {
        List<DeclareIndicatorGroupRespVO> tree = groupService.getGroupTreeByProjectType(projectType);
        return success(tree);
    }

    /**
     * 获取项目类型名称
     */
    private String getProjectTypeName(Integer projectType) {
        if (projectType == null) {
            return null;
        }
        cn.gemrun.base.module.declare.enums.ProjectTypeEnum projectTypeEnum =
                cn.gemrun.base.module.declare.enums.ProjectTypeEnum.valueOf(projectType);
        return projectTypeEnum != null ? projectTypeEnum.getName() : null;
    }

}
