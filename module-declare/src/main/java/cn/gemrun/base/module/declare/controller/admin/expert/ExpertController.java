package cn.gemrun.base.module.declare.controller.admin.expert;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.expert.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
import cn.gemrun.base.module.system.api.user.AdminUserApi;
import cn.gemrun.base.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 专家管理")
@RestController
@RequestMapping("/declare/expert")
@Validated
public class ExpertController {

    @Resource
    private ExpertService expertService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建专家")
    @PreAuthorize("@ss.hasPermission('declare:expert:create')")
    public CommonResult<Long> createExpert(@Valid @RequestBody ExpertSaveReqVO createReqVO) {
        return success(expertService.createExpert(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新专家")
    @PreAuthorize("@ss.hasPermission('declare:expert:update')")
    public CommonResult<Boolean> updateExpert(@Valid @RequestBody ExpertSaveReqVO updateReqVO) {
        expertService.updateExpert(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除专家")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:expert:delete')")
    public CommonResult<Boolean> deleteExpert(@RequestParam("id") Long id) {
        expertService.deleteExpert(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取专家详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('declare:expert:query')")
    public CommonResult<ExpertRespVO> getExpert(@RequestParam("id") Long id) {
        ExpertDO expert = expertService.getExpert(id);
        return success(BeanUtils.toBean(expert, ExpertRespVO.class));
    }

    @GetMapping("/get-by-user")
    @Operation(summary = "根据用户ID获取专家")
    @Parameter(name = "userId", description = "系统用户ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('declare:expert:query')")
    public CommonResult<ExpertRespVO> getExpertByUserId(@RequestParam("userId") Long userId) {
        ExpertDO expert = expertService.getExpertByUserId(userId);
        return success(BeanUtils.toBean(expert, ExpertRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取专家分页列表")
    @PreAuthorize("@ss.hasPermission('declare:expert:query')")
    public CommonResult<PageResult<ExpertRespVO>> getExpertPage(@Valid ExpertPageReqVO pageReqVO) {
        PageResult<ExpertDO> pageResult = expertService.getExpertPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ExpertRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取专家列表")
    @PreAuthorize("@ss.hasPermission('declare:expert:query')")
    public CommonResult<List<ExpertRespVO>> getExpertList(@Valid ExpertListReqVO reqVO) {
        List<ExpertDO> list = expertService.getExpertList(reqVO);
        return success(BeanUtils.toBean(list, ExpertRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获取专家简单列表（下拉选择用）")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<ExpertRespVO>> getExpertSimpleList() {
        // 获取所有在册的专家
        ExpertListReqVO reqVO = new ExpertListReqVO();
        reqVO.setStatus(1); // 在册状态
        List<ExpertDO> list = expertService.getExpertList(reqVO);
        return success(BeanUtils.toBean(list, ExpertRespVO.class));
    }

    @GetMapping("/select-list")
    @Operation(summary = "获取专家选择列表（用于流程选择专家）")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<PageResult<ExpertRespVO>> getExpertSelectList(@Valid ExpertPageReqVO pageReqVO) {
        // 设置默认只查询在册专家
        if (pageReqVO.getStatus() == null) {
            pageReqVO.setStatus(1);
        }
        PageResult<ExpertDO> pageResult = expertService.getExpertSelectList(pageReqVO);
        // 转换为 VO 并设置回避标记
        List<ExpertRespVO> voList = BeanUtils.toBean(pageResult.getList(), ExpertRespVO.class);
        // 设置回避标记
        Long currentDeptId = pageReqVO.getCurrentDeptId();
        if (currentDeptId != null && pageResult.getList() != null) {
            // 获取专家 userId 列表
            List<Long> userIds = pageResult.getList().stream()
                    .map(ExpertDO::getUserId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            if (!userIds.isEmpty()) {
                // 批量查询用户部门信息
                Map<Long, Long> userDeptMap = adminUserApi.getUserList(userIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                AdminUserRespDTO::getId,
                                AdminUserRespDTO::getDeptId,
                                (a, b) -> a
                        ));
                // 设置回避标记
                for (ExpertRespVO vo : voList) {
                    if (vo.getUserId() != null) {
                        Long expertDeptId = userDeptMap.get(vo.getUserId());
                        vo.setIsAvoid(currentDeptId.equals(expertDeptId));
                    }
                }
            }
        }
        return success(new PageResult<>(voList, pageResult.getTotal()));
    }

    @GetMapping("/user-ids")
    @Operation(summary = "获取绑定用户的专家userId列表")
    @PermitAll
    public CommonResult<List<Long>> getExpertUserIds() {
        return success(expertService.getExpertUserIds());
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改专家状态")
    @PreAuthorize("@ss.hasPermission('declare:expert:update')")
    public CommonResult<Boolean> updateExpertStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        expertService.updateExpertStatus(id, status);
        return success(true);
    }

}
