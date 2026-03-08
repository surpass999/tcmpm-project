package cn.gemrun.base.module.declare.controller.admin.expert;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.expert.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.expert.ExpertDO;
import cn.gemrun.base.module.declare.service.expert.ExpertService;
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

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 专家管理")
@RestController
@RequestMapping("/declare/expert")
@Validated
public class ExpertController {

    @Resource
    private ExpertService expertService;

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
    public CommonResult<List<ExpertRespVO>> getExpertSimpleList() {
        // 获取所有在册的专家
        ExpertListReqVO reqVO = new ExpertListReqVO();
        reqVO.setStatus(1); // 在册状态
        List<ExpertDO> list = expertService.getExpertList(reqVO);
        return success(BeanUtils.toBean(list, ExpertRespVO.class));
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
