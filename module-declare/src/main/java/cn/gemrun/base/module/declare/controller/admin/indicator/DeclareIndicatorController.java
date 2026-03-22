package cn.gemrun.base.module.declare.controller.admin.indicator;

import cn.gemrun.base.framework.common.pojo.CommonResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorCaliberDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.service.indicator.DeclareIndicatorCaliberService;
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
 */
@Validated
@RestController
@RequestMapping("/declare/indicator")
public class DeclareIndicatorController {

    @Resource
    private DeclareIndicatorService indicatorService;

    @Resource
    private DeclareIndicatorCaliberService caliberService;

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
        DeclareIndicatorRespVO vo = BeanUtils.toBean(indicator, DeclareIndicatorRespVO.class);
        // 填充口径数据 - 通过 indicatorId 直接查询
        if (vo != null) {
            DeclareIndicatorCaliberDO caliber = caliberService.getCaliberByIndicatorId(vo.getId());
            if (caliber != null) {
                vo.setCaliberId(caliber.getId());
                vo.setDefinition(caliber.getDefinition());
                vo.setStatisticScope(caliber.getStatisticScope());
                vo.setDataSource(caliber.getDataSource());
                vo.setFillRequire(caliber.getFillRequire());
                vo.setCalculationExample(caliber.getCalculationExample());
            }
        }
        return success(vo);
    }

    /**
     * 获取指标分页
     */
    @GetMapping("/page")
    public CommonResult<PageResult<DeclareIndicatorRespVO>> getIndicatorPage(@Valid DeclareIndicatorPageReqVO pageReqVO) {
        PageResult<DeclareIndicatorDO> page = indicatorService.getIndicatorPage(pageReqVO);
        PageResult<DeclareIndicatorRespVO> voPage = BeanUtils.toBean(page, DeclareIndicatorRespVO.class);
        // 填充口径数据
        if (voPage != null && voPage.getList() != null) {
            fillCaliberData(voPage.getList());
        }
        return success(voPage);
    }

    /**
     * 根据项目类型和业务类型获取指标列表（用于动态表单）
     */
    @GetMapping("/list-by-project-type")
    public CommonResult<List<DeclareIndicatorRespVO>> getIndicatorsByProjectType(
            @RequestParam("projectType") Integer projectType,
            @RequestParam("businessType") String businessType) {
        List<DeclareIndicatorDO> list = indicatorService.getIndicatorsByProjectType(projectType, businessType);
        List<DeclareIndicatorRespVO> voList = BeanUtils.toBean(list, DeclareIndicatorRespVO.class);
        // 填充口径数据
        fillCaliberData(voList);
        return success(voList);
    }

    /**
     * 根据业务类型获取指标列表
     */
    @GetMapping("/list-by-business-type")
    public CommonResult<List<DeclareIndicatorRespVO>> getIndicatorsByBusinessType(@RequestParam("businessType") String businessType) {
        List<DeclareIndicatorDO> list = indicatorService.getIndicatorsByBusinessType(businessType);
        List<DeclareIndicatorRespVO> voList = BeanUtils.toBean(list, DeclareIndicatorRespVO.class);
        // 填充口径数据
        fillCaliberData(voList);
        return success(voList);
    }

    /**
     * 获取需要在列表显示的指标（showInList=true）
     */
    @GetMapping("/list-for-list-display")
    public CommonResult<List<DeclareIndicatorRespVO>> getIndicatorsForListDisplay(@RequestParam("businessType") String businessType) {
        List<DeclareIndicatorDO> list = indicatorService.getIndicatorsByBusinessType(businessType);
        // 过滤 showInList=true 的指标
        List<DeclareIndicatorDO> listDisplayIndicators = new java.util.ArrayList<>();
        for (DeclareIndicatorDO indicator : list) {
            if (Boolean.TRUE.equals(indicator.getShowInList())) {
                listDisplayIndicators.add(indicator);
            }
        }
        List<DeclareIndicatorRespVO> voList = BeanUtils.toBean(listDisplayIndicators, DeclareIndicatorRespVO.class);
        // 填充口径数据
        fillCaliberData(voList);
        return success(voList);
    }

    /**
     * 填充指标口径数据
     * 优化：通过 indicatorId 直接查询口径，不再依赖 caliberId
     */
    private void fillCaliberData(List<DeclareIndicatorRespVO> voList) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        for (DeclareIndicatorRespVO vo : voList) {
            // 通过指标ID直接查询口径数据
            DeclareIndicatorCaliberDO caliber = caliberService.getCaliberByIndicatorId(vo.getId());
            if (caliber != null) {
                vo.setCaliberId(caliber.getId());
                vo.setDefinition(caliber.getDefinition());
                vo.setStatisticScope(caliber.getStatisticScope());
                vo.setDataSource(caliber.getDataSource());
                vo.setFillRequire(caliber.getFillRequire());
                vo.setCalculationExample(caliber.getCalculationExample());
            }
        }
    }

}
