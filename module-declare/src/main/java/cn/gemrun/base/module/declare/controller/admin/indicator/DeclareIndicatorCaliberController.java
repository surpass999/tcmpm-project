package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorCaliberDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorCaliberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 指标口径管理 Controller
 *
 * @author Gemini
 */
@Tag(name = "管理后台 - 指标口径管理")
@RestController
@RequestMapping("/declare/indicator-caliber")
@Validated
@Slf4j
public class DeclareIndicatorCaliberController {

    @Resource
    private DeclareIndicatorCaliberService caliberService;

    @PostMapping("/create")
    @Operation(summary = "创建指标口径")
    public CommonResult<Long> createCaliber(@Valid @RequestBody DeclareIndicatorCaliberSaveReqVO reqVO) {
        return CommonResult.success(caliberService.createCaliber(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新指标口径")
    public CommonResult<Boolean> updateCaliber(@Valid @RequestBody DeclareIndicatorCaliberSaveReqVO reqVO) {
        caliberService.updateCaliber(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除指标口径")
    public CommonResult<Boolean> deleteCaliber(@RequestParam("id") Long id) {
        caliberService.deleteCaliber(id);
        return CommonResult.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取指标口径")
    public CommonResult<DeclareIndicatorCaliberRespVO> getCaliber(@RequestParam("id") Long id) {
        DeclareIndicatorCaliberDO caliber = caliberService.getCaliber(id);
        return CommonResult.success(BeanUtils.toBean(caliber, DeclareIndicatorCaliberRespVO.class));
    }

    @GetMapping("/get-by-indicator")
    @Operation(summary = "根据指标ID获取口径")
    public CommonResult<DeclareIndicatorCaliberRespVO> getCaliberByIndicatorId(@RequestParam("indicatorId") Long indicatorId) {
        DeclareIndicatorCaliberDO caliber = caliberService.getCaliberByIndicatorId(indicatorId);
        return CommonResult.success(BeanUtils.toBean(caliber, DeclareIndicatorCaliberRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取指标口径分页")
    public CommonResult<PageResult<DeclareIndicatorCaliberRespVO>> getCaliberPage(@Valid DeclareIndicatorCaliberPageReqVO pageReqVO) {
        PageResult<DeclareIndicatorCaliberDO> pageResult = caliberService.getCaliberPage(pageReqVO);
        return CommonResult.success(BeanUtils.toBean(pageResult, DeclareIndicatorCaliberRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取指标口径列表")
    public CommonResult<List<DeclareIndicatorCaliberRespVO>> getCaliberList() {
        List<DeclareIndicatorCaliberDO> list = caliberService.getCaliberList();
        return CommonResult.success(BeanUtils.toBean(list, DeclareIndicatorCaliberRespVO.class));
    }

}
