package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.gemrun.base.framework.common.pojo.CommonResult.success;

/**
 * 指标管理 Controller
 *
 * @author Gemini
 */
@Validated
@RestController
@RequestMapping("/declare/indicator")
public class DeclareIndicatorController {

    @Resource
    private DeclareIndicatorService indicatorService;

    /**
     * 创建指标
     */
    @PostMapping("/create")
    public CommonResult<Long> createIndicator(@Valid @RequestBody DeclareIndicatorSaveReqVO createReqVO) {
        return success(indicatorService.createIndicator(createReqVO));
    }

    /**
     * 更新指标
     */
    @PutMapping("/update")
    public CommonResult<Boolean> updateIndicator(@Valid @RequestBody DeclareIndicatorSaveReqVO updateReqVO) {
        indicatorService.updateIndicator(updateReqVO);
        return success(true);
    }

    /**
     * 删除指标
     */
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteIndicator(@RequestParam("id") Long id) {
        indicatorService.deleteIndicator(id);
        return success(true);
    }

    /**
     * 获取指标
     */
    @GetMapping("/get")
    public CommonResult<DeclareIndicatorRespVO> getIndicator(@RequestParam("id") Long id) {
        DeclareIndicatorDO indicator = indicatorService.getIndicator(id);
        return success(BeanUtils.toBean(indicator, DeclareIndicatorRespVO.class));
    }

    /**
     * 获取指标分页
     */
    @GetMapping("/page")
    public CommonResult<PageResult<DeclareIndicatorRespVO>> getIndicatorPage(@Valid DeclareIndicatorPageReqVO pageReqVO) {
        PageResult<DeclareIndicatorDO> page = indicatorService.getIndicatorPage(pageReqVO);
        return success(BeanUtils.toBean(page, DeclareIndicatorRespVO.class));
    }

    /**
     * 根据项目类型和业务类型获取指标列表（用于动态表单）
     */
    @GetMapping("/list-by-project-type")
    public CommonResult<List<DeclareIndicatorRespVO>> getIndicatorsByProjectType(
            @RequestParam("projectType") Integer projectType,
            @RequestParam("businessType") String businessType) {
        List<DeclareIndicatorDO> list = indicatorService.getIndicatorsByProjectType(projectType, businessType);
        return success(BeanUtils.toBean(list, DeclareIndicatorRespVO.class));
    }

    /**
     * 根据业务类型获取指标列表
     */
    @GetMapping("/list-by-business-type")
    public CommonResult<List<DeclareIndicatorRespVO>> getIndicatorsByBusinessType(@RequestParam("businessType") String businessType) {
        List<DeclareIndicatorDO> list = indicatorService.getIndicatorsByBusinessType(businessType);
        return success(BeanUtils.toBean(list, DeclareIndicatorRespVO.class));
    }

}
